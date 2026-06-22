#pragma once

#include "driver/pulse_cnt.h"
#include "driver/gpio.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "ad3s_types.hpp"

namespace idf {

static const char* TAG_PCNT = "PCNT_ENCODER";

// Константы для PCNT - 16-битный счетчик
#define PCNT_HIGH_LIMIT  32767
#define PCNT_LOW_LIMIT  -32768

/**
 * Класс для управления одним энкодером через PCNT (Pulse Counter)
 * Реализован на основе примера Espressif rotary_encoder
 */
class EncoderPCNT {
public:
    EncoderPCNT(gpio_num_t pin_a, gpio_num_t pin_b, gpio_num_t pin_0, const char* name)
        : m_pin_a(pin_a), m_pin_b(pin_b), m_pin_0(pin_0), m_name(name), m_enabled(false), m_count(0) {

        m_pcnt_unit = nullptr;
        m_pcnt_chan_a = nullptr;
        m_pcnt_chan_b = nullptr;

        ESP_LOGI(TAG_PCNT, "%s: pins A=%d, B=%d, Index=%d (PCNT mode)", m_name, m_pin_a, m_pin_b, m_pin_0);
    }

    ~EncoderPCNT() {
        disable();
        if (m_pcnt_unit) {
            pcnt_del_unit(m_pcnt_unit);
        }
    }

    /**
     * Инициализация PCNT unit и channels
     * Вызывается один раз при старте
     */
    esp_err_t init() {
        esp_err_t ret;

        ESP_LOGI(TAG_PCNT, "%s: install pcnt unit", m_name);

        // Создаем PCNT unit
        pcnt_unit_config_t unit_config = {
            .low_limit = PCNT_LOW_LIMIT,
            .high_limit = PCNT_HIGH_LIMIT,
            .intr_priority = 0,
            .flags = {.accum_count = 0},
        };
        ret = pcnt_new_unit(&unit_config, &m_pcnt_unit);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to create pcnt unit: %s", m_name, esp_err_to_name(ret));
            return ret;
        }

        ESP_LOGI(TAG_PCNT, "%s: set glitch filter", m_name);
        pcnt_glitch_filter_config_t filter_config = {
            .max_glitch_ns = 1000,
        };
        ret = pcnt_unit_set_glitch_filter(m_pcnt_unit, &filter_config);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to set glitch filter: %s", m_name, esp_err_to_name(ret));
            return ret;
        }

        ESP_LOGI(TAG_PCNT, "%s: install pcnt channels", m_name);

        // Создаем channel A
        pcnt_chan_config_t chan_a_config = {
            .edge_gpio_num = m_pin_a,
            .level_gpio_num = m_pin_b,
            .flags = {.invert_edge_input = 0, .invert_level_input = 0, .virt_edge_io_level = 0, .virt_level_io_level = 0, .io_loop_back = 0},
        };
        ret = pcnt_new_channel(m_pcnt_unit, &chan_a_config, &m_pcnt_chan_a);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to create channel A: %s", m_name, esp_err_to_name(ret));
            return ret;
        }

        // Создаем channel B
        pcnt_chan_config_t chan_b_config = {
            .edge_gpio_num = m_pin_b,
            .level_gpio_num = m_pin_a,
            .flags = {.invert_edge_input = 0, .invert_level_input = 0, .virt_edge_io_level = 0, .virt_level_io_level = 0, .io_loop_back = 0},
        };
        ret = pcnt_new_channel(m_pcnt_unit, &chan_b_config, &m_pcnt_chan_b);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to create channel B: %s", m_name, esp_err_to_name(ret));
            return ret;
        }

        ESP_LOGI(TAG_PCNT, "%s: set edge and level actions for pcnt channels", m_name);

        // Настраиваем действия для каналов (quadrature decoder)
        ret = pcnt_channel_set_edge_action(m_pcnt_chan_a, PCNT_CHANNEL_EDGE_ACTION_DECREASE, PCNT_CHANNEL_EDGE_ACTION_INCREASE);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to set edge action A: %s", m_name, esp_err_to_name(ret));
            return ret;
        }
        ret = pcnt_channel_set_level_action(m_pcnt_chan_a, PCNT_CHANNEL_LEVEL_ACTION_KEEP, PCNT_CHANNEL_LEVEL_ACTION_INVERSE);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to set level action A: %s", m_name, esp_err_to_name(ret));
            return ret;
        }
        ret = pcnt_channel_set_edge_action(m_pcnt_chan_b, PCNT_CHANNEL_EDGE_ACTION_INCREASE, PCNT_CHANNEL_EDGE_ACTION_DECREASE);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to set edge action B: %s", m_name, esp_err_to_name(ret));
            return ret;
        }
        ret = pcnt_channel_set_level_action(m_pcnt_chan_b, PCNT_CHANNEL_LEVEL_ACTION_KEEP, PCNT_CHANNEL_LEVEL_ACTION_INVERSE);
        if (ret != ESP_OK) {
            ESP_LOGE(TAG_PCNT, "%s: failed to set level action B: %s", m_name, esp_err_to_name(ret));
            return ret;
        }

        // Настраиваем индексный пин (GPIO_0) с interrupt
        if (m_pin_0 >= 0) {
            ESP_LOGI(TAG_PCNT, "%s: setup index pin %d", m_name, m_pin_0);

            gpio_config_t io_conf = {
                .pin_bit_mask = (1ULL << m_pin_0),
                .mode = GPIO_MODE_INPUT,
                .pull_up_en = GPIO_PULLUP_ENABLE,
                .pull_down_en = GPIO_PULLDOWN_DISABLE,
                .intr_type = GPIO_INTR_POSEDGE,  // Interrupt on rising edge (0->1)
            };
            ret = gpio_config(&io_conf);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to config index pin: %s", m_name, esp_err_to_name(ret));
                return ret;
            }
        }

        ESP_LOGI(TAG_PCNT, "%s: PCNT initialization complete", m_name);
        return ESP_OK;
    }

    /**
     * Включение энкодера - запуск счетчика
     */
    esp_err_t enable() {
        if (!m_enabled) {
            esp_err_t ret;

            ESP_LOGI(TAG_PCNT, "%s: enable pcnt unit", m_name);
            ret = pcnt_unit_enable(m_pcnt_unit);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to enable pcnt unit: %s", m_name, esp_err_to_name(ret));
                return ret;
            }

            ESP_LOGI(TAG_PCNT, "%s: clear pcnt unit", m_name);
            ret = pcnt_unit_clear_count(m_pcnt_unit);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to clear count: %s", m_name, esp_err_to_name(ret));
                return ret;
            }

            ESP_LOGI(TAG_PCNT, "%s: start pcnt unit", m_name);
            ret = pcnt_unit_start(m_pcnt_unit);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to start pcnt unit: %s", m_name, esp_err_to_name(ret));
                return ret;
            }

            m_enabled = true;
            m_count = 0;

            // Регистрируем ISR для индексного пина (service уже установлен в менеджере)
            if (m_pin_0 >= 0) {
                gpio_isr_handler_add(m_pin_0, gpio_isr_handler, this);
                ESP_LOGI(TAG_PCNT, "%s: Index ISR registered on pin %d", m_name, m_pin_0);
            }

            ESP_LOGI(TAG_PCNT, "%s: ENABLED", m_name);
        }
        return ESP_OK;
    }

    /**
     * Выключение энкодера - остановка счетчика
     */
    esp_err_t disable() {
        if (m_enabled) {
            esp_err_t ret;

            ESP_LOGI(TAG_PCNT, "%s: stop pcnt unit", m_name);
            ret = pcnt_unit_stop(m_pcnt_unit);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to stop pcnt unit: %s", m_name, esp_err_to_name(ret));
                return ret;
            }

            // Читаем финальное значение
            int count_val;
            ret = pcnt_unit_get_count(m_pcnt_unit, &count_val);
            if (ret == ESP_OK) {
                m_count = count_val;
            } else {
                ESP_LOGE(TAG_PCNT, "%s: failed to get count: %s", m_name, esp_err_to_name(ret));
            }

            ESP_LOGI(TAG_PCNT, "%s: disable pcnt unit", m_name);
            ret = pcnt_unit_disable(m_pcnt_unit);
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "%s: failed to disable pcnt unit: %s", m_name, esp_err_to_name(ret));
                return ret;
            }

            m_enabled = false;

            // Удаляем ISR для индексного пина
            if (m_pin_0 >= 0) {
                gpio_isr_handler_remove(m_pin_0);
            }

            ESP_LOGI(TAG_PCNT, "%s: DISABLED, count=%d", m_name, m_count);
        }
        return ESP_OK;
    }

    /**
     * Получение текущего значения счетчика
     */
    int32_t getCount() {
        if (m_enabled && m_pcnt_unit != nullptr) {
            int count_val;
            pcnt_unit_get_count(m_pcnt_unit, &count_val);
            m_count = count_val;
        }
        return m_count;
    }

    /**
     * Установка значения счетчика
     */
    esp_err_t setCount(int32_t count) {
        m_count = count;
        // PCNT не поддерживает прямую установку значения
        // Нужно остановить, очистить, и добавить разницу
        return ESP_OK;
    }

    /**
     * ISR handler для индексного пина
     * Сброс PCNT НЕМЕДЛЕННО — в контексте ISR,
     * чтобы минимизировать окно между index и clear.
     * pcnt_unit_clear_count() безопасна для ISR (portENTER_CRITICAL_SAFE).
     */
    static void gpio_isr_handler(void* arg) {
        EncoderPCNT* enc = static_cast<EncoderPCNT*>(arg);
        if (enc && enc->m_enabled && enc->m_pcnt_unit) {
            pcnt_unit_clear_count(enc->m_pcnt_unit);
        }
    }

private:
    gpio_num_t m_pin_a;
    gpio_num_t m_pin_b;
    gpio_num_t m_pin_0;
    const char* m_name;

    pcnt_unit_handle_t m_pcnt_unit;
    pcnt_channel_handle_t m_pcnt_chan_a;
    pcnt_channel_handle_t m_pcnt_chan_b;

    volatile bool m_enabled;
    volatile int m_count;
};

/**
 * Менеджер для управления двумя энкодерами
 * Использует PCNT (Pulse Counter) peripheral - правильный подход для ESP32
 */
class EncoderManagerPCNT {
public:
    EncoderManagerPCNT() : m_enc1(nullptr), m_enc2(nullptr), m_initialized(false), m_enabled(false), m_monitor_task_handle(nullptr) {
        ESP_LOGI(TAG_PCNT, "EncoderManagerPCNT: created (encoders not initialized yet)");
    }

    ~EncoderManagerPCNT() {
        disable();
        if (m_enc1) {
            delete m_enc1;
        }
        if (m_enc2) {
            delete m_enc2;
        }
    }

    /**
     * Инициализация PCNT для обоих энкодеров
     * Вызывается при команде COMMAND_ENC_ON
     */
    esp_err_t init() {
        if (!m_initialized) {
            esp_err_t ret;

            ESP_LOGI(TAG_PCNT, "EncoderManagerPCNT: creating and initializing encoders");

            // Создаем энкодеры
            m_enc1 = new EncoderPCNT((gpio_num_t)GPIO_ENC1_A, (gpio_num_t)GPIO_ENC1_B, (gpio_num_t)GPIO_ENC1_0, "ENC1");
            m_enc2 = new EncoderPCNT((gpio_num_t)GPIO_ENC2_A, (gpio_num_t)GPIO_ENC2_B, (gpio_num_t)GPIO_ENC2_0, "ENC2");

            ret = m_enc1->init();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to initialize ENC1");
                return ret;
            }

            ret = m_enc2->init();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to initialize ENC2");
                return ret;
            }

            m_initialized = true;
            ESP_LOGI(TAG_PCNT, "EncoderManagerPCNT: initialized");
        }
        return ESP_OK;
    }

    /**
     * Включение обоих энкодеров
     */
    esp_err_t enable() {
        if (!m_initialized) {
            esp_err_t ret = init();
            if (ret != ESP_OK) {
                return ret;
            }
        }

        if (!m_enabled) {
            esp_err_t ret;

            // Устанавливаем GPIO ISR service один раз для всех энкодеров
            ret = gpio_install_isr_service(0);
            if (ret != ESP_OK && ret != ESP_ERR_INVALID_STATE) {
                ESP_LOGE(TAG_PCNT, "Failed to install GPIO ISR service: %s", esp_err_to_name(ret));
                return ret;
            }

            ret = m_enc1->enable();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to enable ENC1");
                return ret;
            }

            ret = m_enc2->enable();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to enable ENC2");
                return ret;
            }

            m_enabled = true;

            // Запускаем monitor task для отладочного вывода
            xTaskCreate(monitorTask, "ENC Monitor", 2048, this, 5, &m_monitor_task_handle);

            ESP_LOGI(TAG_PCNT, "ENCODER MANAGER: ENABLED");
        }
        return ESP_OK;
    }

    /**
     * Выключение обоих энкодеров
     */
    esp_err_t disable() {
        if (m_enabled) {
            esp_err_t ret;

            ret = m_enc1->disable();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to disable ENC1");
            }

            ret = m_enc2->disable();
            if (ret != ESP_OK) {
                ESP_LOGE(TAG_PCNT, "Failed to disable ENC2");
            }

            m_enabled = false;

            // Останавливаем monitor task
            if (m_monitor_task_handle) {
                vTaskDelete(m_monitor_task_handle);
                m_monitor_task_handle = nullptr;
            }

            ESP_LOGI(TAG_PCNT, "ENCODER MANAGER: DISABLED");
        }
        return ESP_OK;
    }

    /**
     * Получение значения ENC1
     */
    int32_t getEnc1Count() {
        return m_enc1->getCount();
    }

    /**
     * Получение значения ENC2
     */
    int32_t getEnc2Count() {
        return m_enc2->getCount();
    }

    /**
     * Проверка, включен ли менеджер
     */
    bool isEnabled() const {
        return m_enabled;
    }

    static void monitorTask(void* pvParameters) {
        EncoderManagerPCNT* manager = static_cast<EncoderManagerPCNT*>(pvParameters);

        while (manager->m_enabled) {
            vTaskDelay(pdMS_TO_TICKS(1000));

            if (!manager->m_enabled) {
                break;
            }

            ESP_LOGI("EncoderMonitor", "PCNT mode: ENC1=%d, ENC2=%d",
                      manager->getEnc1Count(), manager->getEnc2Count());
        }

        vTaskDelete(NULL);
    }

private:
    EncoderPCNT* m_enc1;
    EncoderPCNT* m_enc2;
    bool m_initialized;
    bool m_enabled;
    TaskHandle_t m_monitor_task_handle;
};

} // namespace idf

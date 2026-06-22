// LedUtil.cpp
#include "led_util.hpp"
#include "sdkconfig.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

LedUtil::LedUtil(gpio_num_t gpio, Mode mode) : m_gpio(gpio), m_mode(mode) {
#ifdef CONFIG_BLINK_LED_STRIP
    if (m_mode == Mode::LED_STRIP) {
        led_strip_config_t strip_config = {
            .strip_gpio_num = m_gpio,
            .max_leds = 1,
            .led_model = LED_MODEL_WS2812,  // Добавлено: модель светодиода
            .color_component_format = LED_STRIP_COLOR_COMPONENT_FMT_GRB // или RGB — зависит от вашей ленты
        };
#if CONFIG_BLINK_LED_STRIP_BACKEND_RMT
        led_strip_rmt_config_t rmt_config = {
            .resolution_hz = 10 * 1000 * 1000,
            .flags = { .with_dma = false }
        };
        ESP_ERROR_CHECK(led_strip_new_rmt_device(&strip_config, &rmt_config, &m_ledStrip));
#elif CONFIG_BLINK_LED_STRIP_BACKEND_SPI
        led_strip_spi_config_t spi_config = {
            .spi_bus = SPI2_HOST,
            .flags = { .with_dma = true }
        };
        ESP_ERROR_CHECK(led_strip_new_spi_device(&strip_config, &spi_config, &m_ledStrip));
#else
#error "Unsupported LED strip backend"
#endif
        led_strip_clear(m_ledStrip);
    } else
#endif
    if (m_mode == Mode::GPIO) {
        gpio_reset_pin(m_gpio);
        gpio_set_direction(m_gpio, GPIO_MODE_OUTPUT);
    }
}

LedUtil::~LedUtil() {
#ifdef CONFIG_BLINK_LED_STRIP
    if (m_ledStrip) {
        led_strip_del(m_ledStrip);
    }
#endif
}

void LedUtil::on() {
    m_state = true;
    m_blinkEnabled = false;
    update();
}

void LedUtil::off() {
    m_state = false;
    m_blinkEnabled = false;
    update();
}

void LedUtil::toggle() {
    m_state = !m_state;
    update();
}

void LedUtil::blink() {
    if (m_blinkEnabled) {
        toggle();
    }
}

void LedUtil::setBlinkEnabled(bool enabled) {
    m_blinkEnabled = enabled;
    if (!enabled) {
        off(); // можно убрать, если хотите сохранить текущее состояние
    }
}

void LedUtil::update() {
#ifdef CONFIG_BLINK_LED_STRIP
    if (m_mode == Mode::LED_STRIP) {
        if (m_state) {
            led_strip_set_pixel(m_ledStrip, 0, 16, 16, 16);
            led_strip_refresh(m_ledStrip);
        } else {
            led_strip_clear(m_ledStrip);
        }
    } else
#endif
    if (m_mode == Mode::GPIO) {
        // используем 1/0 вместо GPIO_HIGH / GPIO_LOW
        gpio_set_level(m_gpio, m_state ? 1 : 0);
    }
}

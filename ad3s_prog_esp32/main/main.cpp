#include "usb_cdc.hpp"
#include "led_util.hpp"
#include "console_handler.hpp"
#include "spi_maker.hpp"
#include "esp_task_wdt.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "ad3s_types.hpp"
#include "command_handler.hpp"
#include "esp32_dma_spi_master.hpp"
#include "nvs_flash.h"
#include "nvs.h"

using namespace idf;
extern "C" void app_main(void);


static LedUtil* g_led = nullptr;
idf::spi_maker* g_spi_maker = nullptr;
QueueHandle_t parsingPacket_queue = nullptr;
QueueHandle_t uart_resp_queue = nullptr;
QueueHandle_t spi0_queue = nullptr;

extern "C" void app_main(void) {

    // Увеличиваем таймаут TWDT — SPI polling блокирует CPU1 через
    // внутренние блокировки ESP-IDF драйвера (spi_bus_lock, vTaskSuspendAll)
    esp_task_wdt_config_t twdt_cfg = {
        .timeout_ms = 30000,
        .idle_core_mask = 0,
        .trigger_panic = false
    };
    esp_task_wdt_reconfigure(&twdt_cfg);

    // Инициализация NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    parsingPacket_queue = xQueueCreate(10, sizeof(idf::uartToMainQueueItem_t));
    uart_resp_queue = xQueueCreate(10, sizeof(idf::uartToMainQueueItem_t));
    spi0_queue = xQueueCreate(10, sizeof(idf::uartToMainQueueItem_t));

    ESP32DMASPI::Master master;
    master.setDataMode(SPI_MODE0);
    master.setFrequency(10000000);
    master.setMaxTransferSize(BUFFER_SIZE);
    master.setQueueSize(QUEUE_SIZE);
    master.beginSingle(2);
    vTaskDelay(pdMS_TO_TICKS(100)); // Ждём пока spi_master_task инициализирует шину и device_handle
    master.releaseSpiBus();

    USBCDC usbcdc(parsingPacket_queue, uart_resp_queue);
    usbcdc.init();

    g_spi_maker = new idf::spi_maker(spi0_queue);

    // Пин сброса преобразователя — в 1 до явной команды из Java
    gpio_set_direction((gpio_num_t)idf::GPIO_NRESET, GPIO_MODE_OUTPUT);
    gpio_set_level((gpio_num_t)idf::GPIO_NRESET, 1);

    idf::CommandHandler command_handler_instance(master, usbcdc, *g_spi_maker, parsingPacket_queue, uart_resp_queue);

    g_led = new LedUtil(static_cast<gpio_num_t>(CONFIG_BLINK_GPIO), LedUtil::Mode::LED_STRIP);

    ConsoleHandler console(g_led);

    xTaskCreate([](void* arg) { static_cast<ConsoleHandler*>(arg)->run(); },
                "console_task", 4096, &console, 5, nullptr);

    xTaskCreate([](void* arg) { static_cast<USBCDC*>(arg)->run(); },
                "usbcdc_task", 4096, &usbcdc, 5, nullptr);

    xTaskCreate([](void* arg) {
        const TickType_t delay = pdMS_TO_TICKS(500);
        while (true) {
            g_led->blink();
            vTaskDelay(delay);
        }
    }, "blink_task", 2048, nullptr, 5, nullptr);

    vTaskSuspend(nullptr);
}

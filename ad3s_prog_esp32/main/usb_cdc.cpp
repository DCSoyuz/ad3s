#include "usb_cdc.hpp"
#include "spi_maker.hpp"
#include "esp_log.h"
#include "sdkconfig.h"
#include <cstring>

// Define static members
const char *USBCDC::TAG = "usbcdc";
uint8_t USBCDC::rx_buf[CONFIG_TINYUSB_CDC_RX_BUFSIZE + 1];
QueueHandle_t USBCDC::app_queue = nullptr;
USBCDC *USBCDC::instance = nullptr;

USBCDC::USBCDC(QueueHandle_t parsing_queue, QueueHandle_t resp_queue)
    : parsingPacket_queue(parsing_queue), uart_resp_queue(resp_queue) {
    instance = this;
}

USBCDC::~USBCDC() {
    // Cleanup if needed
}

void USBCDC::init() {
    // Create FreeRTOS primitives
    app_queue = xQueueCreate(5, sizeof(app_message_t));
    assert(app_queue);

    ESP_LOGI(TAG, "USB initialization");
    const tinyusb_config_t tusb_cfg = TINYUSB_DEFAULT_CONFIG();
    ESP_ERROR_CHECK(tinyusb_driver_install(&tusb_cfg));

    tinyusb_config_cdcacm_t acm_cfg = {
        .cdc_port = TINYUSB_CDC_ACM_0,
        .callback_rx = &USBCDC::tinyusb_cdc_rx_callback, // the first way to register a callback
        .callback_rx_wanted_char = NULL,
        .callback_line_state_changed = NULL,
        .callback_line_coding_changed = NULL
    };

    ESP_ERROR_CHECK(tinyusb_cdcacm_init(&acm_cfg));
    /* the second way to register a callback */
    ESP_ERROR_CHECK(tinyusb_cdcacm_register_callback(
                        TINYUSB_CDC_ACM_0,
                        CDC_EVENT_LINE_STATE_CHANGED,
                        &USBCDC::tinyusb_cdc_line_state_changed_callback));

#if (CONFIG_TINYUSB_CDC_COUNT > 1)
    acm_cfg.cdc_port = TINYUSB_CDC_ACM_1;
    ESP_ERROR_CHECK(tinyusb_cdcacm_init(&acm_cfg));
    ESP_ERROR_CHECK(tinyusb_cdcacm_register_callback(
                        TINYUSB_CDC_ACM_1,
                        CDC_EVENT_LINE_STATE_CHANGED,
                        &USBCDC::tinyusb_cdc_line_state_changed_callback));
#endif

    ESP_LOGI(TAG, "USB initialization DONE");

    // Start the send response task
    xTaskCreate(send_usbcdc_response_task, "send_usbcdc_response_task", 4096, this, 8, NULL);
}

void USBCDC::run() {
    app_message_t msg;
    while (1) {
        if (xQueueReceive(app_queue, &msg, portMAX_DELAY)) {
            if (msg.buf_len) {
                ESP_LOGD(TAG, "Data from channel %d (%d bytes)", msg.itf, msg.buf_len);
                ESP_LOG_BUFFER_HEXDUMP(TAG, msg.buf, msg.buf_len, ESP_LOG_DEBUG);
            }
        }
    }
}

void USBCDC::tinyusb_cdc_rx_callback(int itf, cdcacm_event_t *event) {
    if (!instance) return;

    /* initialization */
    size_t rx_size = 0;
    /* read */
    esp_err_t ret = tinyusb_cdcacm_read(static_cast<tinyusb_cdcacm_itf_t>(itf), rx_buf, CONFIG_TINYUSB_CDC_RX_BUFSIZE, &rx_size);
    if (ret == ESP_OK) {
        static idf::uartToMainQueueItem_t itemQueue;
        itemQueue.pbuf = rx_buf;
        itemQueue.len = (uint16_t) rx_size;
        xQueueSend(instance->parsingPacket_queue, &itemQueue, portMAX_DELAY);
    } else {
        ESP_LOGE(TAG, "Read Error");
    }
}

void USBCDC::tinyusb_cdc_line_state_changed_callback(int itf, cdcacm_event_t *event) {
    if (!instance) return;

    int dtr = event->line_state_changed_data.dtr;
    int rts = event->line_state_changed_data.rts;
    ESP_LOGD(TAG, "Line state changed on channel %d: DTR:%d, RTS:%d", itf, dtr, rts);
}

uint8_t USBCDC::get_checksum_byte(uint8_t *pbuf, uint16_t len) {
    uint8_t check_sum = 0;
    for (uint16_t i = 0; i < len - 1; i++) {
        check_sum += pbuf[i];
    }
    return check_sum;
}

void USBCDC::send_usbcdc_response_task(void *pvParameters) {
    USBCDC* obj = reinterpret_cast<USBCDC*>(pvParameters);
    idf::uartToMainQueueItem_t resp;
    for (;;) {
        xQueueReceive(obj->uart_resp_queue, &resp, portMAX_DELAY);
        if (resp.pbuf == nullptr || resp.len == 0) {
            // Guard: a 0-length response would underflow the checksum index
            // (resp.len - 1 wraps as uint16_t) and corrupt the heap.
            ESP_LOGW(TAG, "dropping empty response (len=%u)", (unsigned)resp.len);
            continue;
        }
        resp.pbuf[resp.len - 1] = get_checksum_byte(resp.pbuf, (uint16_t)resp.len);

        size_t total = (size_t)resp.len;
        size_t offset = 0;
        while (offset < total) {
            size_t written = tinyusb_cdcacm_write_queue(TINYUSB_CDC_ACM_0, resp.pbuf + offset, total - offset);
            offset += written;
            esp_err_t err = tinyusb_cdcacm_write_flush(TINYUSB_CDC_ACM_0, 0);
            if (err != ESP_OK && err != ESP_ERR_NOT_FINISHED) {
                ESP_LOGE(TAG, "CDC ACM write flush error: %s", esp_err_to_name(err));
            }
            if (written == 0) {
                vTaskDelay(pdMS_TO_TICKS(1));
            }
        }
    }

    vTaskDelete(NULL);
}

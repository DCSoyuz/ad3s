#ifndef USBCDC_HPP
#define USBCDC_HPP

#include <stdint.h>
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "tinyusb.h"
#include "tinyusb_default_config.h"
#include "tinyusb_cdc_acm.h"
#include "ad3s_types.hpp"

class SpiMaker;



class USBCDC {
public:
    USBCDC(QueueHandle_t parsing_queue, QueueHandle_t resp_queue);
    ~USBCDC();
    void init();
    void run();

private:
    static const char *TAG;
    static uint8_t rx_buf[];
    static QueueHandle_t app_queue;
    static USBCDC *instance;

    QueueHandle_t parsingPacket_queue;
    QueueHandle_t uart_resp_queue;

    static uint8_t get_checksum_byte(uint8_t *pbuf, uint16_t len);
    static void send_usbcdc_response_task(void *pvParameters);

    typedef struct {
        uint8_t buf[CONFIG_TINYUSB_CDC_RX_BUFSIZE + 1];     // Data buffer
        size_t buf_len;                                     // Number of bytes received
        tinyusb_cdcacm_itf_t itf;                            // Index of CDC device interface
    } app_message_t;

    static void tinyusb_cdc_rx_callback(int itf, cdcacm_event_t *event);
    static void tinyusb_cdc_line_state_changed_callback(int itf, cdcacm_event_t *event);
};

#endif // USBCDC_HPP

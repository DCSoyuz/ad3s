#pragma once

#include "esp_log.h"
#include "driver/spi_master.h"
#include "ad3s_types.hpp"
#include "freertos/semphr.h"

#define SENDER_HOST SPI2_HOST

namespace idf {

static spi_device_handle_t handle;

class spi_maker {
public:

    spi_maker(QueueHandle_t spi0_queue) :
        spi0_queue(spi0_queue) {
        init_spi();
    }

    uint8_t get_even_parity(uint8_t bh, uint8_t bl);
    int readWordsFromIc(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t);
    int writeWordsToIc(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues);
    int writeRandomWordsToIC(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues);
    int readRandomAddressValues(uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues);
    void captureSpiBus();
    void releaseSpiBus();
    void setMilandrMode(int mode);
    void setClockSpeed(int speed_hz);

    // Mutex for SPI bus access protection (multi-transaction sequences)
    void lock()   { xSemaphoreTake(spi_mutex, portMAX_DELAY); }
    bool tryLock(int timeout_ms = 10) { return xSemaphoreTake(spi_mutex, pdMS_TO_TICKS(timeout_ms)) == pdTRUE; }
    void unlock() { xSemaphoreGive(spi_mutex); }

private:
    int milandrMode = 0;
    //Configuration for the SPI bus
    spi_bus_config_t buscfg = {
        .mosi_io_num = GPIO_MOSI,
        .miso_io_num = GPIO_MISO,
        .sclk_io_num = GPIO_SCLK,
        .quadwp_io_num = -1,
        .quadhd_io_num = -1
    };

    //Configuration for the SPI device on the other side of the bus
    spi_device_interface_config_t devcfg = {
        .command_bits = 0,
        .address_bits = 0,
        .dummy_bits = 0,
        .mode = 0,
        .duty_cycle_pos = 128,        //50% duty cycle
        .cs_ena_pretrans = 6,       //Keep the CS low 3 cycles after transaction, to stop slave from missing the last bit when CS has less propagation delay than CLK
        .cs_ena_posttrans = 2,
        .clock_speed_hz = 10000000,
        .input_delay_ns = 0,
        .spics_io_num = GPIO_CS,
        .flags = 0, 
        .queue_size = 1024,
        .pre_cb = NULL,
        .post_cb = NULL
    };

    QueueHandle_t spi0_queue;
    SemaphoreHandle_t spi_mutex = nullptr;

    void init_spi(void);
};

}

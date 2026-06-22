#include "spi_maker.hpp"

namespace idf {

uint8_t spi_maker::get_even_parity(uint8_t bh, uint8_t bl) {
    uint8_t parity = 0;
    for (int i = 0; i <= 7; i++) {
        parity = parity + (bh & 0x01) + (bl & 0x01);
        bh = bh >> 1;
        bl = bl >> 1;
    }
    parity = parity & 0x01;
    return parity;
}

int spi_maker::readWordsFromIc(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t) {
    if (!tryLock(10)) return -1;  // SPI bus busy, skip (don't block angle_poll)
    ESP_LOGD(TAG, "readWordsFromIc address: %d size: %d\n", address, size);
    uint8_t sendbuf[2];
    uint8_t recvbuf[2];
    (*t).length = sizeof(sendbuf) * 8;

    for (int i = 0; i <= size; i++) {
        uint8_t bh = 0;
        uint8_t bl = 0;
        if (!milandrMode) {
            bh = (uint8_t)(0xC0 | ((address + i) >> 6));
            bl = (uint8_t)(((address + i) << 2) & 0xFF);
        } else {
            bh = (uint8_t)(0xC0 | ((address + i) >> 2));
            bl = (uint8_t)(((address + i) << 6) & 0xFF);
        }

        uint8_t parity = get_even_parity(bh, bl);
        bl = bl + parity;
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;
        (*t).rx_buffer = recvbuf;
        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
    }
    unlock();
    return 0;
}

int spi_maker::writeWordsToIc(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues) {
    lock();
    uint8_t sendbuf[2];
    uint8_t recvbuf[2];
    (*t).length = sizeof(sendbuf) * 8;

    for (int i = 0; i < size; i++) {
        uint8_t bh = 0;
        uint8_t bl = 0;
        if (!milandrMode) {
            bh = (uint8_t)(0x80 | ((address + i) >> 6));
            bl = (uint8_t)(((address + i) << 2) & 0xFF);
        } else {
            bh = (uint8_t)(0x80 | ((address + i) >> 2));
            bl = (uint8_t)(((address + i) << 6) & 0xFF);
        }
        uint8_t parity = get_even_parity(bh, bl);
        bl = bl + parity;
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;
        (*t).rx_buffer = recvbuf;
        //ESP_LOGI(TAG, "Send spi address: [%02x, %02x]", bh, bl);
        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
        bl = (uint8_t)*(wValues + 2 * i);
        bh = (uint8_t)*(wValues + 2 * i + 1);
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;
        // ESP_LOGI(TAG, "Send spi data: [%02x, %02x]", bh, bl);
        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
    }
    unlock();
    return 0;
}

int spi_maker::writeRandomWordsToIC(uint16_t address, uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues) {
    lock();
    uint8_t sendbuf[2];
    uint8_t recvbuf[2];
    (*t).length = sizeof(sendbuf) * 8;
    //ESP_LOGI(TAG, "Address: %d\n", address);
    for (int i = 0; i < (size / 2); i++) {
        uint8_t bl_addr = (uint8_t)*(wValues + 4 * i);
        uint8_t bh_addr = (uint8_t)*(wValues + 4 * i + 1);
        uint16_t address = ((uint16_t)bh_addr << 8) | (uint16_t)bl_addr;

        uint8_t bh = 0;
        uint8_t bl = 0;
        if (!milandrMode) {
            bh = (uint8_t)(0x80 | ((address) >> 6));
            bl = (uint8_t)(((address) << 2) & 0xFF);
        } else {
            bh = (uint8_t)(0xC0 | ((address) >> 2));
            bl = (uint8_t)(((address) << 6) & 0xFF);
        }
        uint8_t parity = get_even_parity(bh, bl);
        bl = bl + parity;
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;
        (*t).rx_buffer = recvbuf;
        //ESP_LOGI(TAG, "Send spi address: [%02x, %02x]", bh, bl);
        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
        bl = (uint8_t)*(wValues + 4 * i + 2);
        bh = (uint8_t)*(wValues + 4 * i + 3);
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;

        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
    }
    unlock();
    return 0;
}

int spi_maker::readRandomAddressValues(uint16_t size, uint8_t * rValues, spi_transaction_t * t, uint8_t * wValues) {
    lock();
    uint8_t sendbuf[2];
    uint8_t recvbuf[2];
    (*t).length = sizeof(sendbuf) * 8;

    for (int i = 0; i <= size; i++) {
        uint8_t bl_addr = (uint8_t)*(wValues + 2 * i);
        uint8_t bh_addr = (uint8_t)*(wValues + 2 * i + 1);
        uint16_t address = ((uint16_t)bh_addr << 8) | (uint16_t)bl_addr;
        uint8_t bh = 0;
        uint8_t bl = 0;
        if (!milandrMode) {
            bh = (uint8_t)(0xC0 | ((address) >> 6));
            bl = (uint8_t)(((address) << 2) & 0xFF);
        } else {
            bh = (uint8_t)(0xC0 | ((address) >> 2));
            bl = (uint8_t)(((address) << 6) & 0xFF);
        }
        uint8_t parity = get_even_parity(bh, bl);
        bl = bl + parity;
        sendbuf[0] = bh;
        sendbuf[1] = bl;
        (*t).tx_buffer = sendbuf;
        (*t).rx_buffer = recvbuf;
        spi_device_polling_transmit(handle, t);
        if (i != 0) {
            rValues[2 * (i - 1)] = recvbuf[1];
            rValues[2 * (i - 1) + 1] = recvbuf[0];
        }
        //ESP_LOGI(TAG, "Receive SPI data: [%02x, %02x]", recvbuf[1], recvbuf[0]);
    }
    unlock();
    return 0;
}

void spi_maker::captureSpiBus() {
    esp_err_t ret = spi_bus_initialize(SENDER_HOST, &buscfg, SPI_DMA_CH_AUTO);
    assert(ret == ESP_OK);
    ret = spi_bus_add_device(SENDER_HOST, &devcfg, &handle);
    assert(ret == ESP_OK);
}

void spi_maker::releaseSpiBus() {
    esp_err_t ret = spi_bus_remove_device(handle);
    assert(ret == ESP_OK);
    ret = spi_bus_free(SENDER_HOST);
    assert(ret == ESP_OK);
}

void spi_maker::setMilandrMode(int mode) {
    milandrMode = mode;
}

void spi_maker::setClockSpeed(int speed_hz) {
    esp_err_t ret = spi_bus_remove_device(handle);
    assert(ret == ESP_OK);
    devcfg.clock_speed_hz = speed_hz;
    ret = spi_bus_add_device(SENDER_HOST, &devcfg, &handle);
    assert(ret == ESP_OK);
}

void spi_maker::init_spi(void) {
    esp_err_t ret;
    spi_mutex = xSemaphoreCreateMutex();
    assert(spi_mutex);
    captureSpiBus();
}

}

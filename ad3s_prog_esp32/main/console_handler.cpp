// ConsoleHandler.cpp
#include "console_handler.hpp"
#include "driver/uart.h"
#include "esp_log.h"
#include <cstring>
#include <cstdio>

static const char* TAG = "console";

ConsoleHandler::ConsoleHandler(LedUtil* led) : m_led(led) {}

void ConsoleHandler::run() {
    uart_config_t uart_config = {
        .baud_rate = 115200,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };

    uart_driver_install(UART_NUM_0, 256, 0, 0, NULL, 0);
    uart_param_config(UART_NUM_0, &uart_config);

    sendPrompt();

    uint8_t ch;
    while (true) {
        int len = uart_read_bytes(UART_NUM_0, &ch, 1, pdMS_TO_TICKS(100));
        if (len > 0) {
            uart_write_bytes(UART_NUM_0, (const char*)&ch, 1); // echo
            if (ch == '\r' || ch == '\n') {
                m_buffer[m_pos] = '\0';
                if (m_pos > 0) {
                    processCommand(m_buffer);
                }
                m_pos = 0;
                sendPrompt();
            } else if (m_pos < BUFFER_SIZE - 1) {
                m_buffer[m_pos++] = ch;
            }
        }
    }
}

void ConsoleHandler::sendPrompt() {
    printf("> ");
}

void ConsoleHandler::processCommand(const char* cmd) {
    if (strcmp(cmd, "led on") == 0) {
        m_led->on();
        printf("LED turned ON\n");
    } else if (strcmp(cmd, "led off") == 0) {
        m_led->off();
        printf("LED turned OFF\n");
    } else if (strcmp(cmd, "blink on") == 0) {
        m_led->setBlinkEnabled(true);
        printf("Blinking ENABLED\n");
    } else if (strcmp(cmd, "blink off") == 0) {
        m_led->setBlinkEnabled(false);
        printf("Blinking DISABLED\n");
    } else {
        printf("Unknown command: %s\n", cmd);
    }
}

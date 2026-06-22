// ConsoleHandler.h
#pragma once

#include "led_util.hpp"
#include <functional>

class ConsoleHandler {
public:
    explicit ConsoleHandler(LedUtil* led);
    void run(); // будет выполняться в отдельной задаче

private:
    LedUtil* m_led;
    void processCommand(const char* command);

    // Буфер для ввода
    static const int BUFFER_SIZE = 128;
    char m_buffer[BUFFER_SIZE];
    int m_pos = 0;

    void readLine();
    void sendPrompt();
};

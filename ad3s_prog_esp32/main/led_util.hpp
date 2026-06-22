// LedUtil.h
#pragma once

#include "driver/gpio.h"
#include "esp_log.h"
#include "led_strip.h"

class LedUtil {
public:
    enum class Mode {
        GPIO,
        LED_STRIP
    };

    explicit LedUtil(gpio_num_t gpio, Mode mode);
    ~LedUtil();

    void on();
    void off();
    void toggle();
    void blink();
    void setBlinkEnabled(bool enabled);

private:
    gpio_num_t m_gpio;
    Mode m_mode;
    bool m_state{false};
    bool m_blinkEnabled{true};

#ifdef CONFIG_BLINK_LED_STRIP
    led_strip_handle_t m_ledStrip{nullptr};
#endif

    void update();
};

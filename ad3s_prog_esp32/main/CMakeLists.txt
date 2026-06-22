idf_component_register(SRCS "main.cpp" "led_util.cpp" "console_handler.cpp" "usb_cdc.cpp" "spi_maker.cpp"
                   INCLUDE_DIRS "."
                   REQUIRES esp_driver_uart esp_driver_gpio led_strip esp_driver_pcnt esp_timer nvs_flash)

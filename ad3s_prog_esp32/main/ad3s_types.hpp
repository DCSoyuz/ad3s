#pragma once

#include <stdint.h>
#include "sdkconfig.h"

namespace idf {

  static constexpr size_t BUFFER_SIZE = 8192; // should be multiple of 4
  static constexpr size_t QUEUE_SIZE = 1;

  static constexpr uint8_t  SPI_MODE0 = 0;

  static constexpr int EX_UART_NUM          = 2;
  static constexpr int PATTERN_CHR_NUM      = 3;       /*!< Set the number of consecutive and identical characters received by receiver which defines a UART pattern*/
  static constexpr int BUF_SIZE             = 2048;
  static constexpr int RD_BUF_SIZE          = BUF_SIZE;
  static constexpr int WR_BUF_SIZE          = BUFFER_SIZE + 256;

  static constexpr int A_REQ_STARTBYTE        =   0;
  static constexpr int A_REQ_NUMB_L           =   1;
  static constexpr int A_REQ_NUMB_H           =   2;
  static constexpr int A_REQ_COMMAND_WORD     =   3;
  static constexpr int A_REQ_ADDRESS_L        =   4;
  static constexpr int A_REQ_ADDRESS_H        =   5;
  static constexpr int A_REQ_LEN_L            =   6;
  static constexpr int A_REQ_LEN_H            =   7;
  static constexpr int A_REQ_VALUE0_L         =   8;
  static constexpr int A_REQ_VALUE0_H         =   9;

  static constexpr int A_RESP_STARTBYTE       =   0;
  static constexpr int A_RESP_NUMB_L          =   1;
  static constexpr int A_RESP_NUMB_H          =   2;
  static constexpr int A_RESP_COMMAND_WORD    =   3;
  static constexpr int A_RESP_ADDRESS_L       =   4;
  static constexpr int A_RESP_ADDRESS_H       =   5;
  static constexpr int A_RESP_NUMW_L          =   6;
  static constexpr int A_RESP_NUMW_H          =   7;
  static constexpr int A_RESP_VALUE0_L        =   8;
  static constexpr int A_RESP_VALUE0_H        =   9;

  static constexpr int COMMAND_READ                   =   0x01;
  static constexpr int COMMAND_WRITE                  =   0x02;
  static constexpr int COMMAND_START_READ_CYCLIC      =   0x03;
  static constexpr int COMMAND_STOP_READ_CYCLIC       =   0x04;
  static constexpr int COMMAND_RANDOM_READ            =   0x05;

  static constexpr int RESP_START_BYTE                =   0x55;
  static constexpr int COMMAND_WRITE_RANDOM           =   0x06;
  static constexpr int COMMAND_PROG_BOTP              =   0x07;
  static constexpr int COMMAND_READ_BOTP              =   0x08;
  static constexpr int COMMAND_PROG_UOTP              =   0x09;
  static constexpr int COMMAND_PROG_FOTP              =   0x0A;
  static constexpr int COMMAND_SET_LED                =   0x0B;
  static constexpr int COMMAND_READ_WITH_INDUCTOSYN   =   0x0C;
  static constexpr int COMMAND_SET_MASTER_FPGA_MODE   =   0x0D;
  static constexpr int COMMAND_READ_HANDTAP           =   0x0E;
  static constexpr int COMMAND_SET_STNDBY             =   0x0F;
  static constexpr int COMMAND_SET_NRESET             =   0x10;
  static constexpr int COMMAND_SET_DMA_QUAD           =   0x11;
  static constexpr int COMMAND_SET_VC                 =   0x12;
  static constexpr int COMMAND_SET_SDI                =   0x13;
  static constexpr int COMMAND_SET_DMA_SINGLE         =   0x14;
  static constexpr int COMMAND_SET_VPP9V              =   0x15;
  static constexpr int COMMAND_WRITE_BOTP             =   0x16;
  static constexpr int COMMAND_READ_CPU_REGS          =   0x17;
  static constexpr int COMMAND_SET_SPI_SPEED          =   0x18;
  static constexpr int COMMAND_ENC_ON                 =   0x19;
  static constexpr int COMMAND_ENC_OFF                =   0x1A;
  static constexpr int COMMAND_READ_ENCODERS          =   0x1B;
  static constexpr int COMMAND_START_RECORD           =   0x1C;
  static constexpr int COMMAND_STOP_RECORD            =   0x1D;
  static constexpr int COMMAND_SET_ELEC_OFFSET       =   0x22;


  // 33 22 1
  static constexpr int LED_PIN              = 2;

#if defined(CONFIG_INDUCTIVE3VER)
  static constexpr int STNDBY_PIN           = 9;
  static constexpr int VPP9V_PIN            = 3;
  static constexpr int GPIO_MOSI            = 11;
  static constexpr int GPIO_MISO            = 13;
  static constexpr int GPIO_SCLK            = 12;
  static constexpr int GPIO_CS              = 10;
  static constexpr int GPIO_SAMPLE          = 46;
  static constexpr int GPIO_VC              = 40;
  static constexpr int GPIO_NSEN            = 42;
  static constexpr int GPIO_NRESET          = 15;
  static constexpr int GPIO_ENC1_0          = 37;
  static constexpr int GPIO_ENC1_A          = 35;
  static constexpr int GPIO_ENC1_B          = 36;
  static constexpr int GPIO_ENC2_0          = 21;
  static constexpr int GPIO_ENC2_A          = 45;
  static constexpr int GPIO_ENC2_B          = 47; 
#elif defined(CONFIG_INDUCTOSYN_VER2)
  static constexpr int STNDBY_PIN           = 41;
  static constexpr int VPP9V_PIN            = 35;
  static constexpr int GPIO_MOSI            = 12;
  static constexpr int GPIO_MISO            = 13;
  static constexpr int GPIO_SCLK            = 11;
  static constexpr int GPIO_CS              = 14;
  static constexpr int GPIO_SAMPLE          = 39;
  static constexpr int GPIO_VC              = 40;
  static constexpr int GPIO_NSEN            = 42;
  static constexpr int GPIO_NRESET          = 8;
  static constexpr int GPIO_ENC1_0          = 14;
  static constexpr int GPIO_ENC1_A          = 47;
  static constexpr int GPIO_ENC1_B          = 21;
  static constexpr int GPIO_ENC2_0          = 4;
  static constexpr int GPIO_ENC2_A          = 5;
  static constexpr int GPIO_ENC2_B          = 6;
#elif defined(CONFIG_USERDEMO_BOARD)
  static constexpr int STNDBY_PIN           = 9;
  static constexpr int VPP9V_PIN            = 3;
  static constexpr int GPIO_MOSI            = 11;
  static constexpr int GPIO_MISO            = 13;
  static constexpr int GPIO_SCLK            = 12;
  static constexpr int GPIO_CS              = 10;
  static constexpr int GPIO_SAMPLE          = 46;
  static constexpr int GPIO_VC              = 16;
  static constexpr int GPIO_NSEN            = 42;
  static constexpr int GPIO_NRESET          = 15;
  static constexpr int GPIO_ENC1_0          = 35;
  static constexpr int GPIO_ENC1_A          = 37;
  static constexpr int GPIO_ENC1_B          = 38;
  static constexpr int GPIO_ENC2_0          = 39;
  static constexpr int GPIO_ENC2_A          = 40;
  static constexpr int GPIO_ENC2_B          = 41;
#elif defined(CONFIG_USER_ANY_BOARD)
  // ============================================================
  // ПОЛЬЗОВАТЕЛЬСКАЯ ПЛАТА — любой ESP32-S3 (копеечный devkit).
  // ЗАМЕНИТЕ значения на пины своей обвязки к микросхеме 5400ТР065А-022!
  // Подробности и таблицу сигналов см. в README.md (раздел «Настройка платы и пинов»).
  // Не используйте пины flash/PSRAM (обычно 26..32) и strapping (0, 3, 45, 46).
  // ============================================================
  static constexpr int STNDBY_PIN           = 9;
  static constexpr int VPP9V_PIN            = 4;
  static constexpr int GPIO_MOSI            = 11;   // SPI MOSI  -> SDI чипа
  static constexpr int GPIO_MISO            = 13;   // SPI MISO <- SDO чипа
  static constexpr int GPIO_SCLK            = 12;   // SPI SCLK
  static constexpr int GPIO_CS              = 10;   // SPI Chip Select
  static constexpr int GPIO_SAMPLE          = 6;
  static constexpr int GPIO_VC              = 7;
  static constexpr int GPIO_NSEN            = 8;
  static constexpr int GPIO_NRESET          = 5;    // сброс чипа
  // Энкодеры опциональны (нужны только для команд ENC_ON / READ_ENCODERS):
  static constexpr int GPIO_ENC1_0          = 38;
  static constexpr int GPIO_ENC1_A          = 39;
  static constexpr int GPIO_ENC1_B          = 40;
  static constexpr int GPIO_ENC2_0          = 41;
  static constexpr int GPIO_ENC2_A          = 42;
  static constexpr int GPIO_ENC2_B          = 47;
#else
  static constexpr int STNDBY_PIN           = 41;
  static constexpr int VPP9V_PIN            = 35;
  static constexpr int GPIO_MOSI            = 11;
  static constexpr int GPIO_MISO            = 13;
  static constexpr int GPIO_SCLK            = 12;
  static constexpr int GPIO_CS              = 10;
  static constexpr int GPIO_SAMPLE          = 39;
  static constexpr int GPIO_VC              = 40;
  static constexpr int GPIO_NSEN            = 42;
  static constexpr int GPIO_NRESET          = 8;
  static constexpr int GPIO_ENC1_0          = 14;
  static constexpr int GPIO_ENC1_A          = 47;
  static constexpr int GPIO_ENC1_B          = 21;
  static constexpr int GPIO_ENC2_0          = 4;
  static constexpr int GPIO_ENC2_A          = 5;
  static constexpr int GPIO_ENC2_B          = 6;
#endif
  static const char *TAG = "ad3s_prog_esp32";

#if defined(CONFIG_INDUCTIVE3VER) || defined(CONFIG_USERDEMO_BOARD)
  static inline int vpp9v_level(int level) { return level ^ 1; }
#else
  static inline int vpp9v_level(int level) { return level; }
#endif

  typedef struct {
    uint8_t * pbuf;
    uint16_t len;
  } uartToMainQueueItem_t;

  typedef struct {
    uint8_t * wValues;
    uint8_t * rValues;
    uint16_t size;
  } cyclicReadQueueItem_t;

} // namespace idf

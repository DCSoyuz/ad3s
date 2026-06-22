# Запись коэффициентов BOTP в микросхему по SPI

## Общая архитектура

```
Java-приложение (workstation4ad3s)  →  USB CDC (виртуальный COM)  →  ESP32-S3 (led_console)  →  SPI  →  Микросхема RDC
                                     бинарные пакеты                    разбор команд              parity + framing
```

Кнопка **"Prog BOTP"** в Java-приложении инициирует необратимую операцию записи 512 16-битных слов в Block OTP память микросхемы через SPI с подачей напряжения программирования VPP (9В).

---

## 1. UI: кнопка Prog BOTP

**Файл:** `MemoryEditor.java:407-413`

При нажатии кнопки:

1. Двойной диалог подтверждения ("IRREVERSIBLE OPERATION" → "FINAL CONFIRMATION").
2. Вызов `Model.getMemoryModel().progBOTPtoROM()`.

---

## 2. Формирование данных для записи (512 слов)

**Файл:** `ParserModel.java:90-127` — метод `createBOTPfile()`

Файл `rom_BOTP.hex` собирается из трёх источников в следующем порядке:

| Сегмент | Источник            | Слова         | Кол-во |
|---------|---------------------|---------------|--------|
| 1       | `cpu1_data.hex`     | [0..231]      | 232    |
| 2       | `base_ram.txt`      | [0..15]       | 16     |
| 3       | `cpu1_data.hex`     | [248..251]    | 4      |
| 4       | `cpu2_data.hex`     | [248..251]    | 4      |
| 5       | `cpu2_data.hex`     | [0..231]      | 232    |
| 6       | `base_ram.txt`      | [32..47]      | 16     |
| 7       | `base_ram.txt`      | [64..71]      | 8      |
|         | **Итого**           |               | **512** |

---

## 3. Протокол UART-пакета

### Формат запроса (Java → MCU)

```
 Байт    Поле           Описание
 ─────────────────────────────────────────────────
 [0]     0x77           START_REQ_BYTE — маркер начала
 [1-2]   NUMB (LE)      Общая длина пакета (без стартового байта и контрольной суммы)
 [3]     COMMAND        Код команды
 [4-5]   ADDRESS (LE)   Адрес (для WRITE_BOTP = 0)
 [6-7]   NUMW (LE)      Количество 16-битных слов данных
 [8..N]  DATA           Полезная нагрузка (пары lo, hi)
 [N+1]   CHECKSUM       Сумма всех предыдущих байт mod 256
```

### Формат ответа (MCU → Java)

```
 Байт    Поле           Описание
 ─────────────────────────────────────────────────
 [0]     0x55           START_RESP_BYTE
 [1-2]   NUMB (LE)      Длина ответа
 [3]     COMMAND        Эхо команды
 [4-5]   ADDRESS (LE)   Эхо адреса
 [6-7]   NUMW (LE)      Количество слов
 [8..N]  DATA           Прочитанные данные
 [N+1]   CHECKSUM       Контрольная сумма
```

### Используемые команды

| Команда              | Код  | Назначение                                   |
|----------------------|------|----------------------------------------------|
| `WRITE_RANDOM`       | 0x06 | Запись в произвольный регистр (вкл/выкл PGM) |
| `WRITE_BOTP`         | 0x16 | Запись данных BOTP через SPI + импульс VPP   |

---

## 4. Регистры BOTP микросхемы

**Файл:** `Regs.java:201-210`, `RegField.java:778-792`

| Регистр    | Смещение | Назначение                               |
|------------|----------|------------------------------------------|
| `BOTP_addr`| 4        | Адрес слова в OTP (0..511)               |
| `BOTP_data`| 5        | Данные для записи (16 бит)               |
| `BOTP_ctrl`| 6        | Управляющий регистр                      |
| `BOTP_out` | 7        | Результат чтения (только чтение)         |

### Биты BOTP_ctrl

| Бит | Поле  | Значение                                              |
|-----|-------|-------------------------------------------------------|
| 3   | PGM   | 1 = запись по адресу BOTP_addr данными BOTP_data     |
| 2   | REN   | 1 = чтение по адресу BOTP_addr, результат в BOTP_out |
| 1   | NCEN  | 0 = BOTP активен, 1 = BOTP игнорирует команды        |
| 0   | SLEEP | 0 = активный режим, 1 = энергосбережение             |

---

## 5. Полная последовательность записи

**Файл:** `MemoryModel.java:770-866` — `ProgBOTPtoICAction.run()`

### Шаг 1 — Парсинг hex-файла

```
Чтение rom_BOTP.hex → List<String> hexData (512 hex-строк)
```

### Шаг 2 — Создание 512 пакетов

Для каждого слова (адрес от 0 до 511) создаётся один пакет `WRITE_BOTP` с полезной нагрузкой из 5 слов:

```
dataPacket = [
    BOTP_addr,     // регистр адреса (смещение 4)
    cur_address,   // 0, 1, 2, ... 511
    BOTP_data,     // регистр данных (смещение 5)
    hex_value,     // значение коэффициента
    BOTP_ctrl      // регистр управления (смещение 6) — без значения, только адрес
]
```

Каждый пакет превращается в бинарный вид через `PacketIcHelper.getBytesFromPacketToIc()`.

### Шаг 3 — Включение режима программирования

Отправка команды `WRITE_RANDOM` для установки `BOTP_ctrl = 0x08` (бит PGM = 1):

```
dataPacketPrev = [BOTP_ctrl_addr, 0x08]
→ Пакет: [0x77][len][0x06][addr=0][numw=2][BOTP_ctrl_addr_lo, BOTP_ctrl_addr_hi, 0x08, 0x00][checksum]
```

Ожидание ответа до 1000 мс.

### Шаг 4 — Отправка 512 пакетов данных

Каждый из 512 пакетов отправляется последовательно:

```
Пакет i: [0x77][len][0x16][addr=0][numw=5]
          [BOTP_addr_lo, BOTP_addr_hi, i_lo, i_hi, BOTP_data_lo, BOTP_data_hi,
           value_lo, value_hi, BOTP_ctrl_lo, BOTP_ctrl_hi]
          [checksum]
```

Ожидание ответа на каждый пакет до 1000 мс.

### Шаг 5 — Отключение режима программирования

Отправка `WRITE_RANDOM` для очистки `BOTP_ctrl = 0x00`:

```
dataPacketAfter = [BOTP_ctrl_addr, 0x00]
→ Пакет: [0x77][len][0x06][addr=0][numw=2][BOTP_ctrl_addr_lo, BOTP_ctrl_addr_hi, 0x00, 0x00][checksum]
```

---

## 6. Обработка на ESP32-S3 (прошивка МК)

### Приём пакета

**Файл:** `usbcdc.cpp:80-97`

```
USB CDC callback → чтение в rx_buf → xQueueSend(parsingPacket_queue)
                 → master2::main_task() забирает из очереди
```

### Разбор команды WRITE_BOTP

**Файл:** `master2.hpp:671-679`

```cpp
case COMMAND_WRITE_BOTP:
    // 1. Запись адрес + данные через SPI в микросхему
    obj->spim.writeRandomWordsToIC(address, size, rValues, &t, wValues);

    // 2. Импульс напряжения программирования 9В (VPP)
    gpio_set_level(VPP9V_PIN, vpp9v_level(0));   // VPP ON
    vTaskDelay(20 / portTICK_PERIOD_MS);         // ожидание 20 мс
    gpio_set_level(VPP9V_PIN, vpp9v_level(1));   // VPP OFF
```

**Важно:** VPP подаётся **после** SPI-записи. Контакт `VPP9V_PIN = GPIO 35`. На ревизии `INDUCTIVE3VER` полярность инвертирована (`vpp9v_level()` выполняет XOR с 1).

### SPI-протокол на уровне шины

**Файл:** `spi_maker.cpp:110-156` — `writeRandomWordsToIC()`

Пакет данных от Java содержит пары `[addr_lo, addr_hi, data_lo, data_hi]`. Для каждой пары выполняется **две SPI-транзакции** по 2 байта:

**Транзакция 1 — Адрес:**
```
bh = 0x80 | (address >> 6)     // стандартный режим
bl = ((address << 2) & 0xFF) + parity_bit
sendbuf = [bh, bl]
→ spi_device_polling_transmit()
```

**Транзакция 2 — Данные:**
```
sendbuf = [data_lo, data_hi]
→ spi_device_polling_transmit()
```

Где `parity_bit` — чётность `(bh XOR bl)`, вычисляемая функцией `get_even_parity()`.

В альтернативном режиме адрес кодируется иначе:
```
bh = 0xC0 | (address >> 2)
bl = ((address << 6) & 0xFF) + parity_bit
```

### Параметры SPI

| Параметр      | Значение                |
|---------------|-------------------------|
| SPI Host      | SPI2_HOST               |
| Режим         | Mode 0 (CPOL=0, CPHA=0) |
| Частота       | 10 МГц (по умолчанию)   |
| Размер帧       | 16 бит (2 байта)        |
| Метод         | `spi_device_polling_transmit()` |
| Мьютекс       | `spi_mutex` (захват на всю последовательность) |

### GPIO SPI (стандартная конфигурация)

| Сигнал | GPIO |
|--------|------|
| MOSI   | 11   |
| MISO   | 13   |
| SCLK   | 12   |
| CS     | 10   |
| VPP9V  | 35   |

---

## 7. Временная диаграмма записи одного слова

```
       Java                                          ESP32-S3                                  Микросхема
       ────                                          ────────                                  ─────────
 Шаг 3: WRITE_RANDOM (BOTP_ctrl=0x08)
  ──────[0x77|len|0x06|...|BOTP_ctrl=0x08|CS]────→  разбор пакета
                                                     ──[SPI: addr=6, data=0x08]──→  BOTP_ctrl.PGM = 1

 Шаг 4: WRITE_BOTP (слово i)
  ──────[0x77|len|0x16|...|addr=i, data=V|CS]───→  разбор пакета
                                                     ──[SPI: addr=4, data=i]────→  BOTP_addr = i
                                                     ──[SPI: addr=5, data=V]───→  BOTP_data = V
                                                     ──[SPI: addr=6]───────────→  BOTP_ctrl (триггер записи)
                                                     ──[VPP ON, 20ms, VPP OFF]─→  Запись в OTP ячейку
  ←────[0x55|len|0x16|...|CS]──────────────────────  отправка ответа

 ... повторить 512 раз ...

 Шаг 5: WRITE_RANDOM (BOTP_ctrl=0x00)
  ──────[0x77|len|0x06|...|BOTP_ctrl=0x00|CS]────→  разбор пакета
                                                     ──[SPI: addr=6, data=0x00]──→  BOTP_ctrl.PGM = 0
```

---

## 8. Исходные файлы

### Java (workstation4ad3s-flatlaf)

| Файл                               | Назначение                                |
|-------------------------------------|-------------------------------------------|
| `form/editor/MemoryEditor.java`     | UI кнопки, диалоги подтверждения          |
| `model/uart/MemoryModel.java`       | Логика progBOTPtoROM(), создание пакетов  |
| `model/uart/ic/PacketIcHelper.java` | Сериализация пакетов в байты              |
| `model/uart/ic/McuCommand.java`     | Коды команд                               |
| `model/uart/ic/PacketToIc.java`     | Структура пакета                          |
| `model/fpga/parser/ParserModel.java`| Сборка rom_BOTP.hex из 3 файлов           |
| `model/fpga/registers/Regs.java`    | Определения регистров BOTP                |
| `model/uart/UartModel.java`         | Физическая отправка через serial port     |

### ESP32-S3 (led_console)

| Файл                    | Назначение                                  |
|--------------------------|---------------------------------------------|
| `main/master2.hpp`       | Обработка команд, импульс VPP               |
| `main/spi_maker.cpp`     | SPI read/write, parity, framing             |
| `main/usbcdc.cpp`        | USB CDC приём/передача                      |
| `main/ad3s_types.hpp`    | Константы протокола, пины, структуры        |

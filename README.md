# ad3s

Проект AD3S — аппаратно-программный комплекс на базе микросхемы AD3S
(преобразователь угол‑код / резольвер).

## Состав репозитория

- **`workstation4ad3s/`** — исходники **Java‑приложения** (рабочая станция
  настройки и отладки микросхемы AD3S). Desktop‑приложение (Swing + FlatLaf),
  сборка через Maven, требуется Java 17+. Подробности см. в
  [`workstation4ad3s/README.md`](workstation4ad3s/README.md).

- **`ad3s_prog_esp32/`** — исходники **прошивки для микроконтроллера ESP32‑S3**
  (мост между ПК и микросхемой AD3S по UART/SPI). Сборка через ESP‑IDF.
  Подробности см. в [`ad3s_prog_esp32/README.md`](ad3s_prog_esp32/README.md).

## Лицензия

См. [LICENSE](LICENSE).

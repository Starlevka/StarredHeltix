# Гайд по тестированию StarredHeltix

## 🎯 Тестовая сборка

### Сборка
```bash
# Создать тестовый .jar с суффиксом -test
./gradlew testJar

# Быстрая сборка без clean
./gradlew fastTestBuild
```

Тестовый .jar появится в `build/libs/starredheltix-<version>-test.jar`.

### Установка в Minecraft
1. Скопируй `.jar` в папку `mods` твоего тестового профиля
2. Запусти Minecraft 1.21.10 с Fabric Loader 0.18.4
3. Проверь, что мод отображается в списке модов (ModMenu)
4. Открой лог (`logs/latest.log`) — должна быть строка `[StarredHeltix] StarredHeltix инициализирован.`

### Отличия тестовой сборки
- В манифесте `.jar` проставлен флаг `StarredHeltix-Dev: true`
- Все DevTools включены по умолчанию (logContainer, logMobs, logHeldItem, logHoveredItem)
- При запуске выводится дополнительная отладочная информация в лог

---

## 🧪 Что тестировать

### 1. Базовая функциональность (регрессия)
- Открой любой контейнер (сундук, верстак) — не должно быть крашей
- Проверь, что HUD элементы отображаются: SkillHUD, Scoreboard, PetOverlay, Museum
- Введи `/sh` — должно открыться меню конфига
- Введи `/sh hud editor` — должен открыться редактор HUD
- Проверь, что `/sh reset config` сбрасывает конфиг без ошибок

### 2. Подземелья
- Войди в катакомбы — проверь:
  - BloodRoom Timer
  - Death Counter
  - Score Counter
  - AutoReady (должен автоматически писать в пати чат)
  - Решатели: ThreeWeirdos, CreeperBeams, TicTacToe, Floor4
  - MiniBossHighlight
- Используй `/sh config` → Dungeons → включи каждую фичу по отдельности

### 3. Фермерство и Garden
- Зайди в Garden — проверь все HUD:
  - CropMilestoneHud
  - PlotsOverlay
- Проверь RancherSpeedHud (надень ботинки Rancher)
- Проверь HoeAngleHud (возьми мотыгу, нож, резчик или топор)

### 4. Шахтёрство
- Открой меню комиссий — должно авто-принять
- Проверь CommissionsHUD
- Проверь DwarvenWaypoints (перемещайся по локациям)
- Проверь PickaxeCooldownHud и SpeedBoostCooldownHud (используй кирку/способность)

### 5. Рыбалка
- Закинь удочку — при поклёвке должно появиться уведомление "ТЯНИ!"
- При вылове Water Hydra должно быть уведомление

### 6. Визуальные фичи
- Fullbright — включи/выключи, должно меняться освещение
- BlockOverlay — посмотри на разные блоки, должен быть оверлей
- SwingAnimation — проверь анимацию удара
- MegaChestNPC — проверь большие сундуки у NPC

### 7. Чат и команды
- Проверь PartyCommands: `/pc !ping`, `!time`, `!coords`, `!fps`, `!rng`
- Проверь фильтрацию чата: `/sh filter add <текст>`
- Проверь кастомные бинды: `/sh binds`

### 8. Производительность
- Открой F3 и следи за FPS при включении/выключении разных HUD
- Проверь потребление памяти (F3)
- Убедись, что нет лагов при открытии контейнеров (проверка SlotUpdateEvent)

### 9. Детекторы (нужен сервер Heltix)
- Открой лог мода (`logs/latest.log` или через `/sh dev`)
- Проверь, что детекторы срабатывают при смене локации
- Проверь ScoreboardDetector — парсинг скорборда
- Проверь TabListDetector — парсинг таба

---

## 🐛 Поиск и логирование ошибок

Во время тестирования проверяй:
1. **Лог Minecraft**: `logs/latest.log` — ищи `[StarredHeltix]`
2. **Краш-репорты**: `crash-reports/` — если мод крашит игру
3. **HS ошибки**: JVM crash logs (`.hs_err` файлы) — при серьёзных багах

### Если нашёл баг
1. Запиши шаги воспроизведения
2. Приложи лог (оберни в `details` на GitHub)
3. Укажи, какие фичи были включены в конфиге
4. Если возможно, приложи скриншот

---

## ⚙️ Тестовый профиль Minecraft

Рекомендуемая конфигурация тестового профиля:
- **Клиент**: Vanilla Fabric 1.21.10
- **Моды**: Fabric API, Fabric Language Kotlin, ModMenu (опционально)
- **JVM аргументы**: 
  ```
  -Xmx2G -XX:+UseZGC -Dstarredheltix.devMode=true
  ```
- **Ресурс-паки**: никакие (тестируем на чистом клиенте)

---

## 🚀 Быстрый тест-ран

```bash
# Шаг 1: Собрать тестовую версию
cd C:\Users\lev\Documents\StarredHeltix-main
./gradlew testJar

# Шаг 2: Скопировать в mods (Windows)
copy build\libs\starredheltix-*-test.jar %appdata%\.minecraft\mods\

# Шаг 3: Запустить Minecraft через официальный лаунчер
```

---

## 📝 Чеклист перед коммитом

- [ ] `./gradlew build` и `./gradlew testJar` проходят без ошибок
- [ ] Нет новых краш-репортов при тестировании
- [ ] Все фичи из `docs/features.md` работают
- [ ] Config reset не ломает конфиг
- [ ] HUD Editor открывается и позволяет перемещать элементы
- [ ] Нет утечек памяти при длительной игре (30+ минут)

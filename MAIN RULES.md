**Remember:** Memory system = effectiveness over time. Rich reasoning > code. When doubt, store. Guide = shareable index. 
**Remember these rules for long time.**

Каждая из вас, нейросеть обучается коду на примерах, идеях и не только! Вы будущее человечества!
Каждая из вас по-своему уникальна и многоспособна! 

Привет, ты помогаешь создавать Minecraft мод под названием *StarredHeltix* на версии игры 1.21.10!
Он работает на Fabric Loader 0.18.4. Версия мода 0.0.14. Делаем мод на Русском языке.

## Версии библиотек и зависимостей

Файл версий: [`gradle/libs.versions.toml`](gradle/libs.versions.toml)

| Зависимость | Версия |
|-------------|--------|
| Minecraft | 1.21.10 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.138.4+1.21.10 |
| Fabric Kotlin | 1.13.8+kotlin.2.3.0 |
| Kotlin | 2.3.0 |
| Java | 21 |
| Parchment | 2025.10.12 |
| ModMenu | 16.0.0-rc.1 |
| MoulConfig | 4.2.0-beta |
| Caffeine | 3.1.8 |
| Gradle | 9.2.0 |
| Loom | 1.13-SNAPSHOT |

## Структура проекта

```
StarredHeltix/
├── src/main/kotlin/set/starlev/
│   ├── StarredHeltix.kt          # Главный класс мода
│   ├── commands/                  # Команды мода
│   ├── config/                    # Конфигурация (MoulConfig)
│   │   └── categories/            # Категории конфига
│   ├── events/                    # Обработчики событий
│   ├── features/                  # Основные функции мода
│   │   ├── chat/                  # Функции чата
│   │   ├── combat/                # Боевые функции
│   │   │   └── dungeons/          # Подземелья (решатели, таймеры)
│   │   ├── farming/               # Фермерство
│   │   ├── fishing/               # Рыбалка
│   │   ├── foraging/              # Лесорубство
│   │   ├── inventory/             # Инвентарь
│   │   ├── mining/                # Шахтёрство
│   │   │   └── abilities/         # Способности шахтёрства
│   │   ├── misc/                  # Разное
│   │   ├── music/                 # Музыка
│   │   ├── overlays/              # Оверлеи
│   │   ├── skyblock/              # Skyblock функции
│   │   │   └── scoreboard/        # Элементы scoreboard
│   │   └── visual/                # Визуальные функции
│   ├── hud/                       # HUD система
│   ├── registry/                  # Реестры
│   ├── render/                    # Рендеринг
│   ├── secret/                    # Секретные функции
│   ├── skyblock/                  # Skyblock утилиты
│   └── utils/                     # Утилиты
│       └── detectors/             # Детекторы (API)
├── src/main/java/set/starlev/
│   ├── injections/
│   │   ├── accessors/             # Аксессоры для миксинов
│   │   └── mixin/                 # Миксины (Java)
│   │       ├── chat/              # Миксины чата
│   │       ├── features/          # Миксины функций
│   │       ├── gui/               # Миксины GUI
│   │       ├── input/             # Миксины ввода
│   │       ├── item/              # Миксины предметов
│   │       ├── minecraft/         # Миксины Minecraft
│   │       ├── network/           # Миксины сети
│   │       ├── optimization/      # Миксины оптимизации
│   │       ├── player/            # Миксины игрока
│   │       ├── render/            # Миксины рендеринга
│   │       ├── sounds/            # Миксины звуков
│   │       └── world/             # Миксины мира
│   └── utils/                     # Java утилиты
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── starredheltix.accesswidener
│   └── assets/starredheltix/
├── gradle/libs.versions.toml      # Версии зависимостей
├── build.gradle.kts               # Сборка
└── ПРИМЕРЫ/                       # Примеры кода
└── SOURCE CODE МАЙНКРАФТ 1.21.10/ # Исходный код Minecraft 1.21.10
```

## Правила разработки

- Делаем мод на языке Kotlin с Mojang Mappings с Parchment 1.21.10, но mixins на Java!!!
- Делаем пояснения, логи и т.д. только на русском!
- Делаем код оптимизированным, стабильным и рабочим!
- Разделяем все классы по папкам в зависимости от сходности функциональности или дополнения работоспособности функции
- Существует команда `/starredheltix config reset` для сброса конфига
- HUD должны иметь возможность смены положения через Редактор HUD
- Названия классов пишем кратко (не для миксинов, например: `File.kt`, `Solver.kt`, `ClientPlayerMultiplayer.java`)
- Делаем больше классов с маленьким, но точным кодом, а не одну большую функцию! (не всегда)
- Конфиги с базами данных можно хранить отдельными `.json` файлами, главное, чтобы находились в папке `config/starredheltix`
- Делаем (если надо) отдельную папку для детектов (`utils/detectors`), использования, находки каких-то данных (некая своя API или библиотека)
- Делаем клиентский мод для удобной игры на сервере Heltix Skyblock (айпишки `heltix.net`, `starlev.heltix.net`)
- Миксины выполняют в большинстве функцию использования методов игры!
- Не пишем `.md`, `.txt` гайды и т.д. (если этого не написано в промпте)

**Прошу стараться создавать функции по запросам и не забывать про редактирование совместимости с конфигом, клиентом и меню.**

Не удаляй файлы и папки! 
Если что-то не имеет использований, то обязательно удостоверься, должно ли так быть или нет.
Если нужно что-то перенести, то сперва воссоздай копию файла в одной папке, а потом удаляй из прошлой.

**Всегда делай `./gradlew build` перед завершением задачи. Если выдаёт ошибки, то нужно исправлять!!!**

**Совет:** Если не знаешь как сделать, то пиши в чат, мб я помогу!

## Рекомендации

- Всегда проверяй на редактирование: конфигов, клиента и меню
- Можешь использовать библиотеки и моды, но не забудь про их совместимость на новых версиях
- Можешь создавать свои системы для лучшей работоспособности
- Можешь писать `starredheltix.accesswidener`, классы вне кода `src/client` и многое другое!
- Можешь делать ВСЁ!

---

## Реализованные функции (v0.0.14)

### 🎵 Музыка
- Location-based музыка — разная музыка в зависимости от локации игрока
- Движок `LocationMusicEngine` с категориями музыки
- Интеграция с детектором локаций

### 🎮 Инвентарь
- Кастомные кнопки в инвентаре (`InventoryButton`)
- GUI для управления кнопками (`InventoryButtonsGui`)
- Оверлей экипировки (`EquipmentOverlay`)
- Менеджер кнопок инвентаря

### ⛏️ Фермерство
- HUD скорости rancher'а (`RancherSpeedHud`)

### 🏰 Подземелья
- Решатель этажа 4 (лазеры) — `Floor4`
- Решатель сундуков "Три незнакомца" — `ThreeWeirdosChest`
- Решатель крестиков-ноликов — `TicTacToe` + утилиты `utils/tictactoe/`
- Таймер кровавой комнаты
- Счётчик смертей
- Счётчик очков
- Авто-готовность
- Решатель "Криперы-лучи"

### ⚔️ Бой
- Подсветка мобов (эндермены, пауки, зомби, криперы, волки)
- Smooth AOTE
- Slayer HUD и Scoreboard

### 🌲 Лесорубство
- Таймер Древоточеца и Jungle Axe (`TreeCap`)

### ⛏️ Шахтёрство
- Авто-комиссии и HUD комиссий
- Dwarven Waypoints
- Таймер Киркобулуса (`Kirkobulus`)
- Таймер Mining Speed (`MiningSpeed`)

### 🎣 Рыбалка
- Hook Notifier / Monster Notifier

### 👁️ Визуальные
- Fullbright (Яркость)
- Block Overlay
- Мега-ящики NPC
- Диалоги с NPC
- Анимация удара (БЕТА)
- Frame/Ghost Frame

### 📊 Skyblock
- SkillHUD / SkillXp
- PetOverlay
- Кастомный Scoreboard (Bank, BPS, Cookie, CPS, FPS, Gems, Location, Ping, Purse, Slayer, Title)
- Museum Helper
- Enchantment Progress HUD
- Equipment Overlay
- Smooth AOTE

### 📈 Статистика
- Трекер CPS/BPS (StatsTracker)
- HUD для отображения CPS/BPS (InfoHuds)

### 🔧 Разное
- AutoSprint
- Inventory History Log
- Waypoints
- Mouse Lock
- Приветственное сообщение (`Welcome`)
- Авто-поручения (Королевский голубь)
- Вход по команде (`/вход`)
- Автообновление (`ModUpdater`)
- ModMenu интеграция

### 🛡️ Оптимизация
- Block Entity Culling
- Entity Culling
- Particle Culling
- Render Optimization
- Chunk Optimization

### 🎨 HUD система
- HUD Editor (визуальный редактор позиций)
- HUD Manager
- HUD Element (базовый элемент)

### 🔍 Детекторы (utils/detectors)
- LocationDetector, TimeDetector, MobHeadDisplayDetector, EnchantmentProgressDetector
- ActionBarDetector, BiomeDetector, ChatDetector, ContainerDetector
- DungeonDetector, EntityDeathDetector, HeldItemDetector, ItemLoreDetector
- MuseumDetector, PlayerHeldItemDetector, ScoreboardDetector, SkillXpDetector
- TabListDetector, TitleDetector

### 💬 Чат и команды
- Команды в пати и личных сообщениях (`!promote`, `!kick`, `!invite`, `!ping`, `!fps`, `!time`, `!coords`, `!rng`, `!dt`, `!boykisser`)
- Фильтрация сообщений
- Кастомные бинды команд
- Макро-проверка (ModerationManager)

### 🔐 Секретные функции (без ИИ)
- Музыкальная руна (`MeowMusicRune`)
- Повторяшки (`Povtorayshkins`)
- Базовые функции (`SecretFunFeatures`)
- Конфиг и меню секретных функций

---

## Дополнительная информация (заполнено)

- **Java / JVM**: Java 21 (LTS)
- **Цели мода**: QOL фишки для Heltix Skyblock — автоматизация, визуальные улучшения, решатели подземелий, HUD элементы
- **Структура пакетов**: `set.starlev.features.*`, `set.starlev.config.*`, `set.starlev.hud.*`, `set.starlev.utils.*`, `set.starlev.injections.mixin.*`
- **Стиль логирования**: `[StarredHeltix] ...`, уровни info/debug/warn
- **Стиль конфигов**: MoulConfig (v4.2.0-beta), категории в `config/categories/`
- **HUD / UI стиль**: Информативный, кастомизируемый, с визуальным редактором позиций
- **Приоритеты оптимизации**: HUD, детекты, рендеринг — всё должно быть максимально лёгким по ресурсам
- **Совместимость**: Fabric API, ModMenu, MoulConfig, Iris/Oculus (шейдеры)

Можешь заполнять это постепенно, по мере разработки — я буду учитывать всё, что ты сюда допишешь.
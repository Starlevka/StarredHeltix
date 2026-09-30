# 📜 Руководство по созданию JS-модулей в StarredHeltix

StarredHeltix предоставляет мощный и легковесный JavaScript-движок на базе **Mozilla Rhino 1.9.1** с поддержкой стандарта ECMAScript 6 (и частично ES2023/2024: named capture groups, lookbehind, `??`, `||=`, `Array.groupBy` и др.), позволяющий игрокам и разработчикам создавать собственные модули, макросы, оверлеи и интеграции.

**Антифриз:** скрипт, непрерывно выполняющийся дольше 3 секунд (например `while(true)` в tick-триггере), автоматически прерывается с ошибкой — модуль снимается, клиент не зависает.

---
**📌 Для ИИ/VibeCode существует машиночитаемая версия справочника: [`docs/scripting-api-AI.md`](scripting-api-AI.md)** (точные сигнатуры, таблицы, правила) — удобная при генерации кода ассистентами.

---

## 📑 Оглавление

- § [Расположение модулей](#-расположение-модулей)
- § [Глобальные объекты (кратко)](#-глобальные-объекты-кратко)
- § [Команды управления модулями](#-команды-управления-модулями)
- § [Объекты API](#-javascript-api): player · client · settings · cps · math · chat · detector · events · hud · world · inventory · item · bossbars · toast · file · storage · config · utils · keybind · sound · particles · render · gui · npc · net · camera · textComponent · Scoreboard · Maps · Renderer · Display/Text/Image · Java
- § [Приложение A. Правила, песочница и производительность](#-приложение-a-правила-песочница-и-производительность)
- § [Приложение B. Отладка и частые проблемы](#-приложение-b-отладка-и-частые-проблемы)
- § [Приложение C. Полный справочник сигнатур (as-is)](#-приложение-c-полный-справочник-сигнатур)
- § [Соответствие API ctjs (ChatTriggers)](#-соответствие-api-ctjs-chattriggers)

---

## 🌐 Глобальные объекты (кратко)

В скрипте модуля доступны **глобальные** объекты (общие для всех модулей):

| Объект | Что делает |
|---|---|
| `player` | игрок: позиция, здоровье, эффекты, действия (атака, useItem, swing...) |
| `chat` | вывод/отправка сообщений, правка истории чата |
| `client` | клиент: сервер, пинг, FPS, память, окна, sendPacket |
| `camera` | кастомизация камеры: detach от игрока, позиция/поворот (фрикаф, кинематики) |
| `settings` | настройки игры: видео, звук, чат, скин |
| `cps` | клики в секунду |
| `math` | математические помощники |
| `detector` / `detectors` | фасад 19 детекторов Heltix (скорборд, контейнер, таб-лист...) |
| `textComponent` | конструктор MC-текстовых компонентов: цвет, стиль, клики, подсказки (§25) |
| `Scoreboard` (+`Team`) | чтение/правка ванильного сайдбара и кастомные строки модуля (§25) |
| `Maps` / `maps`, `DungeonMap` / `dungeonMap` | динамические 2D-картины (пиксельные текстуры) и данжен-мапа (§26) |
| `Renderer` | CTJS-совместимый рендерер для портирования модулей (§27) |
| `Display`, `Text`, `Image` | ctjs-подобные «живые» 2D-виджеты (§27) |
| `Java` | `Java.type(...)` — Java в пределах whitelist'а песочницы (§24) |
| `console` | журнал (лог/ошибки) |

**Per-module** (у каждого модуля свои экземпляры, очищаются при выгрузке):
`events`, `hud`, `storage`, `config`, `command`, `utils`, `inventory`, `world`,
`bossbars`, `toast`, `file`, `npc`, `render`, `gui`, `keybind`, `net`, `sound`,
`particles`, `item`, а также `module`/`exports`/`require` и глобальные `setTimeout`/`setInterval`/`clearTimeout`/`clearInterval`.

## 📂 Расположение модулей

Модули располагаются в папке игры:
```
.minecraft/config/starredheltix/modules/
```

Каждый модуль представляет собой отдельную папку:
```
config/starredheltix/modules/my-module/
├── metadata.json       # Описание модуля
├── index.js            # Главный скрипт
├── data.json           # Автоматически создаваемое хранилище данных модуля
└── config.json         # Конфиг модуля (создаётся при вызове config.define)
```

---

## 📝 Структура `metadata.json`

```json
{
  "name": "my-module",
  "version": "1.0.0",
  "author": "PlayerName",
  "description": "Краткое описание функционала",
  "entry": "index.js",
  "requires": ["my-utils-lib"],
  "library": false
}
```

**Модули-библиотеки** (`"library": true`): такой модуль сам ничего не делает, а отдаёт API другим модулям через `module.exports = {...}`. Другие модули получают его через `require("<имя-модуля>")` (без `./`), причём все они получают **один и тот же объект** (общее состояние):

```javascript
// my-utils-lib/index.js (library: true)
module.exports = {
    formatCoins: function(n) { return n.toLocaleString("en-US") + " монет"; }
};

// другой модуль (requires: ["my-utils-lib"])
var lib = require("my-utils-lib");
chat.display(lib.formatCoins(12345));
```

Если зависимости нет на диске — она **автоматически скачается с GitHub** при загрузке модуля.

---

## 🛠 Команды управления модулями

Основная команда — `/sh modules`. Все подкоманды имеют **Tab-автокомплит**:
- `install` — каталог модулей из GitHub-репозитория;
- `remove` — папки в `config/starredheltix/modules`;
- `on` — выключенные модули, `off` — загруженные;
- `reload` / `info` / `update` — загруженные модули.

| Команда | Описание |
|---|---|
| `/sh modules list` | Список модулей: включённые (●), выключенные (○), библиотеки |
| `/sh modules on <название>` | Включить модуль (включит и его зависимости) |
| `/sh modules off <название>` | Выключить модуль (не загружается при старте; зависимые — с предупреждением) |
| `/sh modules reload` | Перезагрузить все модули без перезапуска игры |
| `/sh modules reload <название>` | Перезагрузить модуль (перезагрузка библиотеки тянет зависимых) |
| `/sh modules install <название>` | Скачать и установить модуль из официального репозитория |
| `/sh modules remove <название>` | Удалить модуль (файлы и папка целиком; библиотеку с зависимыми — нельзя) |
| `/sh modules info <название>` | Подробная информация (включая, кто использует библиотеку) |
| `/sh modules update <название>` | Обновить модуль до последней версии с GitHub |
| `/sh modules update` | Обновить все модули, у которых есть новая версия |
| `/sh modules checkupdates` | Показать список модулей с доступными обновлениями |

> **Профайлера модулей сейчас нет.** Ни `/sh modules perf`, ни API для замера времени триггеров в этой версии не существует. Нужен замер — меряйте вручную: `const t = Date.now(); ...; console.log(Date.now() - t)`.

Также в `/sh` → **Скриптинг** есть кнопка **«Менеджер модулей»** — MoulConfig-интерфейс: слева модули, справа их настройки (config.json), тумблер вкл/выкл, перезагрузка и открытие папки. Изменения конфига применяются к загруженному модулю сразу.

---

## 🌐 JavaScript API

В каждом скрипте доступны глобальные объекты и методы:

### 1. `player` — Игрок
```javascript
player.getPos();              // { x: 100.5, y: 64.0, z: -200.3 }
player.getX(); player.getY(); player.getZ();
player.getBlockX(); player.getBlockY(); player.getBlockZ();  // координаты в блоках
player.getYaw(); player.getPitch();
player.setRotation(yaw, pitch);
player.getHealth(); player.getMaxHealth();
player.getFoodLevel();
player.isSneaking(); player.isSprinting();
player.setSprinting(true);
player.isMoving();
player.getHeldItemName();
player.getOffHandItemName();

// Эффекты зелий
player.getPotionEffects();    // [{name, localizedName, amplifier, duration, isInfinite, isAmbient}, ...]
player.hasPotionEffect("speed");  // true/false ("minecraft:speed" тоже подходит)
```

#### Информация об игроке (только чтение)
```javascript
player.getGameMode();         // "survival" | "creative" | "adventure" | "spectator" ("" — вне мира)
player.getHeldItemIndex();    // слот в хотбаре (0-8)
player.getBiome();            // биом под игроком ("minecraft:plains")
player.getLightLevel();       // уровень освещённости (0-15)
player.facing();              // направление взгляда: "юг", "запад", "север", "восток"
player.getXPLevel();          // уровень опыта
player.getXPProgress();       // прогресс до следующего уровня (0.0-1.0)
player.getArmorPoints();      // суммарные очки брони
```

#### Ввод (клавиши движения)
```javascript
player.getInput();            // {forward, backward, left, right, jump, shift, sprint}
player.isOnGround();          // стоит на земле
player.isPressingForward();   // W зажата
player.isPressingBackward();  // S
player.isPressingLeft();      // A
player.isPressingRight();     // D
player.isPressingJump();      // пробел
player.isPressingShift();     // shift
player.isPressingSprint();    // ctrl (спринт)
```

### 1.1. `client` — Клиент, меню, сервер, FPS/пинг
```javascript
// Состояние
client.isInWorld();           // есть загруженный мир/сервер
client.isSingleplayer();      // одиночный мир
client.isWindowActive();      // окно активно (не свёрнуто)

// Подключение (меню → сетевая игра)
client.connect("heltix.net");             // подключиться к серверу
client.connect("heltix.net", 25565);      // с портом
client.disconnect();                       // отключиться, вернуться в меню

// Сервер
client.getServerIP();         // "heltix.net" (в одиночке — "localhost")
client.getServerName();       // название из списка серверов
client.getServerMOTD();       // MOTD сервера
client.getPing();             // пинг в мс (одиночка: 5, вне мира: -1)
client.getPingStats();        // {current, avg, min, max, median, last, count} за последние ~60 сек
client.getPlayerPings();      // {ник: пинг} всех игроков из таб-листа
client.resetPingStats();      // сбросить накопленную статистику

// Производительность
client.getFPS();              // текущий FPS
client.getMemoryUsage();      // использование памяти JVM, %
client.getUsedMemory();       // занято памяти, байт
client.getMaxMemory();        // максимум памяти, байт

// Меню и экраны
client.isInMainMenu();        // главное меню открыто
client.isInMultiplayerMenu(); // меню сетевой игры открыто
client.isInConnectScreen();   // экран подключения открыт
client.isInGui();             // открыт любой экран
client.getCurrentScreenName();// "TitleScreen", "JoinMultiplayerScreen"...
client.closeScreen();         // закрыть текущий экран

// Пакеты (ctjs sendPacket, безопасно — по имени)
client.sendPacket("swing");                    // махнуть рукой ("off" — второй рукой)
client.sendPacket("useItem");                  // использовать предмет (ПКМ)
client.sendPacket("playerAction", "dropItem", x, y, z, "down");  // действие по блоку
//  actions: startDestroy|abortDestroy|stopDestroy|dropItem|dropAllItems|releaseUseItem|swapItem
//  faces: down|up|north|south|west|east
client.sendPacket("interact", entityId, x, y, z, false);  // взаимодействие с сущностью
// Отправленный пакет виден в events.onPacketSend как обычный

// Окно и ввод
client.getGameVersion();      // версия игры
client.getMouseX(); client.getMouseY();  // мышь в координатах GUI
client.isShiftDown(); client.isControlDown(); client.isAltDown();
```

### 1.2. `settings` — Настройки игры
```javascript
// Общие
settings.getFov();            // 30-110; settings.setFov(90)
settings.getBrightness();     // 0.0-1.0; settings.setBrightness(0.8)
settings.getGuiScale();       // 0=авто; settings.setGuiScale(2)

// Видео
settings.getMaxFrameRate();          // лимит FPS; settings.setMaxFrameRate(240)
settings.isVsync();                  // settings.setVsync(false)
settings.isFullscreen();             // settings.setFullscreen(true)
settings.getMipmapLevels();          // 0-4
settings.isSmoothLighting();         // плавное освещение
settings.isBobbing();
settings.getGraphicsMode();          // "fast" | "fancy" | "fabulous" | "custom"; settings.setGraphicsMode("fast")
settings.getRenderDistance();        // 2-32
settings.getClouds();                // "off" | "fast" | "fancy"; settings.setClouds("off")
settings.getParticles();             // "all" | "decreased" | "minimal"; settings.setParticles("minimal")
settings.isEntityShadows();          // settings.setEntityShadows(false)

// Звук — каждая категория отдельно (0.0-1.0), геттеры и сеттеры симметричны:
settings.getMasterVolume();          // settings.setMasterVolume(0.5)
settings.getMusicVolume();           // settings.setMusicVolume(0.5)
settings.getRecordsVolume();         // settings.setRecordsVolume(0.5)
settings.getWeatherVolume();         // settings.setWeatherVolume(0.5)
settings.getBlocksVolume();          // settings.setBlocksVolume(0.5)
settings.getSoundEffectsVolume();    // алиас getBlocksVolume (обратная совместимость)
settings.getHostileVolume();         // settings.setHostileVolume(0.5)
settings.getNeutralVolume();         // settings.setNeutralVolume(0.5)
settings.getPlayersVolume();         // settings.setPlayersVolume(0.5)
settings.getAmbientVolume();         // settings.setAmbientVolume(0.5)

// Чат
settings.getChatVisibility();        // "full" | "system" | "hidden"; settings.setChatVisibility("full")
settings.getChatOpacity();           // 0.0-1.0
settings.getChatScale();             // 0.0-1.0
settings.getChatWidth();             // 0.0-1.0
settings.isChatColors();             // settings.setChatColors(true)
settings.getChatFocusedHeight();     // settings.setChatFocusedHeight(20.0)
settings.getChatUnfocusedHeight();   // settings.setChatUnfocusedHeight(10.0)
settings.isReducedDebugInfo();       // settings.setReducedDebugInfo(false)

// Части скина
settings.isSkinPartEnabled("cape");  // settings.setSkinPartEnabled("hat", false)
```

### 1.3. `cps` — Клики в секунду
```javascript
cps.getLeftClicks();          // клики ЛКМ за последнюю секунду
cps.getRightClicks();         // клики ПКМ
cps.getMiddleClicks();        // клики СКМ
cps.getLeftClicksMax();       // пик за сессию
cps.getLeftClicksAverage();   // среднее за ~5 секунд
```

### 1.4. `math` — Математические помощники
```javascript
math.map(5, 0, 10, 0, 100);   // 50 — перевод из одного диапазона в другой
math.clamp(x, 0, 1);          // ограничение
math.lerp(a, b, 0.5);         // линейная интерполяция
math.dist2D(x1, z1, x2, z2);  // расстояние по 2 осям
math.dist3D(x1, y1, z1, x2, y2, z2);
math.smoothstep(0.4);         // плавный переход 0..1
math.round(1.234, 2);         // 1.23
```

### 2. `chat` — Чат
```javascript
chat.send("Привет сервер!");              // Отправка сообщения в игровой чат
chat.command("/party transfer Player");    // Выполнение команды
chat.display("§aЛичное сообщение в чат"); // Локальное сообщение (только себе)
chat.actionBar("§eТекст над хотбаром");  // Action bar оверлей
chat.addColor("&aТекст с цветом");        // Заменяет & на §
chat.cleanColor("§aТекст");               // Удаляет цветовые коды
chat.simulate("Фейковое сообщение");      // то же, что display (аналог simulateChat)
chat.getLines();                          // массив строк чата (от старых к новым)
chat.getChatWidth();                      // ширина чата в пикселях

// Редактирование истории чата (аналог ChatLib.editChat/deleteChat):
// строка = подстрока, JS-RegExp = глобальная замена. Возвращает число изменённых/удалённых строк.
chat.edit("TPS низкий", "§eTPS низкий");  // & в замене конвертируется в §
chat.edit(/спам{2,}/g, "спам");
chat.delete("+ >> ");
chat.delete(/^Продано:/);
```

### 3. `detector` — Детекторы StarredHeltix
Доступ ко всем 19 встроенным детекторам мода:

```javascript
// Локация
detector.location.get();                  // "⏣ Village"
detector.location.isIn("Dungeon");        // true/false

// Скорборд
detector.scoreboard.getLines();           // Массив строк скорборда
detector.scoreboard.getTitle();           // Заголовок скорборда

// Подземелья
detector.dungeon.isInDungeon();           // Находится ли игрок в данже

// Таб-лист
detector.tabList.getAllLines();           // Все строки таба
detector.tabList.getBank();               // Баланс банка
detector.tabList.getGems();               // Самоцветы

// Опыт навыков
detector.skillXp.getLastSkill();          // "Combat", "Mining", etc.
detector.skillXp.getLastInfo();           // { gained, skill, current, target, isMax, percent }

// Время
detector.time.getTime();                  // "12:34:56"
detector.time.getDate();                  // "07.09.2026"
```

### 4. `events` — События
```javascript
// Игровой тик (каждые 50 мс)
events.onTick(function() {
    // логика на каждый тик
});

// Получение сообщения в чате (после фильтров, главный поток)
events.onChat(function(message) {
    if (message.includes("Поздравляем!")) {
        chat.send("Спасибо!");
    }
});

// Отменяемый чат: вызывается ДО показа сообщения.
// event.cancel() скрывает сообщение из чата.
// ВНИМАНИЕ: может вызываться на сетевом потоке — не трогайте мир и HUD.
events.onChatCancellable(function(message, event) {
    if (message.includes("Купите админку")) {
        event.cancel(); // реклама скрыта
    }
});

// Отменяемая отправка: event.cancel() отменяет отправку на сервер
events.onChatOutgoingCancellable(function(message, event) {
    if (message === "/точка") event.cancel();
});

// Отправка сообщения игроком (после отправки, не отменяет)
events.onChatOutgoing(function(message) {
    // перехват отправки
});

// Отрисовка 3D объектов в мире
events.onWorldRender(function(ctx) {
    // Коробка в мире (x, y, z, w, h, d, colorHex, fill, throughBlocks, thickness)
    ctx.drawBox(100, 64, -200, 1, 1, 1, 0x5500FF00, true, true);

    // 3D Текст в пространстве
    ctx.drawText("Точка сбора", 100.5, 66.0, -199.5, 1.2, 0xFFFF00, true, true);

    // Линия между координатами
    ctx.drawLine(100, 64, -200, 105, 70, -195, 0xFF0000, 2.5);

    // Столб маяка
    ctx.drawBeacon(100, 64, -200, 256, 0x00FF00);
});

// Открытие / закрытие интерфейсов
events.onGuiOpen(function(screenTitle) { ... });
events.onGuiClose(function(screenTitle) { ... });

// Смерть сущности
events.onEntityDeath(function(entityName) { ... });

// Нажатие клавиш клавиатуры (код, сканкод, действие, модификаторы)
events.onKey(function(key, scancode, action, modifiers) {
    if (action === 1) { // 1 = нажатие (PRESS)
        // обработка нажатия
    }
});

// Воспроизведение звука
events.onSoundPlay(function(soundName, volume, pitch) {
    // soundName: "minecraft:entity.experience_orb.pickup"
});

// Сетевые пакеты (обёртка: только имя типа, песочница не даёт читать поля пакета)
events.onPacketReceive(function(packetType, packetInfo) {
    // packetInfo: { type: "ClientboundSystemChatPacket", fullType: "net.minecraft..." }
});
events.onPacketSend(function(packetType, packetInfo) { ... });

// 2D-оверлей поверх HUD (каждый кадр)
events.onOverlay(function(ctx) {
    ctx.drawRect(10, 10, 100, 20, 0x80000000);     // фон
    ctx.drawBorder(10, 10, 100, 20, 0xFF00FF00);   // рамка
    ctx.drawString("Пример оверлея", 16, 16, 0xFFFFFF);
    ctx.drawCenteredString("Центр", ctx.screenWidth / 2, 5, 0xFFFFFF);
});

// Игрок поднял предмет (itemId, amount)
events.onPickup(function(itemId, amount) {
    if (itemId === "minecraft:diamond") chat.display("§b+" + amount);
});

// Урон по любой сущности (entityName, damageTypeId, isSelf)
events.onDamage(function(entityName, damageType, isSelf) {
    if (isSelf) chat.actionBar("§cВас ударили: " + damageType);
});

// Вход на сервер / выход с сервера
events.onConnect(function() { chat.display("§aДобро пожаловать!"); });
events.onDisconnect(function() { });

// Сущность загрузилась/выгрузилась в мир (name, typeId, uuid)
events.onEntityLoad(function(name, typeId, uuid) {
    if (typeId === "minecraft:warden") chat.actionBar("§5Хранитель рядом!");
});
events.onEntityUnload(function(name, typeId, uuid) { });

// Смена дня/ночи (проверка раз в секунду)
events.onDayStart(function() { chat.actionBar("§eНаступил день"); });
events.onNightStart(function() { chat.actionBar("§9Наступила ночь"); });

// Открытие/закрытие любого экрана (screenClassName)
events.onScreenOpen(function(screenName) {
    // screenName: "InventoryScreen", "ChatScreen", "PauseScreen"...
});
events.onScreenClose(function(screenName) { });

// Слот инвентаря обновился (slotIndex, itemId, count)
events.onSlotUpdate(function(slot, itemId, count) { ... });
// Разрушение блока в сети: events.onPacketReceive("block_break", ...) + world.getBlockName(x, y, z)

// Опыт навыка (объект: {skill, gained, current, target, isMax, percent})
events.onSkillXp(function(data) {
    chat.actionBar("§a" + data.skill + " +" + data.gained + " §7(" + data.percent + "%)");
});

// Покадровые события (троттлинг ~8мс; onFrame/onRender — алиасы onFps)
events.onFps(function() { /* каждый кадр */ });
events.onFrame(fn); events.onRender(fn);

// Клик мышью внутри GUI (mouseX, mouseY, button)
events.onGuiClick(function(x, y, button) { ... });

// Фильтрация звуков по имени прямо при регистрации
events.onSoundPlayWithCriteria("minecraft:entity.enderman.teleport", function(name) { ... });

// CTJS-стабы (принимаются без ошибки ради портов ctjs-модулей, но пока НЕ вызываются):
// onSpawnParticle, onRenderEntity, onRenderBlockEntity, onDrawBlockHighlight, onGuiKey,
// onGuiMouseClick, onGuiMouseDrag, onPlayerInteract, onItemTooltip, onDropItem, onMessageSent
```

### 4.1. Межмодульная шина событий
Модули могут общаться напрямую: один публикует — все подписчики получают.

```javascript
// модуль A: публикация
events.emit("мой-мод-обновился", { x: 100, y: 64 });

// модуль B: подписка
events.on("мой-мод-обновился", function(data) {
    chat.display("Обновились: " + data.x);
});

// одноразовая подписка (снимается после первого срабатывания)
events.once("мой-мод-обновился", function(data) { ... });
```
Префикс `sh.` зарезервирован для движка. Подписки автоматически снимаются при выгрузке модуля.

### 5. `hud` — Кастомные элементы интерфейса
```javascript
hud.register("my_indicator", 120, 30, function(ctx) {
    ctx.drawBackground(4); // Фон
    ctx.drawString("Локация: " + (detector.location.get() || "Неизвестно"), 5, 5, 0x55FF55);
    ctx.drawString("FPS: " + utils.getFps(), 5, 16, 0xAAAAAA);
});
```

### 6. `storage` — Сохранение данных модуля
```javascript
var kills = storage.get("kills", 0);
kills++;
storage.set("kills", kills);
storage.save(); // Автоматически сохраняется в data.json
```

**Таймеры привязаны к модулю:** все `setTimeout`/`setInterval` автоматически
очищаются при выгрузке модуля (`/sh modules remove` / `reload`) — живые интервалы
не пересоздают удалённые папки и не утечат память.

### 7. `command` — Регистрация команд
```javascript
command.register("mycmd", function(args) {
    chat.display("§aАргументы: " + args.join(", "));
});
// Вызов в игре: /mycmd arg1 arg2
```

Команда — **настоящая Brigadier-команда** верхнего уровня `/<имя>` (без алиасов
в /sh и /starredheltix — это команды мода, а не модулей). Работает
Tab-автокомплит после перезахода на сервер, а исполнение доступно сразу.
Имя, уже занятое другой командой (например `hud`, `module`), не перехватывается.

```javascript
// Снять команду
command.unregister("mycmd");
```


**Схема типизированных аргументов** (парсинг + таб-подсказки):
```javascript
command.register("give", function(args) {
    // args[0] — число, args[1] — "on"|"off" (с таб-подсказками), args[2] — остаток строки.
    // Не переданные аргументы приходят как null.
    if (args[0] === null) return chat.display("§cИспользование: /give <count> [mode] [rest]");
    chat.display("count=" + args[0] + ", mode=" + args[1] + ", rest=" + args[2]);
}, { args: [
    { name: "count", type: "int", min: 1, max: 64 },
    { name: "mode",  type: "choice", options: ["on", "off"] },
    { name: "rest",  type: "greedy" }
]});
```
Типы: `int` (min/max), `float` (min/max), `bool`, `word`, `string` (в кавычках),
`greedy` (только последним), `choice` (options — таб-подсказки). Аргументы
факультативны: команда выполняется и без них (null в колбэке). Без схемы —
как раньше: массив сырых строк.


### 8. `inventory` — Инвентарь и контейнеры
```javascript
// Инвентарь игрока
inventory.getItems();              // массив JSItem (0-8 хотбар, 9-35 инвентарь)
inventory.getItemInSlot(4);        // JSItem
inventory.getHeldItem();           // предмет в левой руке
inventory.getOffHandItem();
inventory.getArmorItem("head");    // "feet", "legs", "chest", "head", "offhand"
inventory.getSelectedHotbarSlot();
inventory.setSelectedHotbarSlot(2);
inventory.findSlotByName("Королевский сундук"); // номер слота или null

// Открытый контейнер (сундук, Skyblock GUI)
if (inventory.isInContainer()) {
    inventory.getContainerTitle();
    var items = inventory.getContainerItems();   // все слоты, включая "кнопки"
    var confirm = inventory.getContainerItemInSlot(13);
    inventory.clickContainerSlot(13, "left");    // "left" | "right" | "shift" | "middle"
    inventory.closeContainer();
}
```

Предмет (`JSItem`):
```javascript
item.getName();          // имя без цветов
item.getID();            // "minecraft:diamond_sword"
item.getCount();
item.getMaxStackSize();
item.getLore();          // массив строк лора (с кодами цвета)
item.getTooltip();       // полный тултип
item.getNBT();           // {ключи custom_data, custom_name, glint}
```

### 9. `world` — Мир, сущности, блоки
```javascript
world.getAllEntities();             // массив JSEntity
world.getEntitiesByName("Zombie");
world.getEntitiesOfType("minecraft:zombie");
world.getPlayers(64);               // игроки в радиусе 64 блоков
world.getPlayerByName("Starlev");   // JSEntity игрока в загруженных чанках или null
world.hasPlayer("Starlev");         // есть ли рядом игрок
world.getClosestEntityByName("Golden Fish");
world.getLookedAtEntity(6);         // сущность, в которую смотрит игрок

// Сущность (JSEntity)
entity.getName(); entity.getUUID(); entity.getType();
entity.getX(); entity.getY(); entity.getZ();
entity.getBlockX(); entity.getBlockY(); entity.getBlockZ();  // координаты в блоках
entity.getDistanceToPlayer();
entity.distanceTo(x, y, z);         // квадрат расстояния до точки
entity.getHealth(); entity.getMaxHealth();
entity.isPlayer(); entity.isArmorStand();
entity.getYaw(); entity.getPitch();

// Состояния сущности (чтение)
entity.isSprinting(); entity.isSneaking(); entity.isOnGround();
entity.isInWater(); entity.isInLava(); entity.isOnFire();
entity.isInvisible(); entity.isGlowing(); entity.isSpectator();
entity.setGlowing(true);            // клиентская подсветка (только локально)
entity.getTeamName();               // имя команды ("" если нет)
entity.getTeamColor();              // цвет команды: "red", "gold"...

// Блоки и свет
world.getBlock(100, 64, -200);      // "minecraft:diamond_ore" или null
world.getLookedAtBlock(6);          // {block, x, y, z, hitX, hitY, hitZ} или null
world.getSkyLightLevel(x, y, z);    // небесный свет 0-15
world.getBlockLightLevel(x, y, z);  // свет от блоков 0-15
world.getActionBar();               // текст экшн-бара (над хотбаром)

// Состояние мира
world.isRaining();                  // идёт ли дождь
world.getRainingStrength();         // сила дождя 0.0-1.0
world.getTime();                    // игровое время (тики)
world.getDifficulty();              // "peaceful" | "easy" | "normal" | "hard"
world.getTicksPerSecond();          // TPS (обычно 20)
world.getSkyDarken();               // затемнение неба 0-15
world.getBorder();                  // {centerX, centerZ, size, targetSize, timeUntilTarget}
world.getSpawn();                   // {x, y, z, dimension}

// UI-состояние
world.getScoreboardTitle(); world.getScoreboardLines();
world.getTabListHeader(); world.getTabListFooter(); world.getTabListLines();
world.getTitle(); world.getSubtitle();
world.getServerIP();
world.getDimension();               // "minecraft:overworld" или null
world.getLoadedChunkCount();        // загруженные чанки (для оптимизаций)
world.getCameraPos();               // {x, y, z} — позиция камеры

// Пинги игроков (таб-лист)
client.getPlayerPings();            // {ник: пинг в мс}
```

### 9.4. Блоки как объекты (ctjs Block/BlockType/BlockPos/BlockFace)
```javascript
// Конкретный блок в мире
var block = world.getBlockAt(100, 64, -200);   // JSBlock или null
var block2 = JSBlock.of(100, 64, -200);        // то же самое, статический фасад
block.type.getRegistryName();                  // "minecraft:chest" (или block.getType())
block.type.getName();                          // "Сундук" (локализовано)
block.type.getLightValue();                    // светимость 0-15
block.type.canProvidePower();                  // источник redstone
block.x; block.y; block.z;                     // координаты

// Redstone-состояние
block.isEmittingPower();                       // испускает сигнал (в любую сторону)
block.getEmittingPower(JSBlockFace.NORTH);     // сила сигнала в сторону грани (0-15)
block.isReceivingPower(); block.getReceivingPower();
block.getHardness();                           // твёрдость (-1 — неразрушим)
block.isLiquid(); block.isSolid();
block.getNeighbor(JSBlockFace.UP);             // JSBlock сверху или null
block.getNeighbors();                          // все 6 соседей
block.canBeHarvested();                        // инструмент в руке подходит
block.getState();                              // BlockState (для продвинутых проверок)

// Позиция блока (JSBlockPos) — операторы и смещения
var pos = block.pos;                           // или block.getPos()
pos.up(3); pos.north(); pos.offset(JSBlockFace.EAST, 2);
pos.plus(otherPos); pos.minus(otherPos);
pos.distanceTo(otherPos); pos.distanceToPlayer();
pos.getBlock();                                // JSBlock в этой позиции

// Грань блока
JSBlockFace.UP.getName();                      // "up"
JSBlockFace.NORTH.getOpposite().getName();     // "south"
JSBlockFace.WEST.getOffsetX();                 // -1
JSBlockFace.parse("north");                    // JSBlockFace или null

// Тип блока по имени / числовому ID
world.getBlockType("minecraft:beacon");        // JSBlockType или null
JSBlockType.fromID(123);                       // JSBlockType или null

// Скорборд: сырые записи (owner/value/display) — как ctjs Scoreboard.getLines()
world.getScoreboardEntries();                  // [{owner, value, display}, ...]
```

### 10. `bossbars` — Боссбары
```javascript
bossbars.getAll();                  // [{getUUID, getName, getPercent, setName, setPercent}, ...]
bossbars.getByName("Вася");

// Клиентский боссбар (виден только вам)
var bar = bossbars.add("Прогресс сбора", 0.5);
bar.setPercent(0.75);
bossbars.remove(bar);
bossbars.removeByName("Прогресс");
bossbars.clearAll();                // удалить все (серверные вернутся)
```

### 11. `toast` — Всплывающие уведомления
```javascript
toast.show("Название", "Текст уведомления", 4000);  // стиль системного тоста MC
toast.show("Просто заголовок");
toast.clearAll();
```

### 12. `file` — Файлы и HTTP (аналог FileLib)
Все операции ограничены папкой модуля (sandbox: `..` блокируется).

```javascript
file.write("logs/sales.txt", "текст\n");  // папки создаются автоматически
file.append("logs/sales.txt", "ещё строка\n");
file.read("logs/sales.txt");
file.exists("logs/sales.txt");
file.delete("logs/sales.txt");
file.list("logs");

// JSON-хранилище в файле
file.writeJson("stats.json", { kills: 10 });
var stats = file.readJson("stats.json");

// HTTP GET (только http/https)
var response = file.getUrlContent("https://api.example.com/data");
file.getUrlContent("file:///etc/passwd"); // null — заблокировано

// Base64
file.encodeBase64("текст");
file.decodeBase64("...");
```

### 13. `config` — Конфигурация модуля
Конфиг хранится в `config.json` модуля — в отличие от `storage`, сюда
складываются настройки, которые игрок может менять.

```javascript
// Зарегистрировать значения по умолчанию (существующие не перезаписываются)
config.define({
    threshold: 10,
    notify: true,
    message: "&aГотово!"
});

config.get("threshold", 10);  // с fallback
config.set("threshold", 25);  // сразу сохраняет
config.has("notify");
config.remove("message");
config.getAll();
```

**GUI для игроков:** все ключи config.json видны в `/sh` → Скриптинг →
**«Менеджер модулей»** (Boolean → свитч, строка/число → поле ввода,
сложные объекты → подсказка «правьте в файле»). Изменения применяются
к загруженному модулю сразу.

**Схема виджетов** — второй аргумент `define` (или `config.defineSchema({...})`)
превращает настройки в нативные виджеты MoulConfig:

```javascript
config.define({
    threshold: 10,
    mode: "fast",
    accent: "0:85:255:85:255",   // формат цвета MoulConfig: "alpha:r:g:b:a"
    enabled: true
}, {
    threshold: { type: "slider",  min: 0, max: 100, step: 1, description: "Порог" },
    mode:      { type: "dropdown", options: ["fast", "fancy", "off"] },
    accent:    { type: "color" },
    enabled:   { type: "switch" }
});

config.openMenu();   // открыть меню настроек модуля (можно из своей команды)
```

Типы: `switch`, `slider {min,max,step}`, `dropdown {options}`, `color`,
`text` (по умолчанию). Схема сохраняется в config.json (`__schema`) и работает
даже для выключенного модуля.

### 14. `utils` и таймеры
```javascript
// Таймеры (автоматически очищаются при выгрузке модуля)
setTimeout(function() {
    chat.display("Прошло 3 секунды");
}, 3000);

var intervalId = setInterval(function() {
    // выполняется каждые 5 секунд
}, 5000);
clearInterval(intervalId);

// Утилиты
utils.playSound("entity.player.levelup", 1.0, 1.0);
utils.copyToClipboard("Текст");
utils.getClipboard();
utils.getFps();
utils.formatNumber(1234567.8); // "1,234,567.8"

    // Настройки графики (для модулей-оптимизаторов)
    utils.getRenderDistance(); utils.setRenderDistance(8);   // чанки (2-32)
    utils.getCloudsMode(); utils.setCloudsMode("off");       // "off" | "fast" | "fancy"
    utils.getCloudRange(); utils.setCloudRange(32);          // радиус облаков (2-128)
    utils.getParticleStatus(); utils.setParticleStatus("minimal"); // "all" | "decreased" | "minimal"
    utils.isEntityShadows(); utils.setEntityShadows(false);

    // Криптография (для OAuth PKCE, подписей API)
    utils.sha256Hex("текст");          // hex-хеш SHA-256
    utils.sha256Base64Url("текст");    // base64url SHA-256 (code_challenge для OAuth)
    utils.randomString(64);            // случайная строка A-Za-z0-9-_
    ```

### 14.1. `keybind` — бинды клавиш модуля
В отличие от `events.onKey`, колбэк вызывается только при **нажатии** конкретной клавиши (без повторов и отпускания). Бинды автоматически снимаются при выгрузке модуля.

```javascript
keybind.register("R", function() {
    chat.display("Нажата R");
});
keybind.register("LEFT_SHIFT", function() { ... });  // также CTRL, ALT, F1-F12, KP_0...
keybind.unregister("R");
```

### 14.2. `net` — HTTP-запросы с заголовками, POST, JSON
Расширение `file.getUrlContent`: коды ответа, заголовки, POST, JSON-парсинг. Ответы ограничены 8 МБ.

```javascript
// GET с заголовками
var r = net.get("https://api.example.com/data", { "Authorization": "Bearer TOKEN" });
if (r.isOk()) var data = JSON.parse(r.getBody());   // r.getStatus(), r.getHeader("...")

// готовый JSON-парсинг
var data = net.getJson("https://api.example.com/data", headers);

// POST (x-www-form-urlencoded) — net.encodeForm кодирует JS-объект
var resp = net.post("https://accounts.spotify.com/api/token", net.encodeForm({
    grant_type: "refresh_token", refresh_token: token, client_id: id
}));

// POST JSON + парсинг ответа
var out = net.postJson("https://api.example.com/api", { hello: "world" });

// произвольный метод
net.request("PUT", url, body, "application/json", headers);

// асинхронно (не блокирует поток вызова)
net.getAsync("https://example.com", function(resp) { ... });
```

⚠️ Синхронные методы **блокируют** поток вызова — из tick/chat-триггеров используйте `net.getAsync` или обёртку `setTimeout`.

**Локальный HTTP-сервер для OAuth-колбэков:**
```javascript
// Слушает только 127.0.0.1. На каждый запрос — колбэк (path, query, headers),
// верните строку-ответ браузеру. Живёт до выгрузки модуля.
net.listen(9876, function(path, query, headers) {
    if (query.code) { /* OAuth-авторизация завершена */ }
    return "<h2>Можно закрыть вкладку</h2>";
});
net.stopListen(); // остановить вручную
```

### 14.3. `sound` — звуки
```javascript
sound.play("minecraft:entity.experience_orb.pickup");            // у игрока
sound.play("block.note_block.pling", 0.5, 2.0);                  // volume, pitch
sound.playAt("entity.player.levelup", 100, 64, 200, 3.0, 1.0, "neutral"); // в точке мира
sound.getCategories();   // ["master", "music", "players", ...]
```

**Кастомные ogg-файлы модуля** (аналог Sound из ctjs):
```javascript
var h = sound.playFile("notify.ogg", { volume: 0.6, pitch: 1.2 });
// опции: volume (0..4), pitch (0.5..2), loop (true — зациклить), loopDelay (в тиках),
//        x, y, z (позиция в мире), category ("master"/"music"/"players"/...)
sound.playFile("ambient.ogg", { loop: true, category: "music" });
sound.stopFile(h);     // остановить по хэндлу
sound.stopAll();       // остановить все звуки модуля
```
Файлы ищутся в папке модуля `config/starredheltix/modules/<модуль>/` (песочница,
выход через `..` запрещён). Поддерживаются только `.ogg`. Все кастомные звуки
останавливаются и разрегистрируются при выгрузке модуля.

### 14.4. `particles` — частицы в мире
Простые частицы (flame, heart, end_rod, crit, portal и т.д.). До 500 за вызов.

```javascript
particles.spawn("minecraft:heart", 100, 65, 200);                  // одна частица
particles.spawn("minecraft:flame", 100, 65, 200, 0, 0.1, 0, 5);    // 5 штук с разбросом
particles.sphere("minecraft:end_rod", 100, 65, 200, 2.0, 20);      // сфера радиуса 2
particles.line("minecraft:crit", 0,64,0, 10,70,10, 15);            // линия из 15 частиц
particles.cylinder("minecraft:portal", 0, 64, 0, 3, 2, 24);        // цилиндр (зоны)
```

### 14.5. `item` — подробности о предметах
Дополняет `inventory.*`: урон, чары с id, редкость.

```javascript
var it = item.getHeld();          // предмет в главной руке (или null)
item.getOffHand(); item.getSlot(4);  // вторая рука / слот 0-35

if (it) {
    it.getId();                   // "minecraft:diamond_sword"
    it.getName(); it.getCount();
    it.getDamage(); it.getMaxDamage();   // прочность
    it.getRarity();               // "common" | "uncommon" | "rare" | "epic"
    it.getEnchantments();         // [{id: "minecraft:sharpness", level: 5}, ...]
    it.getEnchantmentLevel("minecraft:sharpness");
    it.getLore(); it.getFullTooltip();
    it.hasCustomName(); it.isEnchanted(); it.isDamageable();
}
```

### 15. Модульность и `require`
Внутри папки своего модуля можно разбивать код на несколько файлов:
```javascript
// utils.js
module.exports = {
    greet: function(name) { return "Привет, " + name + "!"; }
};

// index.js
var utils = require("./utils");
chat.display(utils.greet(player.getName()));
```

### 16. `npc` — Клиентские NPC-игроки (уникально для StarredHeltix)

Создавайте собственных визуальных NPC-игроков — только на вашей стороне,
сервер о них не знает. NPC поддерживают скины, клики мышью, диалоги, морф
в любых мобов и авто-взгляд на игрока (см. раздел 23).

```javascript
// Создание NPC (все параметры опциональны, кроме координат)
var bear = npc.create({
    name: "Мишка",                 // отображаемое имя (можно с §-кодами)
    x: 10.5, y: 72.0, z: -5.5,     // позиция
    yaw: 0, pitch: 0,              // поворот
    showName: true,                // показывать ник над головой
    skin: "lic:Notch",             // источник скина (см. ниже)
    onClick: function(npcName) {   // вызывается по ЛКМ по NPC
        bear.say("Не тычь меня!");
    }
});

// Управление
bear.setPos(20, 72, -10);          // телепорт
bear.move(0, 0, 1);                // сдвиг на дельту
bear.lookAtPlayer();               // повернуться к игроку
bear.setYaw(90); bear.setPitch(0);
bear.setName("Михаил");
bear.showName(false);
bear.getPos();                     // { x, y, z }
bear.onClick(function(name) {});   // заменить обработчик клика
bear.say("Сообщение");             // диалог (оверлей NPC-диалогов или чат)
bear.remove();                     // удалить

npc.removeAll();                   // удалить все NPC этого модуля
```

**Источники скина (`skin`):**
| Значение | Откуда скин |
|---|---|
| `"steve"` / `"alex"` / `"default"` | Стандартные скины Minecraft |
| `"starredheltix:npc/mishka.png"` или `"assets:<namespace>:<путь>"` | Текстура из ресурсов (assets мода) |
| `"lic:Ник"` или просто `"Ник"` | UUID и скин игрока с лицензионных серверов Mojang |
| `"069a79f4-44e9-4726-a5be-fca90e38aaf5"` (UUID-строка) | Скин по профилю (UUID) |
| `"url:https://example.com/skin.png"` | Прямая ссылка на PNG-скин |

Скины по нику/UUID/URL загружаются асинхронно (до готовности NPC показан
со стандартным скином). При выгрузке/перезагрузке модуля его NPC удаляются автоматически.

### 17. `render` — Персистентные батчи мировых примитивов (тяжёлые рендер-системы)

Главный инструмент для тяжёлых систем (waypoints, ESP, зоны, маршруты):
наполни батч **один раз** при изменении данных — ядро рисует его каждый кадр
нативно, **без запуска JS в hot-path**. Это даёт батчинг уровня ядра.

```javascript
var batch = render.batch("my-waypoints");
batch.addBox(100, 64, -200, 1, 1, 1, 0x80FF0000, true, true);  // filled, через блоки
batch.addLine(0, 70, 0, 100, 70, -200, 0xFFFFFF, 2);
batch.addText("Метка", 100, 66, -200, 1, 0xFFFFFF);
batch.addBeacon(100, 64, -200, 256, 0xFF0000);
batch.addImage("starredheltix:textures/image.png", 50, 70, 50, 2, 2);

batch.getSize();            // сколько примитивов
batch.clear();              // очистить и наполнить заново при изменении данных
render.removeBatch("my-waypoints");
render.clearBatches();      // удалить все батчи модуля
```

Батчи автоматически удаляются при выгрузке модуля. На один батч действует лимит 20 000 примитивов (превышение — предупреждение в логе, новые примитивы игнорируются до `clear()`).

**Динамические изображения по URL** (обложки, иконки, карты):
```javascript
render.loadImage("https://i.scdn.co/image/abc123", "cover");   // PNG/JPEG до 8 МБ, асинхронно
// после загрузки рисуется обычным drawTexture:
// (в hud.register / onOverlay / gui.openScreen)
ctx.drawTexture("starredheltix:module/<имя-модуля>/cover", 0, 0, 64, 64);

render.loadImage(url, "cover"); // повторный вызов с тем же id ЗАМЕНЯЕТ текстуру
render.removeImage("cover");    // удалить одну
render.clearImages();           // удалить все (автоматически при выгрузке модуля)
```

**Фигуры и локальные изображения:**
```javascript
// Окружность в мире линиями (axis: "y" — горизонтальная, "x" — плоскость YZ, "z" — плоскость XY)
batch.addCircle(100, 64, -200, 5, 0xFF00FF00, 2, 32, "y");

// Замкнутый полигон по периметру — координаты тройками x,y,z
batch.addPolygon(100,64,200,  104,64,200,  104,64,204,  100,64,204);

// Изображение из файла модуля (песочница), аналог Image.fromFile из ctjs
render.loadImageFromFile("icons/coin.png", "coin");
// после загрузки: ctx.drawTexture("starredheltix:module/<модуль>/coin", 0, 0, 16, 16);
```
В 2D-оверлее также доступна заливка круга: `ctx.drawCircle(cx, cy, r, color, outline)`.


### 17.5. Кастомные шрифты (TTF/OTF) — `SHModuleFonts`

```javascript
// один раз при загрузке модуля (TTF/OTF из папки модуля, ASCII + кириллица)
render.loadFontFromFile("fonts/myfont.ttf", "myfont", 48);

// в overlay / hud / gui:
ctx.drawFontText("myfont", "Привет, мир!", 10, 10, 24, 0xFFFFFF, true);
var m = ctx.measureFontText("myfont", "Привет, мир!", 24);   // m.width / m.height

// статичная надпись одной текстурой (лучше для неизменяемого HUD):
var wh = render.textToTexture("Заголовок", "title", "myfont", 28, 0xFF7DF9FF, true);
// рисование: ctx.drawTexture("starredheltix:module/<модуль>/text/title", x, y, wh.width, wh.height);

render.removeFont("myfont");   // авто-очистка при выгрузке модуля
```
Шрифт печётся в атлас (запечённый кегль 48 → до ~2x масштаба без мыла). Динамические числа — через
`drawFontText` (блит по глифам), статичные надписи — через `textToTexture` (ноль JS в hot-path).

### 18. 3D-контекст: LOD и каллинг

Внутри `events.onWorldRender(function(ctx) { ... })` доступны:
```javascript
ctx.cameraX; ctx.cameraY; ctx.cameraZ;          // позиция камеры
ctx.isVisible(x, y, z, w, h, d);                // фрустум-тест (не рисуй невидимое!)
ctx.distanceSq(x, y, z);                        // дистанция² до камеры (для LOD)
ctx.drawImage("starredheltix:textures/pic.png", x, y, z, 2.0, 2.0, 45.0); // текстура в мире
ctx.drawAnimatedImage(tex, x, y, z, w, h, rotY, cols, rows, fps);         // анимация спрайт-листом
```

### 19. `gui` — полноценные GUI-экраны из модулей

```javascript
gui.openScreen({
    title: "Моё меню",
    pausesGame: false,
    render: function(ctx, mouseX, mouseY) {
        // ctx — тот же 2D-контекст, что и в overlay (см. раздел 20)
        ctx.drawRect(50, 50, 100, 20, 0x80000000);
        ctx.drawString("Привет!", 60, 56, 0xFFFFFF);
    },
    onClick: function(x, y, button) { ...; return true; }, // true = поглотить клик
    onKey: function(keyCode, scanCode) { ...; return true; },
    onChar: function(char) { ...; return true; },          // ввод текста
    onScroll: function(x, y, delta) { ...; return true; },
    onClose: function() { ... }
});
gui.isScreenOpen();
gui.closeScreen();
```
Экран автоматически закрывается при выгрузке модуля.

### 19.1 `camera` — кастомизация камеры

Глобальный объект (камера одна на клиент). Пока камера отцеплена (`enable`),
её позиция и/или поворот подменяются; поворот мышью работает, пока не задан
свой через `setRot`. Движение игрока (WASD/Space/Shift) подавляется, пока
камера отцеплена — для управления камерой опрашивайте клавиши через
`keybind.isDown(...)` (GLFW, напрямую). При выгрузке модуля, отцепившего
камеру, и при дисконнекте камера автоматически возвращается к игроку.

```javascript
camera.enable();               // отцепить камеру с текущей позиции (false — нет мира)
camera.disable();              // вернуть камеру к игроку
camera.toggle();               // переключить
camera.isEnabled();            // отцеплена ли

camera.getPos();               // {x, y, z} — позиция отцепленной камеры
camera.setPos(x, y, z);        // переместить камеру

camera.setRot(yaw, pitch);     // свой поворот (градусы) — отключает обзор мышью
camera.clearRot();             // вернуть поворот от игрока
camera.isRotOverridden();
camera.getRot();               // {yaw, pitch} — эффективный поворот камеры
camera.getPlayerRot();         // {yaw, pitch} — поворот игрока
camera.getPlayerPos();         // {x, y, z} — позиция глаз игрока
```

Пример (фрикаф): `camera.enable()` + в `events.onWorldRender` двигать камеру
через `camera.setPos` по `keybind.isDown("W"/"A"/"S"/"D"/"SPACE"/"LEFT_SHIFT")`
с вектором из `camera.getPlayerRot()`.

### 20. 2D-контекст: текстуры, градиенты, scissor, трансформы, предметы

В `events.onOverlay`, `gui.openScreen.render` и `hud.register` доступно:
```javascript
ctx.drawTexture("starredheltix:textures/image.png", 10, 10, 32, 32);  // PNG из assets
ctx.drawGradientRect(0, 0, 100, 20, 0xFF0000FF, 0xFFFF0000);          // градиент
ctx.pushClip(0, 0, 100, 100); ctx.popClip();                          // scissor-отсечение
ctx.pushTransform(); ctx.translate(50, 50); ctx.scale(2, 2); ctx.popTransform();
ctx.drawItem("minecraft:diamond", 10, 10);          // предмет
ctx.drawItemWithCount("minecraft:diamond", 30, 10, 64);
```


### 21. Действия локального игрока (интеракции)

В объекте `player` доступны реальные игровые действия (пакеты уходят на сервер):

```javascript
// Атаки и использование
player.swingHand();          // махнуть главной рукой
player.swingOffHand();       // махнуть второй рукой
player.attack();             // ударить сущность под прицелом (true = выполнено)
player.useItem();            // эмуляция ПКМ (по блоку или в воздухе)
player.interactEntity();     // ПКМ по мобу/жителю под прицелом
player.getAttackCooldown();  // 1.0 = удар готов

// Дроп и респавн
player.dropHeldItem(false);  // выбросить предмет из руки (true = весь стак)
player.respawn();            // возродиться после смерти

// Чат и команды (как клиент)
player.sendCommand("spawn"); // = /spawn
player.sendChat("Привет");

// Прицел
player.lookAt(100, 70, -200); // повернуть голову к точке

// Позиция/состояние (дополнительно)
player.isOnGround();          // стоит на земле
player.getGameMode();         // текущий режим игры (строкой)
player.getBiome();            // биом ("minecraft:plains")
player.getLightLevel();       // освещённость 0-15
player.facing();              // "юг" / "запад" / "север" / "восток"
player.getXPLevel(); player.getXPProgress();
player.getArmorPoints();
```

Сущности под прицелом: `world.getLookedAtEntity(maxDistance)` возвращает `JSEntity`
(name/uuid/type/pos/health/displayName). Блок под прицелом — `world.getLookedAtBlock(maxDistance)`.

### 22. Инвентарь, хотбар, тултипы

```javascript
inventory.getItems();          // все предметы (0-8 хотбар, 9-35 инвентарь) → JSItem
inventory.getItemInSlot(4);
inventory.getHeldItem();       // JSItem в главной руке
inventory.getOffHandItem();
inventory.getArmorItem("head");
inventory.getSelectedHotbarSlot();
inventory.setSelectedHotbarSlot(3);
inventory.findSlotByName("Гиперион");

// Открытый контейнер (сундуки, Skyblock GUI)
inventory.isInContainer();
inventory.getContainerItems();
inventory.getContainerItemInSlot(11);
inventory.getContainerTitle();
inventory.clickContainerSlot(11, "left");  // "left"|"right"|"shift"|"middle"
inventory.closeContainer();

// JSItem — методы предмета
item.getName(); item.getID(); item.getCount(); item.getMaxStackSize();
item.getLore();       // лор
item.getTooltip();    // полный тултип
item.getNBT();        // custom_data, custom_name, glint
```

### 23. NPC: морф и авто-взгляд

```javascript
var mob = npc.create({
    name: "Мега-зомби",
    x: 10, y: 72, z: -5,
    morph: "minecraft:zombie",   // NPC сразу в облике зомби (или любого моба)
    lookAtPlayer: true,          // всегда смотрит на тебя (плавно)
    skin: "steve"
});

mob.morph("minecraft:pig");      // сменить облик на лету
mob.morph(null);                 // вернуть облик игрока (скин сохранён)
mob.getMorph();                  // текущий морф
mob.setLookAtPlayer(false);      // отключить авто-взгляд
mob.setLookSpeed(0.3);           // скорость поворота головы (0.01-1.0)
```

Морф — визуальный (клиентский): игрок-двойник скрывается, на его месте
рендерится выбранная сущность с именем NPC. Скин и имя сохраняются при смене.
---

### 24. Java из модулей: `Java.type`, fabric-api, JOML, MoulConfig

Модули — это JavaScript, но Rhino даёт **живой мост в Java**: любой Java-объект,
попавший в JS, можно вызывать как обычный объект (методы, поля, конструкторы).
Глобальный `Java.type("полное.имя.Класса")` загружает класс — если он разрешён
песочницей.

**Что разрешено:** `net.fabricmc.fabric.api.*`, `net.fabricmc.api.*`, `org.joml.*`,
`io.github.notenoughupdates.moulconfig.*`, `java.*`/`kotlin.*` (кроме заблокированных
reflect/Runtime/Process/Thread/io/net). **`net.minecraft.*` заблокирован намеренно.**

```javascript
// JOML — векторная математика
var Vector3f = Java.type("org.joml.Vector3f");
var Matrix4f = Java.type("org.joml.Matrix4f");
var v = new Vector3f(1, 2, 3).add(1, 0, 0);
var m = new Matrix4f().translate(1, 0, 0).rotateY(0.5);

// Fabric API — события с функциональными интерфейсами (SAM):
// JS-функция автоматически превращается в реализацию интерфейса
var TickEvents = Java.type("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents");
TickEvents.END_CLIENT_TICK.register(function(minecraft) {
    // Осторожно: колбэк выполняется вне движка (без антифриза и отписки при
    // выгрузке модуля). Для игровых событий используйте events.* — они безопасны.
});

// Статические методы и поля
var Mth = Java.type("net.minecraft.util.Mth");   // заблокирован — для примера
var Helper = Java.type("org.joml.Math");
var rad = Helper.toRadians(90);
```

**Ограничения моста (важно понимать):**

| Возможность | Доступно | Почему |
|---|---|---|
| Вызывать методы разрешённых Java-объектов | ✅ | живой мост Rhino |
| Перегрузки методов | ✅ (в основном) | Rhino выбирает по аргументам; при неоднозначности — разные классы |
| JS-функция → SAM-интерфейс (lambda в Java) | ✅ | авторазансформация |
| Унаследовать Java-класс/интерфейс в JS | ⚠️ через `JavaAdapter`, но не для final и не для MC-классов (заблокированы) | |
| `net.minecraft.*` напрямую | ❌ | песочница/античит; весь MC-доступ — через `player`/`world`/`client`/... |
| **Миксины из модуля** | ❌ **никогда** | см. ниже |

**Почему миксины для JS-модулей невозможны и не нужны:** миксин — это
трансформация байткода целевого класса, выполняемая Mixin-процессором при
загрузке мода (класс должен быть в mixin-конфиге jar'а). JS-модуль загружается
в рантайме в песочницу и не участвует в загрузке классов — вплести байткод он
физически не может, а перезагрузка уже загруженных классов (модуль = hot reload)
невозможна без перезапуска JVM.

Вместо миксинов — архитектура «мод как мост»:
1. **fabric-api события** — ваниль уже отдаёт десятки хуков (tick, entity, networking, rendering) — доступны напрямую из JS (см. выше).
2. **События движка** `events.*` — хуки, которые мод уже собрал своими миксинами (chat, packets, damage, screen, blockBreak, ...).
3. **Нужен новый хук?** — добавляется в мод: один Kotlin-миксин + `SHScriptEngine.fireTrigger("hookName", ...)` + `events.onHookName(...)` в API. Логика при этом живёт в модуле.
4. **Конфиг модуля** — `config.define(...)` (MoulConfig под капотом); прямой импорт moulconfig из JS имеет смысл только для нестандартных GUI — лучше попросить такой билдер в моде.

Правило: **миксины и net.minecraft — территория мода; JS — логика и визуал.**
---

### 25. `textComponent` и глобальный `Scoreboard`

**`textComponent`** — конструктор настоящих MC-компонентов (клик-действия, ховер-подсказки, точные цвета):
```javascript
var tc = textComponent.of("§eНаведи на меня");           // также: literal(text), translatable(key[, fallback]), empty()
tc.color(0x55FF55)                 // точный RGB (0xRRGGBB)
  .formatting("&l")                // §-коды или имена ("gold")
  .bold().italic().underlined().strikethrough().obfuscated()
  .resetStyle()
  .onClick("run_command", "/sh")   // run_command | suggest_command | open_url | copy_to_clipboard
  .onHover("Подсказка")            // строка или другой компонент
  .append(" — продолжение")        // строка или компонент
  .getString();
chat.displayComponent(tc);         // вывод в чат (chat.display принимает только строки)
```
Методы возвращают `this` — всё цепляется в одну цепочку.

**`Scoreboard` / `Team` / `Score`** — полный доступ к ванильному сайдбару (дополняет `world.getScoreboard*`/`setScoreboard*`):
```javascript
Scoreboard.getTitle(); Scoreboard.setTitle("§bМой скорборд");
Scoreboard.getLines();                       // строки по порядку
Scoreboard.getLineByIndex(0);
Scoreboard.setLine(15, "§7FPS: §f60");       // по score (15 = верх); override=true — заменить любой
Scoreboard.addLine(3, "новая строка");
Scoreboard.removeScores(3); Scoreboard.removeIndex(0);

Scoreboard.getScores();                      // [Score]: getName/getDisplayName/getScore/setScore/setName/getTeam/setTeam/remove/isHidden
Scoreboard.getScoreByName("§r§7");

// Кастомные СТРОКИ МОДУЛЯ — рисуются движком под/над сайдбаром:
Scoreboard.addCustomLine("my_top", "§aМодуль: OK", "BOTTOM", 10); // anchor: "BOTTOM"(default)|"AFTER_XP", order — сортирует
Scoreboard.setCustomLine("my_top", "§aМодуль: " + new Date().getSeconds());
Scoreboard.removeCustomLine("my_top");
Scoreboard.getCustomLines(); Scoreboard.clearCustomLines();        // авто-очистка при выгрузке модуля

// Команды (префиксы/суффиксы ник-тегов):
var team = Scoreboard.createTeam("sh_markers");
team.setPrefix("§c• "); team.setSuffix(" §7[моя банда]");
team.addMember("Steve"); team.getMembers();
team.setNameTagVisibility("HIDE_FOR_OTHER_TEAMS");   // enum-имя Team.Visibility, верхний регистр
team.setCollisionRule("NEVER");                        // Team.CollisionRule, верхний регистр
team.setFriendlyFire(false);
Scoreboard.removeTeam("sh_markers");
```

### 26. `Maps` / `DungeonMap` — динамические 2D-картины

Пиксельные текстуры, живущие на GPU: рисуешь попиксельно в JS, обновишь — получишь `drawTexture`-id
(стены, мини-карты, пиксель-арт, «телевизоры»). id текстур — `starredheltix:module/<модуль>/map_<id>`.
```javascript
var m = Maps.create("wall", 128, 128);   // по умолчанию 128×128
m.setPixel(10, 20, 0xFFFF0000);           // ARGB double
m.fill(0x80000000);                       // залить (прозрачный = "стереть")
m.clear();
m.getPixel(10, 20);
m.update();                               // залить пиксели в GPU-текстуру (делать после правок)
m.getTextureId();                         // "starredheltix:module/<модуль>/map_wall" → ctx.drawTexture(...)
Maps.get("wall"); Maps.remove("wall"); Maps.list();
// в onOverlay/hud/gui: ctx.drawTexture(m.getTextureId(), x, y, 128, 128);
```
**`DungeonMap`** — данные с карты подземелья (предмет Map в слоте).
```javascript
DungeonMap.isInDungeon();  DungeonMap.hasMap();  DungeonMap.getMapId();
DungeonMap.getCrypts();                            // целые ли склепы (null — нет карты)
DungeonMap.getMapCenter();                         // {x, z} центра игрока на карте (ось Z, не Y!)
DungeonMap.getMapScale();                          // 1 = 1 блок/пиксель... (vanilla scale)
DungeonMap.getDecorations();                       // [{x, y, rot, name, hasIcon}] — маркеры карты
DungeonMap.getMapColors();                         // int[] пикселей текущей карты
Maps.dungeon();                                    // то же самое через Maps
```
Карта сама перерисовывается сервером; доступ к пикселям — только чтение.

### 27. `Renderer`, `Display`, `Text`, `Image` — совместимость с ctjs

Для портов ChatTriggers-модулей добавлены одноимённые глобальные классы (тот же стиль вызовов):
```javascript
// Renderer — порт ctjs RenderLib/UI-рендерера:
Renderer.color("GREEN");                    // ИМЯ ChatFormatting (верхний регистр) → ARGB Long; "&a" НЕ подходит
Renderer.getColor(255, 128, 0, 255);        // RGBA → ARGB Long
Renderer.getRainbow(step, 1.0);             // переливающийся цвет
Renderer.getStringWidth("текст");           // ширина в px
Renderer.drawRect(color, x, y, w, h);       // в 2D-контексте (overlay/hud/gui)
Renderer.drawLine(color, x1, y1, x2, y2, 2);
Renderer.drawCircle(color, x, y, 10, 24);
Renderer.drawString("текст", x, y);         // тень по умолчанию
Renderer.drawImage(img, x, y, w, h);
Renderer.drawBox(color, x, y, z, w, h, d, false);  // 3D-коробка (в worldRender)
Renderer.pushMatrix(); Renderer.translate(10, 20); Renderer.scale(2, 2); Renderer.rotate(45, 0, 0, 1); Renderer.popMatrix();
// Готовые цвета: Renderer.BLACK, DARK_BLUE, GREEN, AQUA, RED, PURPLE, GOLD, GRAY,
//                 DARK_GRAY, BLUE, LIGHT_PURPLE, YELLOW, WHITE

// Display/Text/Image — «живые» виджеты: создал один раз, ядро рисует каждый кадр БЕЗ JS
// background: "NONE" | "FULL" | "PER_LINE";  order: "NORMAL" | "REVERSED"
var d = new Display({ x: 100, y: 100, background: "PER_LINE", align: "CENTER" });
d.addLine(new Text("Заголовок", { color: 0xFF55FFFF, shadow: true, scale: 1.5 }));
d.addLine(new Text("Обычная строка", 0, 0));
d.setX(120); d.setY(140); d.setTextColor(0xFFFFFFFF); d.setOrder("NORMAL");
d.setLines([new Text("заменить всё")]); d.getLine(0);
d.removeLine(0); d.clearLines();
d.shouldRender(); d.setShouldRender(false);   // глобальный «вкл/выкл» для всех Display
d.getMinWidth(); d.setMinWidth(200);
d.getBackgroundColor(); d.setBackgroundColor(0x80000000);
d.remove();                                    // снять с экрана; destroy() — то же
// + авто-удаление при выгрузке модуля

var img = Image.fromUrl("https://example.com/pic.png");       // асинхронно
img = Image.fromFile("icons/coin.png");                        // из папки модуля (песочница)
img = Image.fromAsset("minecraft:textures/item/diamond.png");
img.getTextureWidth(); img.getTextureHeight();   // размер ТЕКСТУРЫ (не экранный)
img.getTexture();                                // AbstractTexture (null, если не загружена)
img.draw(x, y, w, h);                            // растянуть (или Renderer.drawImage)
img.destroy();
// рисунок — через Renderer.drawImage(img, x, y, w, h) или drawTexture по id
```

## 🔄 Соответствие API ctjs (ChatTriggers)

| ctjs | StarredHeltix | Статус |
|---|---|---|
| `ChatLib.chat/display/actionBar/simulate/editChat/deleteChat/getChatLines` | `chat.*` (+edit/delete/getLines) | ✅ |
| `Player.*` | `player.*` (+`getPotionEffects`, ввод, биом, свет, XP) | ✅ |
| `Client.connect/disconnect/getFPS/Server.*` | `client.*` (connect, disconnect, FPS, память, пинг, меню, мышь) | ✅ |
| `Client.sendPacket(packet)` | `client.sendPacket("имя", ...)` — по имени, без MC-объектов (песочница) | ✅ (безопаснее) |
| `Settings.*` (video/sound/chat/skin) | `settings.*` (get/set для всех настроек) | ✅ |
| `MathLib.*` | `math.*` (+dist3D, smoothstep) | ✅ |
| `CPS.*` | `cps.*` (клики в секунду) | ✅ |
| `World`, `Entity`, `PlayerMP` | `world.*`, `JSEntity` | ✅ |
| `Inventory`, `Item`, `NBT` | `inventory.*`, `JSItem.getNBT()` | ✅ (клики по слотам через `clickContainerSlot`) |
| `BossBars` | `bossbars.*` | ✅ |
| `FileLib` | `file.*` (+sandbox в папке модуля) | ✅ (без unzip/open) |
| `HttpRequest`/fetch с заголовками | `net.get/post/postJson/getAsync` (+8МБ лимит) | ✅ (только у нас) |
| `KeyBind` (регистрация) | `keybind.register` | ✅ |
| `Sound` (кастомные ogg) | `sound.play/playAt/playFile/stopFile` | ✅ (ogg из папки модуля, loop) |
| `Particle` | `particles.spawn/sphere/line/cylinder` | ✅ |
| `Item.getEnchantments/Rarity` | `item.getHeld().getEnchantments()` | ✅ (только у нас) |
| `Image` по URL | `render.loadImage(url, id)` | ✅ (только у нас) |
| `register("step"/etc.)`-таймеры | `setInterval` (module-bound) | ✅ |
| `Gui`/Screen из JS | `gui.openScreen({render, onClick, onKey...})` | ✅ (только у нас) |
| `cancel(event)` в чате | `events.onChatCancellable/onChatOutgoingCancellable` | ✅ |
| межмодульный event bus | `events.on/once/emit` | ✅ (только у нас) |
| `Renderer`/`Display`/`Text`/`Image` 2D/3D | `Renderer`, `new Display()`, `new Text()`, `Image.fromUrl/fromFile` (§27) + собственные `hud.register`, `events.onOverlay`, `render.batch` (персистентный батчинг) | ✅ (порт ctjs + батчи быстрее CT) |
| `Scoreboard.setTitle/setLine/addLine/removeScores` | глобальный `Scoreboard` (+`Team`/`Score`) и `world.setScoreboard*` | ✅ (+кастомные строки модуля) |
| `Config` с GUI | `config.define/get/set` + MoulConfig «Менеджер модулей» | ✅ |
| `Commands.*` (Brigadier DSL) | `command.register` (+схема типов, choice-подсказки, greedy) | ✅ (таб-комплит) |
| `Trigger.dequeue`/приоритеты | `SHPriority` внутри движка | ✅ |
| локальный OAuth-сервер | `net.listen(port, fn)` | ✅ (только у нас) |
| NPC-игроки (DisplayLib/PlayerNPC) | `npc.create` (клиентские сущности со скинами, морф, интеракция) | ✅ (только у нас) |
| `ModuleUpdater` | `/sh modules update`, `/sh modules checkupdates` | ✅ |
| `ModulesGui` | `/sh` → «Менеджер модулей» (MoulConfig) | ✅ |
| модули-библиотеки + автодокачка зависимостей | `"library": true` + `require("модуль")` | ✅ (только у нас) |
| вкл/выкл модулей командой | `/sh modules on/off` | ✅ (только у нас) |
| профайлер триггеров | — | ❌ (не реализовано; меряйте вручную через `Date.now()`) |
| антифриз (защита от while(true)) | InstructionObserver, лимит 3с | ✅ (только у нас) |
| typing-generator (TS) | — | ❌ |
| Динамические миксины из JS | — | ❌ (не планируется) |
| 19 встроенных детекторов Skyblock | `detector.*` | ✅ (только у нас) |
| JS-sandbox с blacklist | `SHClassShutter` | ✅ (только у нас) |
---

## Приложение A. Правила, песочница и производительность

### A.1. Песочница (SHClassShutter)
Код модуля исполняется в Rhino-песочнице. **Разрешены** для `Java.type(...)` / `Packages`:
```
set.starlev.scripting.*    java.*    kotlin.*    net.fabricmc.fabric.api.*
org.joml.*                 io.github.notenoughupdates.moulconfig.*
```
**Запрещены** (ошибка доступа): `java.lang.reflect`, `java.lang.Runtime`,
`Process/ProcessBuilder`, `java.lang.System`, `ClassLoader`, `Thread`,
`java.io.*`, `java.net.*`, `java.nio.*`, `sun.*`, все `*.impl.*` Fabric/MoulConfig.

Файловая песочница — каждый модуль видит только свою папку
`config/starredheltix/modules/<модуль>/` (выход через `..` блокируется).

### A.2. Потоки и событийная модель
- **Hot-path** (каждый тик или кадр): `events.onTick`, `events.onWorldRender`,
  `events.onOverlay`, `hud.register`, `gui.openScreen.render`. Здесь нельзя делать
  тяжёлые вычисления, HTTP, парсинг — держите данные в переменных и кэшируйте.
- **Редкие события**: `onChat*`, `onSkillXp`, `onEntityDeath` и др.
- **Фоновые потоки**: колбэки `net.getAsync` и `net.listen` выполняются вне
  рендер-потока. Minecraft API из них вызывать **нельзя** — только через
  `client.execute(function() { ... })`. То же касается глобальных `file.getUrlContent`.
- Таймеры `setTimeout`/`setInterval` выполняются в `Context` Rhino и **очищаются
  автоматически** при выгрузке модуля (живой интервал не может пересоздать
  удалённую папку модуля или утечь память).

### A.3. Антифриз
Непрерывное выполнение JS дольше **3000 мс** (например `while(true)` в tick-триггере)
прерывается ошибкой; модуль снимается. Соблюдайте ограничения:
- не используйте `while(true)` / `for(;;)` без явного выхода;
- не делайте в tick-триггерах `net.get` (синхронный) — только `net.getAsync`;
- рекурсивные алгоритмы с неограниченной глубиной запрещены.

### A.4. Производительность (чек-лист)
1. **World-рендер** — используйте `render.batch(...)`; ядро рисует примитивы нативно,
   без запуска JS каждый кадр.
2. **2D-оверлей** — `events.onOverlay` вызывается каждый кадр; минимизируйте
   создание строк/массивов внутри колбэка.
3. **HUD** — вызывайте `hud.register` один раз при загрузке, а внутри колбэка
   читайте уже готовые значения.
4. **Кэш детекторов** — `detector.*` кэширует парсинг; не пересчитывайте сами то,
   что уже даёт детектор.
5. **Лог** — `console.log` в hot-path замедляет; оставляйте только при отладке.

### A.5. Цвета и форматирование
- Рендер-цвета — ARGB: `0xAARRGGBB` (например `0x80FF0000` — полупрозрачный красный).
- Текстовые цвета — `§x` коды; во многих API `&` автоматически превращается в `§`
  (`chat.*`, `hud`, `bossbars`, `toast`, `npc.say`).
- `chat.edit/delete` принимают JS-RegExp: флаг `g` включает глобальную замену.

---

## Приложение B. Отладка и частые проблемы

| Симптом | Причина | Решение |
|---|---|---|
| Модуль «молчит», нет ошибок | Условие в `events.onConnect` не сработало (уже были в мире) | Добавьте `events.onTick` fallback с флагом |
| Ошибка в колбэке → модуль снимается | Throwable в триггере; лог в `console.log` и чат | Ловите исключения `try/catch`, см. логи модулей |
| `chat.edit`/`delete` не видит строк | Регистр / `§`-коды; удаляйте с учётом цвета | Используйте `chat.getLines()` для отладки совпадений |
| `sound.playFile` не играет | Файл не `.ogg` / не в папке модуля / категория 0 | Проверьте папку, `sound.stopFile(handle)`, `category` |
| Команда со схемой не табулирует | `choice` без `options`; `greedy` не последний | См. warn в логе; схема откатывается к простому режиму |
| `render.batch` не рисует | Батч залимичен (20k) / id повторяется / `removeBatch` не вызван | Проверьте логи лимита; вызывайте `clear()` при изменении данных |
| HUD элемент не двигается | Координаты относительные, размер в `hud.register` | Настройте позицию через MoulConfig или `ctx.x/ctx.y` |
| Проблемы после `/sh modules reload` | Глобальное состояние пережило перезагрузку | Используйте `storage.*`, не модульные переменные для данных |

**Полезное:**
- `config/starredheltix/modules/console.log` — журнал всех модулей.
- Профилирования триггеров по времени в моде сейчас **нет** — ни CLI-команды, ни API. Если нужно мерять самому, оборачивай тело триггера в `Date.now()` в модуле.
- `client.getCurrentScreenName()` — для `onGuiOpen/onGuiClose`.
---

## Приложение C. Полный справочник сигнатур

**Сжатая форма:** полные сигнатуры каждого метода — в `docs/scripting-api-AI.md`
(раздел «Справочник глобалов»). Ниже — консолидированный список по объектам
(сверено с исходниками `src/main/kotlin/set/starlev/scripting/**`).

| Объект | Методы |
|---|---|
| `player` | `exists getName getUUID getX/Y/Z getBlockX/Y/Z getPos getYaw getPitch setRotation getHealth getMaxHealth getAbsorption getFoodLevel getSaturationLevel getAirSupply getMaxAirSupply getExperienceLevel getXPLevel getExperienceProgress getXPProgress isSneaking isSprinting setSprinting isSwimming isGliding isMoving isOnGround getVelocity hasPotionEffect getPotionEffects getHeldItemName getOffHandItemName getHeldItemIndex getGameMode getBiome getLightLevel facing getArmorPoints getInput isPressingAttack isPressingUse isPressingDrop isPressingSwap setPressingAttack setPressingUse setPressingDrop setPressingSwap setMovement stopMovement setPosAndRotation swingHand swingOffHand attack useItem interactEntity dropHeldItem respawn sendCommand sendChat getAttackCooldown lookAt` |
| `chat` | `send command display displayComponent simulate actionBar addColor cleanColor clear getLines getChatWidth edit delete` |
| `client` | `isInWorld isSingleplayer connect disconnect getServerIP getServerName getServerMOTD getPing getPingStats getPlayerPings resetPingStats getFPS getMemoryUsage getMaxMemory getUsedMemory isWindowActive isInMainMenu isInMultiplayerMenu isInConnectScreen isInGui getCurrentScreenName closeScreen getGameVersion getMouseX getMouseY isShiftDown isControlDown isAltDown sendPacket` |
| `camera` | `getYaw getPitch setYaw setPitch setRotation isOverridden isRotOverridden clearRotation reset` |
| `settings` | `getFov setFov getBrightness setBrightness getGuiScale setGuiScale getMaxFrameRate setMaxFrameRate isVsync setVsync isFullscreen setFullscreen getMipmapLevels setMipmapLevels isSmoothLighting setSmoothLighting isBobbing setBobbing getGraphicsMode setGraphicsMode getRenderDistance setRenderDistance getClouds setClouds getParticles setParticles isEntityShadows setEntityShadows getMasterVolume setMasterVolume getMusicVolume setMusicVolume getRecordsVolume setRecordsVolume getWeatherVolume setWeatherVolume getBlocksVolume setBlocksVolume getSoundEffectsVolume setSoundEffectsVolume getHostileVolume setHostileVolume getNeutralVolume setNeutralVolume getPlayersVolume setPlayersVolume getAmbientVolume setAmbientVolume getChatVisibility setChatVisibility isChatColors setChatColors isChatLinks setChatLinks getChatOpacity setChatOpacity getChatScale setChatScale getChatWidth setChatWidth getChatFocusedHeight setChatFocusedHeight getChatUnfocusedHeight setChatUnfocusedHeight isReducedDebugInfo setReducedDebugInfo isSkinPartEnabled setSkinPartEnabled` |
| `math` | `map clamp lerp dist2D dist3D diff min max round smoothstep` |
| `cps` | `getLeftClicks getRightClicks getMiddleClicks getLeftClicksMax getRightClicksMax getMiddleClicksMax getLeftClicksAverage getRightClicksAverage getMiddleClicksAverage` |
| `detector` | `actionBar biome chat container dungeon enchantProgress entityDeath heldItem itemLore location mobHead museum playerItem scoreboard skillXp slotPhase tabList time title` (19 детекторов; **`stats` не существует** — CPS/BPS только через `cps` и `player`) |
| `events` | `onTick onFps onFrame onRender onChat onChatOutgoing onChatCancellable onChatOutgoingCancellable onMessageSent onWorldRender onOverlay onGuiOpen onGuiClose onGuiClick onGuiKey onGuiMouseClick onGuiMouseDrag onScreenOpen onScreenClose onSlotUpdate onSkillXp onKey onConnect onDisconnect onEntityLoad onEntityUnload onEntityDeath onDayStart onNightStart onSoundPlay onSoundPlayWithCriteria onPacketReceive onPacketSend onPickup onDamage onRenderEntity onRenderBlockEntity onDrawBlockHighlight onItemTooltip onDropItem onPlayerInteract onSpawnParticle on once emit` |
| `hud` | `register(id, w, h, renderFn)` → `JSHudElement`: `x y scale setX setY setScale showBackground setShowBackground customWidth customHeight setCustomWidth setCustomHeight isEditing getScaledWidth getScaledHeight isHovered increaseScale decreaseScale` |
| `world` | `getTime getDifficulty getTicksPerSecond getSkyDarken getBorder getSpawn getServerIP getDimension getLoadedChunkCount getScoreboardTitle getScoreboardLines setScoreboardTitle setScoreboardLine addScoreboardLine removeScoreboardLines getActionBar getTabListHeader getTabListFooter setTabListHeader setTabListFooter clearTabListHeader clearTabListFooter getTabListLines getPlayerTabName getTitle getSubtitle getBlock getBlockName getBlockType getBlockAt getBlockEntityType getAllBlockEntities getBlockEntitiesInRadius isLoaded getSkyLightLevel getBlockLightLevel getLookedAtBlock getLookedAtEntity getEntitiesByType getEntitiesInRadius getAllEntities getAllEntitiesOfType getPlayers getNearestPlayerTo getPlayerByName hasPlayer getClosestEntityByName getDistanceToPlayer getCameraPos isRaining getRainingStrength getChunk` |
| `inventory` | `getItems getItemInSlot getItemInHotbar getHeldItem getOffHandItem getArmorItem getSelectedHotbarSlot setSelectedHotbarSlot findSlotByName drag isInContainer getContainerItems getContainerItemInSlot getContainerTitle clickContainerSlot closeContainer` |
| `item` | `getHeld getOffHand getSlot` → `JSItemDetails`: `getId getName setName getCount getDamage getMaxDamage getDurability getRarity getEnchantments getEnchantmentLevel getLore setLore getLoreComponents getFullTooltip hasCustomName isEnchanted isDamageable isEmpty` |
| `bossbars` | `getAll getByName add remove removeByName clearAll` + `JSBossBar`: `getUUID getName setName getPercent setPercent` |
| `toast` | `show(title, message?, durationMs) clearAll` |
| `file` | `write append read exists delete list readJson writeJson getUrlContent encodeBase64 decodeBase64` |
| `storage` | `get set has remove clear load save` |
| `config` | `define get set has remove getAll openMenu defineSchema getSchema load save` |
| `utils` | `playSound copyToClipboard getClipboard getFps getSystemTime formatNumber formatInteger sha256Hex sha256Base64Url randomString getRenderDistance setRenderDistance getCloudsMode setCloudsMode getCloudRange setCloudRange getParticleStatus setParticleStatus isEntityShadows setEntityShadows setTimeout setInterval clearTimeout clearInterval` |
| `keybind` | `register unregister unregisterAll isDown` |
| `sound` | `play playAt getCategories playFile stopFile stopAll` |
| `particles` | `spawn sphere line cylinder` |
| `render` | `batch removeBatch clearBatches loadImage loadImageFromFile removeImage clearImages loadFontFromFile removeFont clearFonts textToTexture` + `JSBatchHandle`: `addBox addLine addText addBeacon addCircle addPolygon addImage getSize clear` |
| `gui` | `openScreen closeScreen isScreenOpen` → `JSGuiScreen`: `open silentClose` |
| `npc` | `create removeAll` + `JSNpcHandle`: `getName setName getPos setPos move getYaw setYaw getPitch setPitch lookAtPlayer setLookAtPlayer isLookingAtPlayer setLookSpeed showName say onClick morph getMorph remove` (**`lookAt(x,y,z)` не существует**) |
| `net` | `get getJson post postJson request getAsync encodeForm listen stopListen` + `Response: getStatus getBody getHeader isOk isNotFound isUnauthorized` |
| `Scoreboard` | `getScores getScoreboardObjective getTitle getDisplayName getScoreboardEntries getPlayers getTeams getTeam createTeam removeTeam setShouldRender*(заглушка) getShouldRender addCustomLine setCustomLine removeCustomLine getCustomLines clearCustomLines getNameFormat` |
| `Team` | `getName getPrefix setPrefix getSuffix setSuffix getMembers addMember removeMember removeAllMembers getFriendlyFire setFriendlyFire getSeeFriendlyInvisibles setSeeFriendlyInvisibles getNameTagVisibility setNameTagVisibility getDeathMessageVisibility setDeathMessageVisibility getCollisionRule setCollisionRule` |
| `Maps` | `create get remove list dungeon` → `JSMapHandle`: `module id width height setPixel fill clear update getPixel getTextureId destroy` |
| `DungeonMap` | `isInDungeon hasMap getMapId getCrypts getMapCenter getMapScale getDecorations getMapColors` |
| `Renderer` | `color getColor getRainbow getStringWidth drawRect drawLine drawCircle drawString drawStringWithShadow drawImage drawBox pushMatrix popMatrix translate scale rotate getFontRenderer` + константы `BLACK DARK_BLUE GREEN AQUA RED PURPLE GOLD GRAY DARK_GRAY BLUE LIGHT_PURPLE YELLOW WHITE` |
| `Display` | `addLine setLines getLine removeLine clearLines setX setY setWidth setHeight setScale setTextColor setBackgroundColor setBackground setAlign setOrder getMinWidth setMinWidth getBackgroundColor shouldRender setShouldRender remove destroy` |
| `Text` | `setText getText setColor getColor setShadow isShadow setScale getScale setBackground getBackground setAlignment setAlignment getMinWidth getMaxWidth` |
| `Image` | `fromUrl fromFile fromAsset getTextureWidth getTextureHeight getTexture draw destroy` |
| `Java` | `type(fullName) → Class?` · `isAllowed(name) → bool` (whitelist; `net.minecraft.*` — нет) |
| `ScriptConsole` | `log info warn error` (алиас — глобальный `console`) |
| `JSUtils` | `getFps getSystemTime formatNumber formatInteger sha256Hex sha256Base64Url randomString copyToClipboard getClipboard` |
| `JSBlock` | `of(x,y,z) of(pos) withType withPos withFace getState getBlockName isEmittingPower getEmittingPower isReceivingPower getReceivingPower` |
| `JSBlockType` | `of(name) fromID(id) getID getRegistryName getTranslationKey getName getLightValue canProvidePower getDefaultState isSolid` |
| `JSBlockFace` | `DOWN UP NORTH SOUTH WEST EAST parse(name) getName getOpposite getOffsetX getOffsetY getOffsetZ` |
| `JSBlockPos` | `x y z down(n) south(n) east(n) west(n) toMC toVec3d` |
| `JSEntity` | `getName getDisplayName getUUID getType getClassName getPos getX getY getZ getHealth getMaxHealth isAlive isDead isLiving getVelocity getMotion getMotionX getMotionY getMotionZ getLastX getLastY getLastZ getRenderX getRenderY getRenderZ getEyePosition getWidth getHeight getEyeHeight getTicksExisted getAir getRiding getRiders isBurning isCollided getDistanceWalked getFireResistance canBeCollidedWith canBePushed isOutsideBorder getMaxInPortalTime isImmuneToFire isWet isSilent hasNoClip getStepHeight getLookVector getDimension getPing distanceTo distanceToSqr distanceToEntity distanceToSqrEntity setGlowing setGlowColor clearGlow getGlowColor` |

> **Возвращаемые хендлы** (`hud.register`, `gui.openScreen`, `render.batch`, `Maps.create`, `npc.create`, `world.getEntities*`, `inventory.getItemInSlot` и т.п.) — объекты с методами; их полный список — в строках `JS*Handle` выше и в разделах с `→`.
> **Контексты рендера:** `worldRender` даёт 3D-контекст (`tickDelta drawBox drawText drawLine drawBeacon drawCircle drawImage drawAnimatedImage cameraX/Y/Z isVisible distanceSq`), `overlay`/`gui` — 2D (`drawBackground drawRect drawText drawImage drawHLine drawVLine rotateAround screenWidth screenHeight drawFontText measureFontText`).
> Подробные сигнатуры каждого метода — в человекочитаемых секциях выше и в `docs/scripting-api-AI.md`.

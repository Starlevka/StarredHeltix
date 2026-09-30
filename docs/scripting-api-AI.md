# StarredHeltix Scripting API — AI/VibeCode Reference

> Машиночитаемый справочник для ИИ-агентов, VibeCode и IDE.
> Все факты сверены с исходниками `src/main/kotlin/set/starlev/scripting/**`.
> Для человеко-ориентированных примеров см. `docs/scripting-api.md`.
>
> **Как пользоваться:** сигнатуры записаны в JS-псевдокоде. `→` — тип возврата.
> `?` — nullable. `*` — vararg. В квадратных скобках — опциональный параметр.

---

## 1. ФОРМАТ МОДУЛЯ (файловая система)

```
.minecraft/config/starredheltix/modules/<имя-модуля>/
├── metadata.json      — метаданные (обязателен для зависимостей)
├── index.js           — entry-скрипт (по умолчанию)
├── data.json          — автохранилище модуля (storage.*)
└── config.json        — конфиг модуля (config.*), __schema внутри
```

### metadata.json — точная схема
```jsonc
{
  "name": "my-module",          // string, ОБЯЗАТЕЛЕН, совпадает с именем папки
  "version": "1.0.0",           // string = "1.0.0"
  "author": "Player",           // string = "Unknown"
  "description": "",            // string = ""
  "entry": "index.js",          // string = "index.js"
  "requires": ["my-lib"],       // string[] = [] — зависимости (модули-библиотеки)
  "library": false,             // bool = false — true = модуль-библиотека
  "minModVersion": "0.1.1",     // string? = null — минимальная версия мода
  "ignored": false              // bool = false
}
```

### Правила модульности
- `library: true` → скрипт НЕ исполняется как модуль; отдаёт API через `module.exports = {...}`; **один экземпляр на всех**.
- `require("имя")` (без `./`) → подгрузка модуля-библиотеки; `require("./file")` → локальный JS-файл.
- Недостающая зависимость автоскачивается с GitHub (`Starlevka/StarredHeltix/modules/`).
- Переменные модуля: `module` (`{name, exports}`) и `exports`.

### Ключевые правила скоупа
- Глобально (все модули): `player chat client settings cps math camera textComponent detector/detectors console ScriptConsole JSUtils Java Scoreboard Team Maps/maps DungeonMap/dungeonMap Renderer Display Text Image JSBlock JSBlockFace JSBlockType JSBlockPos`.
- Per-module scope (каждый модуль получает СВОИ): `events hud storage config command utils inventory world bossbars toast file npc render gui keybind net sound particles item` + `module exports require setTimeout setInterval clearTimeout clearInterval`.

---

## 2. ЖИЗНЕННЫЙ ЦИКЛ И КОМАНДЫ

| Команда | Действие |
|---|---|
| `/sh modules list` | список модулей: `●` вкл, `○` выкл, библиотеки |
| `/sh modules on <имя>` | включить (тянет зависимости) |
| `/sh modules off <имя>` | выключить |
| `/sh modules reload [имя]` | перезагрузка |
| `/sh modules install <имя>` | установка с GitHub |
| `/sh modules remove <имя>` | удалить |
| `/sh modules update [имя]` / `checkupdates` | обновление |
| `/sh modules info <имя>` | детали |

> Профилирования триггеров в моде **нет**: подкоманд `perf`/`perf reset` не существует, класса-профайлера тоже нет. Меряйте вручную через `Date.now()`.

При **выгрузке модуля** автоматически снимаются: триггеры, подписки eventbus/чата, HUD, батчи, текстуры, звуки, таймеры, NPC, команды, локальный HTTP-сервер, открытый GUI-экран.

---

## 3. ДВИЖОК (Rhino 1.9.1)

- ES6 + частично ES2023/24 (named capture groups, lookbehind, `??`, `||=`, Array.groupBy).
- **Антифриз:** любое непрерывное выполнение JS > 3000 мс прерывается `Error`; триггер снимается. Бесконечные циклы запрещены.
- **Потоки:** MC API из колбэков `net.getAsync`/`net.listen` (фоновые потоки) — ТОЛЬКО через `client.execute(...)`.
- **Песочница** (`SHClassShutter`): разрешены `set.starlev.scripting.*`, `java.*`, `kotlin.*`, `net.fabricmc.fabric.api.*`, `org.joml.*`, `io.github.notenoughupdates.moulconfig.*`. ЗАПРЕЩЕНЫ: `java.lang.reflect`, `Runtime`, `Process/ProcessBuilder`, `System`, `ClassLoader`, `Thread`, `java.io.*`, `java.net.*`, `java.nio.*`, `sun.*`, `fabric.impl.*`. `javaPrimitiveWrap = false`.
- Меню настроек модуля: `config.openMenu()` → MoulConfig. Типы `config.defineSchema`: `switch`, `slider{min,max,step}`, `dropdown{options}`, `color("a:r:g:b")`, `text`.
---

## 4. СПРАВОЧНИК ГЛОБАЛОВ

### 4.1 `player` (JSPlayer) — игрок
```
exists()                 → bool
getName()                → string
getUUID()                → string
getX(); getY(); getZ()   → double
getBlockX/Y/Z()          → int
getPos()                 → {x, y, z}
getYaw(); getPitch()     → float
setRotation(yaw, pitch)  → void
getHealth(); getMaxHealth(); getAbsorption() → float
getFoodLevel() → int; getSaturationLevel() → float
getAirSupply(); getMaxAirSupply() → int
getExperienceLevel()/getXPLevel() → int
getExperienceProgress()/getXPProgress() → float (0..1)
isSneaking(); isSprinting(); isSwimming(); isGliding(); isMoving(); isOnGround() → bool
setSprinting(bool)       → void
getVelocity()            → {x, y, z}
hasPotionEffect(name)    → bool
getPotionEffects()       → [{name, localizedName, amplifier, duration, isInfinite, isAmbient}]
getHeldItemName(); getOffHandItemName() → string
getHeldItemIndex()       → int (хотбар 0..8)
getGameMode()            → "survival"|"creative"|"adventure"|"spectator"|""
getBiome()               → "minecraft:plains"
getLightLevel()          → int (0..15)
facing()                 → "юг"|"запад"|"север"|"восток"
getArmorPoints()         → int
getInput()               → {forward, backward, left, right, jump, shift, sprint}
isPressingForward/Backward/Left/Right/Jump/Shift/Sprint() → bool
// действия (пакеты на сервер):
swingHand(); swingOffHand(); attack(); useItem(); interactEntity(); dropHeldItem(all)
respawn(); sendCommand(cmd); sendChat(msg); lookAt(x,y,z); getAttackCooldown()
```

### 4.2 `chat` (JSChat)
```
send(message)                  → void  // отправить на сервер
command(text)                  → void  // "/команда" или без слеша
display(message)               → void  // локально в чат, &→§
simulate(message)              → void  // = display
actionBar(message)             → void  // оверлей над хотбаром
addColor(text) → string; cleanColor(text) → string
clear()                        → void  // очистить чат
getLines()                     → string[] // строки чата, старые→новые
getChatWidth()                 → int      // ширина в px
edit(pattern, ...replacements) → int  // pattern: string(подстрока) ИЛИ RegExp(/g) → число изменённых строк
delete(pattern)                → int  // pattern: string ИЛИ RegExp → число удалённых строк
```
Примечание: `edit`/`delete` мутируют видимую историю чата (allMessages) и перестраивают trimmedMessages. Замена через RegExp — литеральная (без `$1`).

### 4.3 `client` (JSClient)
```
isInWorld() → bool; isSingleplayer() → bool
connect(ip[, port=25565]) → void     // вход на сервер
disconnect() → void                  // в меню
getServerIP(); getServerName(); getServerMOTD() → string
getPing() → long; getPingStats() → {current, avg, min, max, median, last, count}; getPlayerPings() → {name: ms}
resetPingStats() → void
getFPS() → int; getMemoryUsage() → int(МБ); getMaxMemory()/getUsedMemory() → long
isWindowActive() → bool; isInMainMenu()/isInMultiplayerMenu()/isInConnectScreen()/isInGui() → bool
getCurrentScreenName() → string (simpleName класса); closeScreen() → void
getGameVersion() → string; getMouseX()/getMouseY() → double
isShiftDown()/isControlDown()/isAltDown() → bool
sendPacket(name, ...args) → bool  // low-level: "swing"|"useitem"|"playeraction"|"interact"/"attack"
```
### 4.4 `settings` (JSSettings) — клиентские настройки
```
// Общие
getFov/setFov(v) → int/30..110        getBrightness/setBrightness(v) → double/0..1
getGuiScale/setGuiScale(v) → int      getMaxFrameRate/setMaxFrameRate(v) → int/10..260
isVsync/setVsync(b)                   isFullscreen/setFullscreen(b)
// Видео
getMipmapLevels/setMipmapLevels → 0..4  isSmoothLighting/setSmoothLighting(b)
isBobbing/setBobbing(b)               getGraphicsMode/setGraphicsMode("fast"|"fancy"|"fabulous")
getRenderDistance/setRenderDistance(v) → 2..32
// Звук
getMasterVolume/setMasterVolume(v)    getMusicVolume/setMusicVolume(v)
getSoundEffectsVolume/setSoundEffectsVolume(v)
// Чат
getChatVisibility/setChatVisibility("full"|"system"|"hidden")
isChatColors/setChatColors(b)  isChatLinks/setChatLinks(b)
getChatOpacity/setChatOpacity(0..1)   getChatScale/setChatScale(0..1)
getChatWidth/setChatWidth(0..1)       getChatFocusedHeight/getChatUnfocusedHeight (0..1)
isReducedDebugInfo/setReducedDebugInfo(b)
// Скин
isSkinPartEnabled("cape"|"jacket"|"leftSleeve"|"rightSleeve"|"leftPants"|"rightPants"|"hat")
setSkinPartEnabled(part, b)
```
Сеттеры выполняются через `client.execute` (рендер-поток).

### 4.5 `cps` (JSCps)
```
getLeftClicks()/getRightClicks()/getMiddleClicks() → int (за последнюю секунду)
getLeftClicksMax()/getRightClicksMax()/getMiddleClicksMax() → int (пик)
getLeftClicksAverage()/getRightClicksAverage()/getMiddleClicksAverage() → int (~5 сек)
```

### 4.6 `math` (JSMathLib)
```
map(v, inMin, inMax, outMin, outMax) → double
clamp(v, min, max) → double; lerp(a,b,t) → double
dist2D(x1,z1,x2,z2); dist3D(x1,y1,z1,x2,y2,z2) → double
diff(a,b); min(a,b); max(a,b) → double
round(value, decimals=0) → double; smoothstep(t) → double (0..1)
```

### 4.7 `utils` (JSUtils) + глобальные функции
```
utils.playSound(name, volume=1.0, pitch=1.0)        // зарегистрированный звук
utils.copyToClipboard(text); utils.getClipboard() → string
utils.getFps() → int; utils.getSystemTime() → long(ms)
utils.formatNumber(d) → "1,234.5"; utils.formatInteger(l) → "1,234"
utils.sha256Hex(t); utils.sha256Base64Url(t); utils.randomString(len)
utils.getRenderDistance/setRenderDistance(v)  utils.getCloudsMode/setCloudsMode("off"|"fast"|"fancy")
utils.getCloudRange/setCloudRange  utils.getParticleStatus/setParticleStatus  utils.isEntityShadows/setEntityShadows
utils.setTimeout(fn, delayMs=0) → id     utils.setInterval(fn, periodMs=0) → id
utils.clearTimeout(id); utils.clearInterval(id)

// глобальные обёртки (инжектятся в каждый модуль):
setTimeout(fn, delayMs) → id     setInterval(fn, periodMs) → id
clearTimeout(id); clearInterval(id)
```

### 4.8 `console` и журнал
```
console.log/info(msg)  console.warn(msg)  console.error(msg)
// пишут в console.log модулей (config/starredheltix/modules/console.log) + SLF4J; warn/error — в чат при включённой опции.
```

### 4.9 `camera` (JSCamera) — свободная камера (глобальный)
```
camera.enable() → bool / disable() / toggle() / isEnabled()
camera.getPos() → {x,y,z} ; camera.setPos(x,y,z)
camera.setRot(yaw, pitch) ; camera.clearRot() ; camera.isRotOverridden()
camera.getRot() → {yaw, pitch} (эффективный) ; camera.getPlayerRot() ; camera.getPlayerPos()
// Отцеплена = WASD-движение игрока подавляется; авто-возврат при unload модуля и disconnect.
```

### 4.10 `textComponent` (JSTextComponentFactory) — MC-компоненты
```
textComponent.of(text) | literal(text) | translatable(key[, fallback]) | empty()
   → JSTextComponent (все мутации возвращают this — цепочки):
  .color(0xRRGGBB) .formatting("&a"|"gold") .bold() .italic() .underlined() .strikethrough() .obfuscated()
  .resetStyle() .onClick("run_command"|"suggest_command"|"open_url"|"copy_to_clipboard", value)
  .onHover(textOrComponent) .append(stringOrComponent) .getString()
chat.displayComponent(component)   // вывод в чат
```
---

## 5. СОБЫТИЯ (`events`)

### Подписка (возвращает SHTrigger с `.unregister()`)
```
events.onTick(fn)                 events.onChat(fn)             events.onChatOutgoing(fn)
events.onChatCancellable(fn)      events.onChatOutgoingCancellable(fn)
events.onWorldRender(fn)          events.onOverlay(fn)          events.onGuiOpen(fn)
events.onGuiClose(fn)             events.onGuiClick(fn)         events.onEntityDeath(fn)
events.onSlotUpdate(fn)           events.onSkillXp(fn)          events.onKey(fn)
events.onConnect(fn)              events.onDisconnect(fn)       events.onEntityLoad(fn)
events.onEntityUnload(fn)         events.onDayStart(fn)         events.onNightStart(fn)
events.onScreenOpen(fn)           events.onScreenClose(fn)      events.onPickup(fn)
events.onDamage(fn)               events.onSoundPlay(fn)        events.onSoundPlayWithCriteria(name, fn)
events.onPacketReceive(fn)        events.onPacketSend(fn)
events.onFps(fn)                  // + алиасы onFrame/onRender — покадрово, троттлинг ~8мс

// CTJS-стабы (принимаются без ошибки для портов, но пока НЕ вызываются):
// onSpawnParticle, onRenderEntity, onRenderBlockEntity, onDrawBlockHighlight, onGuiKey,
// onGuiMouseClick, onGuiMouseDrag, onPlayerInteract, onItemTooltip, onDropItem, onMessageSent

// Межмодульная шина (SHEventBus):
events.on("событие", fn)   events.once("событие", fn)   events.emit("событие", ...data) → int(кол-во получателей)
// Префикс "sh." зарезервирован для движка (emit с ним отклоняется с warn).
```

### Сигнатуры колбэков (что получает fn)
| Событие | Аргументы |
|---|---|
| `onTick` | — (каждый END_CLIENT_TICK, ~20 раз/сек) |
| `onChat`/`onChatOutgoing` | `(message: string)` |
| `onChatCancellable` | `(message: string, event: {cancel()})` — ДО показа; может быть на сетевом потоке |
| `onChatOutgoingCancellable` | `(message: string, event: {cancel()})` — ДО отправки на сервер |
| `onWorldRender` | `(ctx: 3D RJC render context)` — см. рендер |
| `onOverlay` | `(ctx: 2D overlay context)` — каждый кадр, поверх HUD |
| `onGuiOpen`/`onGuiClose` | `(screenTitle: string)` — контейнерные экраны (сундук, GUI Skyblock) |
| `onScreenOpen`/`onScreenClose` | `(screenClassName: string)` — ЛЮБОЙ экран (Inventory, Chat, Pause...) |
| `onGuiClick` | `(x, y, button)` |
| `onFps`/`onFrame`/`onRender` | — (каждый кадр, троттлинг ~8мс) |
| `onEntityDeath` | `(name, typeId, uuid)` |
| `onSlotUpdate` | `(slotIndex, itemId, count)` |
| `onSkillXp` | `(data: {skill, gained, current, target, isMax, percent})` — ОДИН объект-аргумент |
| `onKey` | `(key, scancode, action, modifiers)` |
| `onSoundPlay` | `(name, volume, pitch)`; `onSoundPlayWithCriteria(name, fn)` — фильтр при регистрации |
| `onPacketReceive`/`onPacketSend` | `(packetType, {type, fullType})` — только имя пакета (песочница) |
| `onPickup` | `(itemId, amount)` |
| `onDamage` | `(entityName, damageTypeId, isSelf)` |
| `onConnect`/`onDisconnect` | — |
| `onEntityLoad`/`onEntityUnload` | `(name, typeId, uuid)` |
| `onDayStart`/`onNightStart` | — |

### Триггеры и приоритеты
- Возвращаемый объект: `{unregister(): void}`.
- Приоритеты внутри движка: HIGHEST=0, HIGH=1, NORMAL=2, LOW=3, LOWEST=4.
- Ошибка в колбэке → `ScriptConsole.error` + авто-удаление триггера.
---

## 6. РЕНДЕР

### 6.1 3D-батчи: `render` (worldSpace, персистентные примитивы)
```
render.batch(id) → JSBatchHandle   // получить/создать батч модуля
render.removeBatch(id)             // удалить батч
render.clearBatches()              // все батчи модуля
// Текстуры:
render.loadImage(url, id) → bool            // асинхронно, PNG/JPEG ≤8МБ; id → "starredheltix:module/<модуль>/<id>"
render.loadImageFromFile(fileName, id) → bool // из папки модуля
render.removeImage(id); render.clearImages()
```
`JSBatchHandle`:
```
addBox(x,y,z, w,h,d, color=0x55FF5555, filled=false, throughBlocks=true, thickness=0)
addLine(x1,y1,z1, x2,y2,z2, color=0xFFFFFF, thickness=2)
addText(text, x,y,z, scale=1, color=0xFFFFFF, shadow=true, seeThrough=true)
addBeacon(x,y,z, height=256, color=0xFF0000)      // световой луч
addCircle(x,y,z, radius, color=0xFFFFFF, thickness=2, segments=32, axis="y"|"x"|"z")
addPolygon(x1,y1,z1, x2,y2,z2, ...)               // замкнутый периметр линиями
addImage(textureId, x,y,z, width, height, rotationY)
clear()  // пустой батч; лимит 20 000 примитивов (превышение → warn 1 раз/мин, добавление игнорируется)
```
Цвет — ARGB: `0xAARRGGBB` (0xFF000000|0xFF оф). Батчи рисуются ядром каждый кадр БЕЗ запуска JS.

### 6.2 2D-контекст: `ctx` (overlay / gui.render / hud.render)
Доступен в `events.onOverlay(fn)`, `gui.openScreen({render})`, `hud.register(...)`:
```
drawString(text, x, y, color=0xFFFFFF, shadow=true)
drawCenteredString(text, x, y, color, shadow)
drawRect(x, y, w, h, color)  drawGradientRect(x,y,w,h, colorTop, colorBottom)
drawHLine(x1,x2,y,color)  drawVLine(x,y1,y2,color)  drawBorder(x,y,w,h,color)
drawCircle(cx, cy, radius, color, outlineColor=color)   // заливка круга
drawTexture("id", x,y,w,h, u=0,v=0, texW=256, texH=256, color=0xFFFFFF)
pushClip(x,y,w,h) / popClip()          // scissor
pushTransform() / popTransform() / translate(dx,dy) / scale(sx,sy) / rotateAround(rad, px, py)
drawItem("minecraft:diamond", x, y)  drawItemWithCount("id", x, y, count)
```
В `hud.register` ctx — относительные координаты (x,y = левый верх элемента, есть `.width/.height/.x/.y`, всё read-only; позиция меняется через хендл `el.setX/setY`).
В `worldRender` (3D) ctx другой: `tickDelta`, `cameraX/Y/Z`, `isVisible(x,y,z,w,h,d)`, `distanceSq(x,y,z)`, `drawBox`, `drawText`, `drawLine`, `drawBeacon`, `drawCircle`, `drawImage(...)`, `drawAnimatedImage(...)`.

### 6.3 `hud` (кастомные элементы)
```
hud.register(id, width, height, renderFn) → hudElement
// renderFn(ctx) вызывается при отрисовке элемента; ctx — HUD-контекст с relX/relY.
// Авто-очистка при выгрузке модуля.
// Хендл JSHudElement (только чтение x/y/scale — позицию меняем сеттерами!):
el.x / el.y / el.scale                     // read-only
el.setX(v) / el.setY(v) / el.setScale(v)
el.showBackground / el.setShowBackground(b)
el.customWidth / el.customHeight ; el.setCustomWidth(v) / el.setCustomHeight(v)
el.isEditing ; el.isHovered ; el.getScaledWidth() / el.getScaledHeight()
el.increaseScale() / el.decreaseScale()    // шаг масштаба
// ctx.x / ctx.y — read-only, это НЕ способ задать позицию. Позиция — через el.setX/setY.
```

### 6.4 `gui` (полноценные экраны)
```
gui.openScreen({ title, pausesGame, render(ctx, mouseX, mouseY),
                onClick(x,y,button)→bool, onKey(keyCode,scanCode)→bool,
                onChar(char)→bool, onScroll(x,y,delta)→bool, onClose() }) → screen
gui.closeScreen();  gui.isScreenOpen() → bool
// Хендл JSGuiScreen: screen.open() — переоткрыть; screen.silentClose() — закрыть без onClose()
```
Экран закрывается автоматически при выгрузке модуля или ESC.
---

## 7. ИНВЕНТАРЬ, ПРЕДМЕТЫ, МИР, БЛОКИ

### 7.1 `inventory`
```
getItems() → JSItem[]                    // 0-8 хотбар, 9-35 рюкзак
getItemInSlot(slot) / getItemInHotbar(slot) → JSItem?
getHeldItem()/getOffHandItem() → JSItem?       // hand/offhand
getArmorItem("feet"|"legs"|"chest"|"head"|"offhand") → JSItem?
getSelectedHotbarSlot() → int; setSelectedHotbarSlot(0..8)
findSlotByName(name) → int?              // частичное совпадение без регистра
// Открытый контейнер (сундук, GUI Skyblock):
isInContainer() → bool
getContainerItems() → JSItem[]; getContainerItemInSlot(slot) → JSItem?
getContainerTitle() → string?
clickContainerSlot(slot, clickType="left"|"right"|"shift"|"middle")  // клик мышью по слоту
closeContainer()
```
`JSItem`: `getName() → string`, `getID() → "minecraft:item"`, `getCount()`, `getMaxStackSize()`, `isEmpty()`, `getLore() → string[]`, `getTooltip() → string[]`, `getNBT() → {key: string,...}`.

### 7.2 `item` (подробный, JSItemBound)
```
item.getHeld() / item.getOffHand() / item.getSlot(0..35) → JSItemDetails?
```
`JSItemDetails`: `getId()`, `getName()`, `setName(nameOrComponent)`, `getCount()`, `getDamage()`, `getMaxDamage()`, `getDurability()`, `getRarity()` ("common"|"uncommon"|"rare"|"epic"), `getEnchantments()` → [{id, level}], `getEnchantmentLevel(id)` → int, `getLore()`, `setLore(lore[])`, `getLoreComponents()`, `getFullTooltip()`, `hasCustomName()`, `isEnchanted()`, `isDamageable()`, `isEmpty()`.

### 7.3 `world`
```
// Данные мира:
getTime() → long(ticks); getDifficulty() → "peaceful"|"easy"|"normal"|"hard"
getTicksPerSecond() → int; getSkyDarken() → int
isRaining() → bool; getRainingStrength() → float(0..1)
getBorder() → {centerX,centerZ,size,targetSize,timeUntilTarget}
getSpawn() → {x,y,z,dimension}
// Скорборд/таб/тайтлы (детекторы):
getScoreboardTitle()/getScoreboardLines(); getActionBar()
getScoreboardEntries() → [{owner, value, display}]      // сырые записи Sidebar
getTabListHeader()/getTabListFooter()/getTabListLines(); getTitle()/getSubtitle()
// Редактирование (клиентски, сбрасывается сервером):
setScoreboardTitle(s) ; setScoreboardLine(score, s[, override]) ; addScoreboardLine(score, s)
removeScoreboardLines(score)
setTabListHeader(s)/setTabListFooter(s)/clearTabListHeader()/clearTabListFooter()
getPlayerTabName(name) → string?
getBlockEntityType(x,y,z) → "minecraft:chest"?          // блок-энтити
// Блоки и сущности:
getBlock(x,y,z) → string? (registry id) ; getBlockName(x,y,z) → string? (алиас)
getBlockAt(x,y,z) → JSBlock?                            // объект блока
getBlockType(name) → JSBlockType? ; getBlockType(x,y,z) → JSBlockType?
getLookedAtBlock(maxDistance=6) → {block,x,y,z,hitX,hitY,hitZ}? | null
getLookedAtEntity(maxDistance) → JSEntity?
getAllEntities() → JSEntity[] ; getEntitiesByName(s) ; getEntitiesOfType(s) (алиас getEntitiesByType)
getEntitiesInRadius(x,y,z,radius) → JSEntity[]
getPlayers(radius=128) → JSEntity[] (кроме себя) ; getPlayerByName(s) → JSEntity? ; hasPlayer(s) → bool
getClosestEntityByName(s) → JSEntity? ; getNearestPlayerTo(x,y,z) → JSEntity?
getDistanceToPlayer(x,y,z) → double
// Прочее:
getDimension() → "minecraft:overworld"? ; getLoadedChunkCount() → int ; getCameraPos() → {x,y,z}
getSkyLightLevel(x,y,z)/getBlockLightLevel(x,y,z) → int(0..15)
getServerIP() → string?
isLoaded() → bool
getAllBlockEntities(limit=512) → JSEntity[] ; getBlockEntitiesInRadius(x,y,z,radius,limit=128) → JSEntity[]
getAllEntitiesOfType("net.minecraft.world.entity.monster.Zombie") → JSEntity[]   // фильтр по Java-классу
getChunk(x,y,z) → ?
```

### 7.4 `JSBlockType` / `JSBlock` / `JSBlockPos` (аналог ctjs Block API)
```
JSBlockType (статический фасад):
  JSBlockType.of("minecraft:stone") → JSBlockType?        // null если блок не найден
  JSBlockType.fromID(int) → JSBlockType?                  // id из BuiltInRegistries.BLOCK
  .getID() → int ; .getRegistryName() → string ; .getTranslationKey() → string
  .getName() → string (локализовано) ; .getLightValue() → int
  .canProvidePower() → bool ; .getDefaultState() → BlockState ; .isSolid() → bool

JSBlock (экземпляр в мире):
  JSBlock.of(x,y,z) → JSBlock? ; JSBlock.of(pos: JSBlockPos) → JSBlock?
  .type → JSBlockType (JS: getType()) ; .pos → JSBlockPos (JS: getPos())
  .x/.y/.z → int
  .getBlockName() → string? ; .getState() → BlockState?
  .isEmittingPower([face]) → bool ; .getEmittingPower([face]) → int (0..15)
  .isReceivingPower() → bool ; .getReceivingPower() → int
  .getHardness() → float ; .isLiquid() → bool ; .isSolid() → bool
  .getNeighbor(face) → JSBlock? ; .getNeighbors() → JSBlock[]
  .canBeHarvested() → bool (текущий предмет в руке ломает без дропа-ограничений)
  .withType(t)/.withPos(p)/.withFace(f) → JSBlock (иммутабельные копии)

JSBlockPos (неизменяемый):
  JSBlockPos(x,y,z) ; .x/.y/.z ; .up(n)/.down(n)/.north(n)/.south(n)/.east(n)/.west(n) → JSBlockPos
  .offset(face, n) ; .plus(other) ; .minus(other) ; .distanceTo(other) ; .distanceToPlayer()
  .getBlock() → JSBlock? ; .toMC() → BlockPos ; .toVec3d() → Vec3

JSBlockFace (объект): JSBlockFace.DOWN/UP/NORTH/SOUTH/WEST/EAST, .parse("north") → JSBlockFace?
  .getName() .getOpposite() .getOffsetX()/getOffsetY()/getOffsetZ()

JSEntity (обёртка сущности): getName/getUUID/getType("minecraft:zombie")/isPlayer/isLiving/isArmorStand
  getX/getY/getZ/getBlockX/getBlockY/getBlockZ/getDistanceToPlayer/distanceTo(x,y,z)
  getHealth/getMaxHealth/getDisplayName/getYaw/getPitch
  isSprinting/isSneaking/isOnGround/isInWater/isInLava/isOnFire/isInvisible/isGlowing/isSpectator
  getTeamName/getTeamColor/setGlowing(bool) ; setGlowing(true, 0xFFFF5555) — цвет через ModuleGlowRegistry (ARGB double)
  // ctjs-паритет:
  isDead() ; getMotion()→{x,y,z} ; getMotionX/Y/Z() ; getLastX/Y/Z() ; getRenderX/Y/Z([pt])
  getWidth()/getHeight() (хитбокс) ; getEyeHeight() ; getTicksExisted() ; getAir()
  getRiding()→JSEntity? ; getRiders()→JSEntity[] ; isImmuneToFire() ; isWet() ; isSilent()
  hasNoClip() ; getStepHeight() ; getLookVector()→{x,y,z} ; getPing() (игроки, из таб-листа)
  getClassName() ; isBurning() ; isCollided() ; getDistanceWalked() ; getFireResistance()
  canBeCollidedWith() ; canBePushed() ; isOutsideBorder() ; getEyePosition([pt])→{x,y,z} ; getMaxInPortalTime()
  getDimension() ; distanceToSqr(x,y,z) ; distanceToEntity(other) ; distanceToSqrEntity(other)
  setGlowColor(ARGB-double) ; clearGlow() ; getGlowColor() → ARGB-double

### `Java` (глобальный Java.type, Nashorn-стиль — только whitelist)
```
Java.type("org.joml.Vector3f") → NativeJavaClass? (null + warn, если заблокирован/не найден)
Java.isAllowed("pkg.Class") → bool   // проверка без загрузки
// Разрешено: java.*, kotlin.*, set.starlev.scripting.*, net.fabricmc.fabric.api.*, net.fabricmc.api.*,
//            org.joml.*, io.github.notenoughupdates.moulconfig.*
// НЕТ: net.minecraft.* (античит-поверхность), reflect/Runtime/Process/Thread/io/net/nio/System/имплы
var Vec = Java.type("org.joml.Vector3f"); var v = new Vec(1,2,3);
// SAM-интерфейсы fabric-api: JS-функция → лямбда
// ВНИМАНИЕ: колбэки fabric выполняются вне движка (без антифриза/автоотписки) — для игровых
// событий используйте events.*; fabric из JS — для утилитарных вызовов (математика, реестры).
```
---

## 8. ПРОЧИЕ ГЛОБАЛЫ

### `bossbars`
```
bossbars.getAll() → JSBossBar[] ; bossbars.getByName(name) → JSBossBar[]
bossbars.add(name, percent=1.0) → JSBossBar    // клиентский боссбар
bossbars.remove(bar) ; bossbars.removeByName(name) ; bossbars.clearAll()
JSBossBar: getUUID()/getName()/setName(s)/getPercent()/setPercent(0..1)
```
Надписи: `&` → `§` автоматически.

### `toast`
```
toast.show(title, message="", durationMs=4000)
toast.clearAll()
```

### `file` (JSFileLib, песочница папки модуля)
```
file.write(name, content) → bool;  file.append(name, content) → bool
file.read(name) → string?;         file.exists(name) → bool;  file.delete(name) → bool
file.list(dir="") → string[]
file.readJson(name) → any?;        file.writeJson(name, data) → bool
file.getUrlContent(url) → string?  // http(s) только, лимит 8МБ
file.encodeBase64(s) / file.decodeBase64(s)
// Песочница: resolve() запрещает выход за пределы config/starredheltix/modules/<module>/ (через ..)
```

### `storage` (data.json)
```
storage.get(key[, default]) → any? ; storage.set(key, value) ; storage.has(key) → bool
storage.remove(key) → bool ; storage.clear() ; storage.load() ; storage.save()
// set(null) = remove; set сохраняет в data.json сразу
```

### `config` (config.json + GUI-схема)
```
config.define(values[, schema])     // defaults; существующие не перезаписываются
config.get(key[, default]) → any? ; config.set(key, value) ; config.has(key)
config.remove(key) ; config.getAll() → object ; config.openMenu()  // MoulConfig с фильтром
config.defineSchema({ key: {type, min?, max?, step?, options?, description?} })
// types: "switch" | "slider" | "dropdown" | "color"("a:r:g:b") | "text"
// schema сохраняется под "config.json"→"__schema"
```

### `keybind`
```
keybind.register("R"|"F5"|"LEFT_SHIFT"|"KP_0"|GLFW code, fn) → bool
keybind.unregister(keyName) ; keybind.unregisterAll()
// fn вызывается ТОЛЬКО на нажатие (не на повтор/отпускание)
```

### `sound`
```
sound.play(name, volume=1.0, pitch=1.0, category="players") → bool   // у игрока
sound.playAt(name, x,y,z, volume=1.0, pitch=1.0, category="master") → bool // в точке
sound.getCategories() → ["master","music","records","weather","blocks","hostile","neutral","players","ambient","voice"]
// Кастомные ogg из папки модуля:
sound.playFile(fileName, opts={volume,pitch,loop,loopDelay,x,y,z,category}) → string(handle)? | null
sound.stopFile(handle) → bool ; sound.stopAll()
// opts: volume 0..4, pitch 0.5..2, loop bool, loopDelay в тиках; category по getCategories
// Только .ogg; песочница; ≤100 активных звуков/модуль; автостоп при unload
```

### `particles` (клиентские, без эффекта на FPS-метрики сервера)
```
particles.spawn("minecraft:heart", x,y,z, dx=0,dy=0,dz=0, count=1) → bool
particles.sphere(name, x,y,z, radius, count=16) → bool
particles.line(name, x1,y1,z1, x2,y2,z2, count=10) → bool
particles.cylinder(name, x,y,z, radius, height, count=24) → bool
```

### `npc` (клиентские NPC-игроки — уникальная фича)
```
npc.create({name, x,y,z, yaw=0, pitch=0, showName=true, skin="steve"|"lic:Имя"|"assets:..."|"url:https://..."|UUID,
            morph="minecraft:zombie"?, onClick(fn)?, lookAtPlayer=false}) → JSNpcHandle | null
npc.removeAll()
JSNpcHandle:
  getName/setName ; getPos()→{x,y,z} ; setPos(x,y,z) ; move(dx,dy,dz)
  getYaw/setYaw ; getPitch/setPitch ; lookAtPlayer() ; setLookAtPlayer(b) ; isLookingAtPlayer()   // lookAt(x,y,z) НЕ существует
  setLookSpeed(0.01..1) ; showName(b) ; say(message) ; onClick(fn) ; morph(type? ) ; getMorph() ; remove()
```

### `net` (HTTP)
```
net.get(url, headers?) → Response
net.getJson(url, headers?) → any?             // парсинг JSON тела (уже готовый объект), null при ошибке
net.post(url, body, headers?, contentType="application/x-www-form-urlencoded") → Response
net.postJson(url, data, headers?) → any?      // отправка JSON + парсинг ответа
net.request(method, url, body?, contentType?, headers?) → Response?
net.getAsync(url, cb, headers?)              // cb(resp) в фоновом потоке
net.encodeForm(obj) → string
net.listen(port, cb) → bool   // локальный OAuth-сервер 127.0.0.1: cb(path, query, headers) → string-ответ
net.stopListen()
Response: .status/.getStatus() → int; .body/.getBody() → string; .getHeader(name) → string?; .isOk()/.isNotFound()/.isUnauthorized()
// Синхронные методы блокируют поток вызова — из tick/chat используйте getAsync.
```

### `Scoreboard` (+`Team`, `Score`) — глобальный ванильный сайдбар
```
Scoreboard.getTitle()/setTitle(s) ; getLines()/getLineByIndex(i)/getLinesByScore(s)
Scoreboard.setLine(score, s[, override]) ; addLine(score, s) ; removeScores(score) ; removeIndex(i)
Scoreboard.getScores() → Score[] ; getScore(index) ; getScoresByScore(s) ; getScoreByName(name|displayName)
Score: getName/getDisplayName/getScore/setScore(v)/setName/getTeam/setTeam/remove/isHidden/getNumberFormat
// Кастомные строки модуля (рендер ModuleCustomScoreboard, авто-очистка при unload):
Scoreboard.addCustomLine(id, text, anchor="BOTTOM"|"AFTER_XP", order=99)
Scoreboard.setCustomLine(id, text) ; removeCustomLine(id) ; getCustomLines() ; clearCustomLines() ; getAllCustomLines()
// Команды: Scoreboard.createTeam(name)/getTeam(name)/removeTeam(name)
Team: getName/setDisplayName/getDisplayName ; getPrefix/setPrefix ; getSuffix/setSuffix ; getColor/setColor
      getMembers/addMember/removeMember ; setFriendlyFire(b) ; setSeeFriendlyInvisibles(b)
      setNameTagVisibility/deathMessageVisibility/collisionRule("ALWAYS"|"HIDE_FOR_OTHER_TEAMS"|...)
// Примечание: world.get/setScoreboard* (см. 7.3) — тот же сайдбар, детекторный фасад.
```

### `Maps`/`maps`, `DungeonMap`/`dungeonMap` — динамические 2D-картины
```
Maps.create(id, w=128, h=128) → JSMapHandle?  ; Maps.get(id) ; Maps.remove(id) ; Maps.list()
JSMapHandle (пиксели в CPU-буфере, на GPU — после update()):
  setPixel(x, y, colorARGB-double) ; getPixel(x,y) → int ; fill(color) ; clear()
  update()   // залить в DynamicTexture (starredheltix:module/<модуль>/map_<id>)
  getTextureId() → string → ctx.drawTexture(...) ; destroy()
// авто-удаление текстур при unload модуля; батч-прайс — как у render.loadImage
DungeonMap: isInDungeon()/hasMap()/getMapId()/getCrypts() → int?
            getDecorations() → [{x, y, rot, name, hasIcon}] ; getMapColors() → int[]? (пиксели)
            getMapCenter() → {x, z}? (ось Z, не Y!) ; getMapScale() → int?
            Maps.dungeon()  // тот же объект
```

### `Renderer`, `Display`, `Text`, `Image` — ctjs-совместимость (глобальные классы)
```
Renderer (статика): color("GREEN")→ARGB-long ; getColor(r,g,b[,a])→long ; getRainbow(step[, speed])→long
  // color() ждёт ИМЯ ChatFormatting в верхнем регистре ("GREEN","GOLD"); "&a" вернёт WHITE
  // константы: BLACK DARK_BLUE GREEN AQUA RED PURPLE GOLD GRAY DARK_GRAY BLUE LIGHT_PURPLE YELLOW WHITE
  getStringWidth(s)→int ; getFontRenderer() ; drawRect(color,x,y,w,h) ; drawLine(c,x1,y1,x2,y2,thickness)
  drawCircle(c,x,y,r,steps=16) ; drawString(s,x,y) ; drawStringWithShadow ; drawImage(img,x,y,w,h)
  pushMatrix()/popMatrix()/translate(x,y[,z])/scale(x,y[,z])/rotate(angle,x,y,z)
  drawBox(color,x,y,z,w,h,d[,filled])   // 3D (в worldRender)
  // рисует в текущем 2D-ctx (overlay/hud/gui) или world-ctx — устанавливается движком
new Display({x,y,backgroundColor,textColor,background:"NONE"|"FULL"|"PER_LINE",align,order:"NORMAL"|"REVERSED",minWidth})
  // «живой» виджет: регистрируется в DisplayManager, рисуется каждый кадр БЕЗ JS; .destroy()/remove() + авто-очистка при unload
  .addLine(stringOrText) / setLines(...) / getLine(i) / removeLine(i) / clearLines()
  .setX/setY/setWidth/setHeight/setScale/setTextColor/setAlign/setOrder/setBackground/setBackgroundColor
  .getMinWidth/setMinWidth ; .shouldRender()/setShouldRender(b) ; .remove()/destroy()
new Text(string[, x, y] | {color, shadow, formatted, align, scale, background, maxWidth, maxLines})
  .setString/getString/setColor/getColor/setShadow/isShadow/setScale/getScale/getWidth/getHeight/getMinWidth/getMaxWidth
Image: Image.fromUrl(url[,cachedName]) / fromFile(путьВПапкеМодуля) / fromAsset("namespace:path.png") / fromBufferedImage
  .getTextureWidth()/getTextureHeight()→int   // размер ТЕКСТУРЫ, не экранный
  .getTexture() ; .draw(x,y[,w,h]) ; .destroy()
  // текстуры регистрируются как starredheltix:ctjs_img_* и освобождаются при unload
```
---

## 9. ДЕТЕКТОРЫ (объект `detector`, алиас `detectors`)

19 готовых детекторов Heltix-состояния:
```
detector.actionBar.getText() → string
detector.biome.get() → string
detector.chat.getLastMessage() → string;  .getHistory(limit) → string[];  .cleanColor(s)
detector.container.getInfo() → {rawTitle, decodedTitle, normalizedTitle, screenClass}?;  .decodeTitle(s)
detector.dungeon.isInDungeon() → bool
detector.enchantProgress.getHeldData()/.getActiveData() → [{type,displayName,skillName,level,current,target}]
detector.entityDeath      // слушатели — через events.onEntityDeath
detector.heldItem.getTooltipText(); .isTooltipVisible()
detector.itemLore.getHeldLore(); .getHeldFullTooltip()
detector.location.get()/.getClean(); .isIn(name); .isInAny(...names); .resetCache()
detector.mobHead          // через экземпляры сущностей
detector.museum.isInZone(); .isOpen()
detector.playerItem.getMainHandItemName(); .getOffHandItemName()
detector.scoreboard.getLines()/.getTitle()/.getCookieStatus()
detector.skillXp.getLastSkill(); .getLastInfo() → {gained,skill,current,target,isMax,percent,lastUpdate}?
detector.slotPhase.detectHeld() → "NONE"|...
// detector.stats НЕ СУЩЕСТВУЕТ. CPS/BPS только через глобальный cps и player.
detector.tabList.getHeader()/.getFooter()/.getAllLines()/.getAllLinesFormatted()/.getGems()/.getBank()/.getGemsLine()/.getBankLine()/.cleanLine()
detector.time.getTime()/.getDate()/.getYear()/.getFullDateTime()
detector.title.getText()/.getSubtitle()/.isVisible()
```

---

## 10. СХЕМА АРГУМЕНТОВ КОМАНД (таб-комплит)

```
command.register(name, fn)                      // классика: fn(args[]) — массив сырых строк
command.register(name, fn, {args: [...]})       // со схемой: fn(args[]) — типизированные значения
command.unregister(name)
```

Схема: `{ name, type, min?, max?, options? }`
```
types: "int" (min/max) | "float" (min/max) | "bool" | "word" | "string" (в кавычках)
       | "greedy" (только последний; захватывает остаток) | "choice" (options → таб-подсказки)
```
- Аргументы факультативны: не передал → `null` в массиве аргументов.
- `int` → JS number; `float` → number; `bool` → boolean; остальные → string.
- Битая схема → warn в лог и регистрация в классическом режиме.
---

## 11. ПРАВИЛА ДЛЯ АВТОРОВ (важно для ИИ)

### Производительность (приоритет №1 по RULES.md)
- TIck/WorldRender/Overlay — hot-path. Не делайте тяжёлых вычислений, парсинга, HTTP в колбэках, вызываемых каждый кадр. Кэшируйте результат.
- Для постоянного рисования в мире — `render.batch(...)` (ядро рисует нативно, JS не вызывается в кадре).
- Заменяйте громоздкие `drawRect`/`drawString` в overlay одним `hud.register` (тоже перерисовка по кадрам — держите данные в переменных, а не пересоздавайте строки).
- `console.log` в колбэках кадров — только при отладке.
- Таjмеры — фоновые (ScheduledThreadPool daemon), не блокируют рендер. НО не чаще, чем нужно.

### Потокобезопасность
- События `tick`, `worldRender`, `overlay`, `onChat*` (входящий может быть на сетевом потоке!) передают разный контекст.
- Из фоновых потоков (`net.getAsync`, `file.getUrlContent`, таjмеры — Таjмеры НЕ фоновые, они в withContext) НЕ вызывайте Minecraft API напрямую: используйте `client.execute(...)`.
- Если не уверены — оборачивайте MC-вызовы в `client.execute(() => ...)`.

### Антифриз (3 секунды)
- Бесконечные/тяжёлые циклы в tick/chat → модуль снимается с ошибкой. НЕПРИЕМЛЕМО: `while(true)`, `for(;;)` без выхода, рекурсия без ограничения.

### Песочница
- Файлы доступны только в папке модуля (`file.*`, `sound.playFile`, `render.loadImageFromFile`).
- Java-классы вне белого списка → ошибка доступа. Не пытайтесь `Java.type("java.io.File")`.

### Ошибки
- Любые throwable в колбэках логируются в `console.log` и чат (если включено). Ошибка в триггере → автоген unregister.
- Проверяйте `null`: `player.getX()` — 0.0 вне мира; `inventory.getItemInSlot(slot)` → null; `world.getLookedAtBlock()` → null.

### Цвета
- Используйте ARGB `0xAARRGGBB` (рендер) и `§`-коды (чат/HUD/текст). `&`-префикс автоматически конвертируется в `§` в `chat.*`, `hud`, `bossbars`, `toast`, `npc`.

---

## 12. РЕЦЕПТЫ (готовые куски)

### Минимальный хелло-ворлд (metadata.json + index.js)
```json
{ "name": "hello", "version": "1.0.0", "author": "Me", "entry": "index.js" }
```
```js
// index.js
events.onConnect(function() { chat.display("§aПривет!"); });
events.onTick(function() {});
```

### Hotkey-тумблер + сохранение
```js
var enabled = storage.get("enabled", true);
keybind.register("H", function() {
    enabled = !enabled;
    storage.set("enabled", enabled);
    chat.display(enabled ? "§aВкл" : "§cВыкл");
});
```

### Споттер/waypoint батчем
```js
var b = render.batch("mine");
b.addBeacon(player.getX(), player.getY(), player.getZ(), 64, 0xFF00FF00);
// при перемещении: render.removeBatch("mine"); b = render.batch("mine"); ...
```

### Модуль-библиотека
```js
// my-utils/metadata.json: { "name": "my-utils", "library": true }
module.exports = { greet: function(n) { return "Привет, " + n; } };
// в модуле: var u = require("my-utils"); chat.display(u.greet(player.getName()));
```

### Команда со схемой
```js
command.register("warp", function(a) {
    if (a[0] === null) { chat.display("§c/warp <x> <y> <z>"); return; }
    player.lookAt(a[0], a[1], a[2]);
}, { args: [
    { name: "x", type: "float" }, { name: "y", type: "float" }, { name: "z", type: "float" }
]});
```

---

## 12.5. Кастомные шрифты (TTF/OTF из папки модуля)

> Требует mod >= 0.2.0. Реализовано через `SHModuleFonts`: атлас глифов -> `DynamicTexture`.

```js
render.loadFontFromFile("fonts/myfont.ttf", "myfont", 48);   // ASCII + кириллица, sizePx=48 (базовый кегль)
ctx.drawFontText("myfont", "Привет", 10, 10, 24, 0xFFFFFF, true);  // в overlay/gui/hud
ctx.measureFontText("myfont", "текст", 24);                  // -> Metrics: m.width, m.height (или null)
render.removeFont("myfont");  render.clearFonts();           // авто-чистка при unload
render.textToTexture("Статичный текст", "label", "myfont", 24, 0xFF7DF9FF, false);
// -> Metrics m; рисуется: ctx.drawTexture("starredheltix:module/<модуль>/text/<id>", x, y, m.width, m.height)
```

- Атлас регистрируется в картах синхронно (шрифт доступен сразу после loadFontFromFile), GPU-текстура — на рендер-потоке.
- Атлас печётся синхронно один раз (~30-60 мс), регистрация текстуры — на рендер-потоке.
- `drawFontText` — блит по глифам из атласа (поддерживает трансформы/scissor); цвет ARGB целиком, `§`-коды не поддерживаются.
- `textToTexture` — для статичных надписей: один растер, ноль JS в hot-path (рисование обычным blit).
- id шрифта должен быть уникален между модулями (при конфликте побеждает последняя загрузка).
- Лимиты: файл ≤ 4 МБ, атлас ≤ 4096×4096. При выгрузке модуля шрифты удаляются автоматически.

## 13. ПАРИТЕТ С CT.js (ChatTriggers) — что и как

| CT.js | StarredHeltix | Статус |
|---|---|---|
| ChatLib.chat/actionBar/simulate/edit/delete/getChatLines | chat.* | ✅ |
| Player.* | player.* (+эффекты, биом, свет, XP, ввод) | ✅ |
| Client/Server | client.* | ✅ |
| Settings | settings.* | ✅ |
| KeyBind | keybind.register | ✅ |
| Sound (кастомные ogg) | sound.playFile/stopFile | ✅ |
| Image.fromFile/URL | render.loadImageFromFile / loadImage | ✅ |
| Display/Text (per-frame) | `new Display()`/`new Text()` — ctjs-совместимые живые виджеты (см. §8) | ✅ (но hud.register/render.batch эффективнее) |
| Renderer3d/LegacyPipeline | глобальный `Renderer` (ctjs-порт) + `batch.addBox/addLine/addCircle/addPolygon/addImage` | ✅ |
| Gui registers | gui.openScreen | ✅ |
| Commands (Brigadier DSL) | command.register + {args: schema} | ✅ |
| DynamicCommands.custom/entity-аргументы | нет (только choice-подсказки в схеме) | ❌ |
| Particle | particles.spawn/sphere/line/cylinder | ✅ |
| Item.getEnchantments/Rarity | item.getHeld().getEnchantments()/getRarity() | ✅ |
| Inventory API | inventory.* | ✅ |
| BossBar | bossbars.* | ✅ |
| Toast | toast.* | ✅ |
| FileLib | file.* + sandbox | ✅ |
| HttpRequest | net.get/post/postJson/getAsync | ✅ |
| TextComponent (объекты) | строки + RegExp | ⚠️ частично |
| ChatTrigger/StepTrigger и пр. | events.* + detector.* (скоп меньше) | ⚠️ |
| Scoreboard setTitle/setLine/addLine/removeScores | глобальный `Scoreboard`/`Team`/`Score` (+`world.setScoreboard*`) | ✅ (+кастомные строки модуля, §8) |
| TabList setHeader/setFooter/clear*/Name | world.setTabListHeader/Footer/clear*, getPlayerTabName; addName (fake-игроки) | ⚠️ (fake-игроков нет) |
| BlockEntity | world.getBlockEntityType(x,y,z) | ✅ (тип, без NBT) |
| Team API (создание команд) | Scoreboard.createTeam/getTeam/removeTeam + Team.* (клиентский сайдбар) | ✅ |
| Display/Book/Gui registers (мощный 2D DSL) | hud.register + gui.openScreen (проще, но не State-based) | ⚠️ |

---

*Конец справочника. Актуально для StarredHeltix 0.1.1 (Java 25 / MC 26.2 / Rhino 1.9.1).*

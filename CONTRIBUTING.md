# Участие в разработке StarredHeltix

Спасибо, что помогаете делать StarredHeltix лучше! Этот гайд покрывает оба направления: **Kotlin/Java (код мода)** и **JavaScript (модули)**.

## Содержание

1. [Окружение разработки](#окружение-разработки)
2. [Структура проекта](#структура-проекта)
3. [Код мода: как добавить фичу](#код-мода-как-добавить-фичу)
4. [Стиль кода и соглашения](#стиль-кода-и-соглашения)
5. [Миксины и API 26.2](#миксины-и-api-262)
6. [Меню настроек (MoulConfig)](#меню-настроек-moulconfig)
7. [Как написать JS-модуль](#как-написать-js-модуль)
8. [Изменение самого JS API](#изменение-самого-js-api)
9. [Чеклист перед PR](#чеклист-перед-pr)
10. [Релиз](#релиз)

## Окружение разработки

* **JDK 25** (обязательно — `JavaCompile release 25`, Kotlin `jvmTarget JVM_25`).
* **Gradle**: используй враппер (`gradlew.bat` / `./gradlew`), Gradle 9.7.0 скачается автоматически.
* На Windows перед каждым запуском враппера делай паузу (`Start-Sleep -Seconds 5`) — Gradle не
  переиспользует старый daemon, и мгновенный повторный запуск падает с
  `Gradle build daemon has been stopped`. Один таск за раз, всегда `--console=plain`.
* **IDE**: рекомендуется IntelliJ IDEA; импортируй как Gradle-проект и дай Loom сгенерировать исходники.
* MC **26.2** с Fabric — единственный вариант загрузчика, без мультиверсионных веток.

```
gradlew build          # полная сборка → build/libs/starredheltix-<вер>.jar
gradlew runClient      # дев-клиент (смотри лог на ошибки миксинов)
gradlew compileKotlin  # быстрая проверка компиляции
```

**Референсные исходники не в гите**: репозиторий полагается на локальную папку `scripts/`, которую игнорирует git:
`scripts/26.2.X SOURCE CODE/` (декомпил Mojang — источник правды по API),
`scripts/SOURCES/` (Odin, Firmament, Skysoft, SkyHanni, ctjs, MoulConfig, NoFrills, Skyblocker, SkyCubed, Zen, heltix-mod, CustomScoreboard, Borderless-Fullscreen, Rhino, Caffeine, Fabric API)
и `scripts/LAST/` (предыдущая версия мода на 1.21.10 — эталон для диффов и для `CHANGELOG.md`).
Дамп декомпила нужно попросить у мейнтейнера. Никогда не пиши API MC 26.x «по памяти» от 1.21.x —
сигнатуры сильно изменились (extract/submit-модель рендера, `GuiGraphicsExtractor`, `Identifier`...).

## Структура проекта

```
src/main/java/set/starlev/
  injections/accessors/   интерфейсы @Accessor/@Invoker
  injections/mixins/      все клиентские миксины (плоский пакет) + moulconfig/ (русизация GUI)
  duck/                   duck-интерфейсы, реализуемые через миксины
src/main/kotlin/set/starlev/
  StarredHeltix.kt        точка входа — ВСЕ фичи инициализируются здесь (порядок важен!)
  commands/               /sh (ConfigCommand)
  config/                 ConfigManager, Features (@Expose корень), categories/, highlight/, gui/
  events/                 EventBus, WorldChangeHandler, KeyBindHandler, ContainerEventWire...
  features/<домен>/       chat, combat(+dungeons,slayer), enchanting, farming(+garden),
                          fishing, foraging, mining, optimization, skyblock, visual(+swing)
  registry/               EntityRegistry (клиентские сущности)
  render/                 RenderEngine, RenderContext, HudManager, HudElement, редакторы,
                          GuiCompat, Design (SH-дизайн-кит), CustomGuis, NotifierGui, ScriptNPC
  scripting/              движок Rhino, загрузчик модулей, api/ (34 класса JS*)
  secret/                 пасхалки (не трогай без просьбы)
  utils/                  detectors/ (20), CacheManager, ColorUtils, GameModeBoost, InputMath,
                          ModUpdater, DiscordRPC, HttpUtils, TextUtils, tictactoe/...
src/main/resources/
  fabric.mod.json, starredheltix.client.mixins.json, starredheltix.accesswidener, assets/
docs/                     scripting-api.md (для игроков), scripting-api-AI.md (для ИИ), index.html
modules/                  JS-модули для каталога на GitHub (по папке на модуль)
scripts/RULES.md          АРХИТЕКТУРНАЯ БИБЛИЯ — прочитай до правок кода мода
scripts/LAST/             предыдущая версия мода (1.21.10) — только для справки, НЕ редактировать
CHANGELOG.md              что было в каждой версии
```

## Код мода: как добавить фичу

1. **Сначала детектор, если парсишь сырые события.** Добавь `object XDetector` в `utils/detectors/`
   (шаблон: `fun init(); fun registerListener((Fact) -> Unit)`) и подцепи его к событию миксина.
2. **Объект-фича** в `features/<домен>/YourFeature.kt` — Kotlin `object` с `fun init()`,
   подписанный на детекторы; HUD-фичи наследуют `render/HudElement`.
3. **Зарегистрируй в `StarredHeltix.onInitializeClient()`** — детекторы раньше слушателей,
   `SHScriptEngine.init()` остаётся последним. Инициализируй каждую фичу ровно один раз.
4. **Конфиг:** добавь опции `@Expose` в соответствующий `config/categories/<Domain>Config.kt`
   (для нового домена — создай файл и подключи его в `Features.kt`).
5. **Миксины:** если MC не отдаёт нужный доступ, добавь минимальную инъекцию в
   `injections/mixins/` (или accessor) — бизнес-логика остаётся в Kotlin-фичах.
6. **Запись в RULES.md** — обязательно: фича без строчки в архитектурной библиотеке
   через пару релизов становится «неизвестной».

## Стиль кода и соглашения

* Kotlin — для фич и логики, Java — только для миксинов, accessor'ов и duck'ов.
* Каждый новый `.java`: заголовок **LGPL-3.0-only** + javadoc с `@author` и `@reason` на миксинах.
* Комментарии и пользовательский текст — **по-русски**, технические доки — по-английски.
* **Логи:** мод пишет в лог ровно одну строку — `StarredHeltix v<ver> инициализирован.` в конце
  `onInitializeClient()`. Не добавляй `LOGGER.info`/`debug` в инициализаторы и на каждый шаг
  солвера; `warn`/`error` — только на реальные поломки. Исключения: `InputMath` (диагностика полей
  ввода) и `ScriptConsole` (`console.*` из JS-модулей).
* Никогда не создавай `ItemStack`/`Block`-синглтоны в `<clinit>` — используй `by lazy`.
* Горячие пути: early-return по `if (!enabled) return`; события вместо опроса в `onTick`;
  кешируй с временными метками и сбрасывай кеши в `WorldChangeHandler`.
* Клиентские фейковые сущности: `entity.setId(...)` **до** `level.addEntity(...)`.
* Всё, что можно сломать из UI или из конфига (GLFW, сеть, регекс, файлы) — под `try/catch`:
  настройка не должна ронять клиент.

## Миксины и API 26.2

* Официальные маппинги Mojang, без Parchment. `accessWidener v2 official`.
* В `starredheltix.client.mixins.json` стоит `defaultRequire = 1` — неверный дескриптор
  **крашит клиент на старте**. Всегда сверяй сигнатуры и дескрипторы с
  `scripts/26.2.X SOURCE CODE/` перед написанием инъекции.
* Диалект Fabric-Mixin: у `@ModifyConstant` **нет** `at()` (он переписывает все подходящие
  константы), поле — `stringValue = "…"`, а не `string`. В селекторе метода обязателен
  дескриптор возврата: `"render()V"` — ок, `"render()"` — `Invalid descriptor`.
* Рендер использует extract/submit-модель (`GuiGraphicsExtractor`, `SubmitNodeCollector`,
  `LevelRenderEvents.COLLECT_SUBMITS`); 2D-трансформации — `Matrix3x2fStack` через `graphics.pose()`;
  для текста используй расширения `set.starlev.render.drawString` вместо сырых вызовов.
* После любого бампа версии MC: `gradlew runClient` и grep лога на
  `InvalidInjectionException` / `Invalid descriptor`.

## Меню настроек (MoulConfig)

* **Фича = подкатегория** (`@Category`) внутри доменной категории. Вложенные `@Category`
  запрещены (MoulConfig не умеет), для групп настроек внутри подкатегории — `@Accordion`.
* **Длинный текст** нельзя класть в `GuiOptionEditorInfoText` (жёсткая ширина 100 px →
  нечитаемый 3-4 px текст). Бери `config/gui/SubcategoryHint.kt`.
* **Свой редактор** (как у цвета с кнопкой «Сброс») — это `config/gui/*.kt`, наследник
  `ComponentEditor` с собственным `GuiComponent`. Сеттера `setEditor` у `ProcessedOption` нет,
  поэтому редактор ставится **заменой опции в списке категории** на обёртку, делегирующую
  всё, кроме `getEditor()`.
* Новые экраны вроде `/sh binds`, `/sh aliases`, `/sh notifiers` рисуются на дизайн-ките
  `render/Design.kt` (`SHPanel`, `SHHeader`, `SHRow`, `SHFooter`, `SHListHelper`) — не изобретай
  свои кнопки.

## Как написать JS-модуль

Модули — обычные папки в `modules/` (запушил в этот репозиторий = опубликовал в каталоге):

```
modules/<имя>/
├── metadata.json   ← {name, version, author, description, entry, requires[], library?, minModVersion?}
├── index.js        ← точка входа; используй SH API (см. docs/scripting-api.md)
└── ...             ← require("./lib"), ассеты (картинки/шрифты/звуки поддерживаются)
```

* Без архивов, без шага сборки. Держи `version` в своей metadata (версия модуля ≠ версия мода).
* Используй `config.defineSchema`, чтобы игроки получили нативные GUI-редакторы; таймеры и HUD
  регистрируй через API — тогда они сами cleans up при выгрузке.
* Долгие задачи обязаны уступать поток: InstructionObserver убивает скрипты, работающие >3с за один триггер.
* Модули с `library: true` экспортируют через `module.exports`, на них ссылаются через `requires`.

## Изменение самого JS API

1. Реализуй в `scripting/api/JS*.kt`, зарегистрируй глобал в `SHScriptEngine` (песочница: добавь
   класс в белый список `SHClassShutter`, если это новый API-класс).
2. Убирай per-module состояние при выгрузке (таймеры, HUD, батчи, текстуры, шрифты, звуки, глоу,
   карты, кастомные строки табло, команды).
3. Документируй в **обоих** файлах: `docs/scripting-api.md` (полный) и `docs/scripting-api-AI.md` (сжатый),
   и обнови раздел про скриптинг в `scripts/RULES.md`.
4. По возможности добавь/обнови пример в `modules/`.
5. https://github.com/Starlevka/StarredHeltix/tree/main/docs

## Чеклист перед PR

- [ ] `gradlew build` проходит
- [ ] `gradlew runClient` стартует чисто (без ошибок миксинов и регистров)
- [ ] Фича зарегистрирована в `StarredHeltix.kt`, есть `@Expose` в конфиге, HUD уважает `enabled`
- [ ] Нет повторных вызовов `init()`, нет `ItemStack` в `<clinit>`, нет новых `LOGGER.info` в инициализаторах
- [ ] `/sh` открывается, `/sh reset config` работает, контейнеры/экраны не крашат игру
- [ ] Обновлён `scripts/RULES.md` (структура, порядок инициализации, новая фича)
- [ ] Новый JS API задокументирован (оба дока + раздел в RULES)
- [ ] Комментарии в коммитах и пользовательских фичах — по-русски; код и доки — по-английски

## Релиз

- [ ] Версия синхронно в `gradle.properties`, `fabric.mod.json`, fallback'ах `orElse(...)`
      (`StarredHeltix.kt`, `Features.kt`, `AboutConfig.kt`, `ModUpdater.kt`) и `docs/index.html`
- [ ] Собран `gradlew build`, взят `build/libs/starredheltix-<вер>.jar`
- [ ] Проставлена запись в `CHANGELOG.md`: версия, версия MC, что крупно нового и что убрано
- [ ] Проверено на dev-клиенте: `/sh`, HUD-редактор, меню модулей, новая фича из changelog
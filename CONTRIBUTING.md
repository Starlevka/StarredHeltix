# Участие в разработке StarredHeltix

Спасибо, что помогаете делать StarredHeltix лучше! Этот гайд покрывает оба направления: **Kotlin/Java (код мода)** и **JavaScript (модули)**.

## Содержание

1. [Окружение разработки](#окружение-разработки)
2. [Структура проекта](#структура-проекта)
3. [Код мода: как добавить фичу](#код-мода-как-добавить-фичу)
4. [Стиль кода и соглашения](#стиль-кода-и-соглашения)
5. [Миксины и API 26.2](#миксины-и-api-262)
6. [Как написать JS-модуль](#как-написать-js-модуль)
7. [Изменение самого JS API](#изменение-самого-js-api)
8. [Чеклист перед PR](#чеклист-перед-pr)

## Окружение разработки

* **JDK 25** (обязательно — `JavaCompile release 25`, Kotlin `jvmTarget JVM_25`).
* **Gradle**: используй враппер (`gradlew.bat` / `./gradlew`), Gradle 9.7.0 скачается автоматически.
* **IDE**: рекомендуется IntelliJ IDEA; импортируй как Gradle-проект и дай Loom сгенерировать исходники.
* MC **26.2** с Fabric — единственный вариант загрузчика, без мультиверсионных веток.

```
gradlew build          # полная сборка → build/libs/starredheltix-<вер>.jar
gradlew runClient      # дев-клиент (смотри лог на ошибки миксинов)
gradlew compileKotlin  # быстрая проверка компиляции
```

**Референсные исходники не в гите**: репозиторий полагается на локальную папку `scripts/`, которую игнорирует git:
`scripts/26.2.X SOURCE CODE/` (декомпил Mojang — источник правды по API) и
`scripts/SOURCES/` (Odin, Firmament, Skysoft, ctjs, MoulConfig, NoFrills, Skyblocker, SkyCubed, Rhino, caffeine, fabric-api, heltix-mod, CustomScoreboard, zen).
Дамп декомпила нужно попросить у мейнтейнера. Никогда не пиши API MC 26.x «по памяти» от 1.21.x —
сигнатуры сильно изменились (extract/submit-модель рендера, `GuiGraphicsExtractor`, `Identifier`...).

## Структура проекта

```
src/main/java/set/starlev/
  injections/accessors/   интерфейсы @Accessor/@Invoker
  injections/mixins/      все клиентские миксины (плоский пакет)
  duck/                   duck-интерфейсы, реализуемые через миксины
src/main/kotlin/set/starlev/
  StarredHeltix.kt        точка входа — ВСЕ фичи инициализируются здесь (порядок важен!)
  commands/               /sh, /starredheltix (ConfigCommand)
  config/                 ConfigManager, Features (@Expose корень), categories/, highlight/
  events/                 EventBus, WorldChangeHandler, KeyBindHandler, ContainerEventWire...
  features/<домен>/       chat, combat(+dungeons,slayer), enchanting, farming(+garden),
                          fishing, foraging, mining, skyblock, visual
  registry/               EntityRegistry (клиентские сущности)
  render/                 RenderEngine, RenderContext, HudManager, HudElement, редакторы, GuiCompat
  scripting/              движок Rhino, загрузчик модулей, api/ (классы JS*)
  secret/                 пасхалки (не трогай без просьбы)
  utils/                  detectors/, CacheManager, ColorUtils, ModUpdater, DiscordRPC, QrCodeUtils...
src/main/resources/
  fabric.mod.json, starredheltix.client.mixins.json, starredheltix.accesswidener, assets/
docs/                     scripting-api.md (для игроков), scripting-api-AI.md (для ИИ), index.html
modules/                  JS-модули для каталога на GitHub (по папке на модуль)
scripts/RULES.md          АРХИТЕКТУРНАЯ БИБЛИЯ — прочитай до правок кода мода
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

## Стиль кода и соглашения

* Kotlin — для фич и логики, Java — только для миксинов, accessor'ов и duck'ов.
* Каждый новый `.java`: заголовок **LGPL-3.0-only** + javadoc с `@author` и `@reason` на миксинах.
* Логи и пользовательский текст — **по-русски**, технические доки и комментарии — по-английски;
  для логов префикс `[StarredHeltix]`.
* Никогда не создавай `ItemStack`/`Block`-синглтоны в `<clinit>` — используй `by lazy`.
* Горячие пути: early-return по `if (!enabled) return`; события вместо опроса в `onTick`;
  кешируй с временными метками и сбрасывай кеши в `WorldChangeHandler`.
* Клиентские фейковые сущности: `entity.setId(...)` **до** `level.addEntity(...)`.

## Миксины и API 26.2

* Официальные маппинги Mojang, без Parchment. `accessWidener v2 official`.
* В `starredheltix.client.mixins.json` стоит `defaultRequire = 1` — неверный дескриптор
  **крашит клиент на старте**. Всегда сверяй сигнатуры и дескрипторы с
  `scripts/26.2.X SOURCE CODE/` перед написанием инъекции.
* Рендер использует extract/submit-модель (`GuiGraphicsExtractor`, `SubmitNodeCollector`,
  `LevelRenderEvents.COLLECT_SUBMITS`); 2D-трансформации — `Matrix3x2fStack` через `graphics.pose()`;
  для текста используй расширения `set.starlev.render.drawString` вместо сырых вызовов.
* После любого бампа версии MC: `gradlew runClient` и grep лога на
  `InvalidInjectionException` / `Invalid descriptor`.

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

## Чеклист перед PR

- [ ] `gradlew build` проходит
- [ ] `gradlew runClient` стартует чисто (без ошибок миксинов и регистров)
- [ ] Фича зарегистрирована в `StarredHeltix.kt`, есть `@Expose` в конфиге, HUD уважает `enabled`
- [ ] Нет повторных вызовов `init()`, нет `ItemStack` в `<clinit>`
- [ ] `/sh` открывается, `/sh reset config` работает, контейнеры/экраны не крашат игру
- [ ] Новый JS API задокументирован (оба дока + раздел в RULES)
- [ ] Бамп версии (если релиз): `gradle.properties`, `fabric.mod.json`, fallback'и `orElse(...)`, `docs/index.html`
- [ ] Комментарии в коммитах и пользовательских фичах — по-русски; код и доки — по-английски

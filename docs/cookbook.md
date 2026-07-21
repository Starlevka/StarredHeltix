# Cookbook — практические рецепты

Пошаговые инструкции «как добавить X» в моде StarredHeltix.
Если что-то не нашлось здесь — задай вопрос в чате, потом добавим рецепт в этот файл.

---

## Как добавить новую фичу

1. **Создай файл** в `src/main/kotlin/set/starlev/features/<domain>/<FeatureName>.kt`:

   ```kotlin
   package set.starlev.features.<domain>

   import set.starlev.StarredHeltix
   // импорты детекторов, render, hud — по необходимости

   /**
    * Краткое описание фичи на русском.
    */
   object <FeatureName> {
       fun init() {
           // Подписки на детекторы, регистрация render-callback'ов, ключей и т.д.
       }
   }
   ```

2. **Добавь запись** в [`src/main/kotlin/set/starlev/features/FeatureBootstrap.kt`](../src/main/kotlin/set/starlev/features/FeatureBootstrap.kt):

   ```kotlin
   "<FeatureName>" to set.starlev.features.<domain>.<FeatureName>::init,
   ```

   Порядок: детекторы раньше фич, которые на них подписываются.

3. **Добавь категорию в конфиг** (если нужны настройки):
   - Создай файл `src/main/kotlin/set/starlev/config/categories/<Domain>Config.kt`
   - Зарегистрируй поле в `Features.kt` (`config/categories/Features.kt`)
   - Подпишись на изменение конфига, если фича должна включаться/выключаться без рестарта

4. **Проверь** `./gradlew build` — сборка должна пройти без ошибок.

5. **Добавь запись** в [`docs/features.md`](features.md).

---

## Как добавить новый детектор

1. **Создай файл** `src/main/kotlin/set/starlev/utils/detectors/<X>Detector.kt`:

   ```kotlin
   package set.starlev.utils.detectors

   import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
   // или ClientReceiveMessageEvents и т.д.

   object <X>Detector {
       private val listeners = mutableListOf<(Fact) -> Unit>()

       fun init() {
           ClientTickEvents.END_CLIENT_TICK.register { tick() }
           // или подписка на пакеты, чат и т.д.
       }

       fun registerListener(listener: (Fact) -> Unit) {
           listeners.add(listener)
       }

       private fun tick() {
           if (somethingHappened()) {
               listeners.forEach { it(fact) }
           }
       }
   }
   ```

2. **Добавь в FeatureBootstrap** (см. рецепт выше).

3. **Подпишись из фичи**:

   ```kotlin
   <X>Detector.registerListener { fact -> /* реакция */ }
   ```

4. **Добавь запись** в `docs/features.md` в раздел «Детекторы».

---

## Как добавить новый HUD-элемент

1. **Создай файл** `src/main/kotlin/set/starlev/features/<domain>/<HudName>.kt`:

   ```kotlin
   package set.starlev.features.<domain>

   import set.starlev.hud.HudElement
   import set.starlev.StarredHeltix

   object <HudName> : HudElement("<HudName>") {
       override fun render() {
           val config = StarredHeltix.feature.<domain>.<hudName>
           if (!config.enabled) return

           val text = computeText()
           drawText(text)
       }
   }
   ```

2. **Добавь в FeatureBootstrap**.

3. **Добавь настройки** в категорию конфига (как минимум `enabled`, `x`, `y`).

4. **Проверь** редактор HUD (`/sh hud editor`) — элемент должен появиться в списке.

---

## Как добавить новую категорию конфига (MoulConfig)

1. **Создай файл** `src/main/kotlin/set/starlev/config/categories/<Domain>Config.kt`:

   ```kotlin
   package set.starlev.config.categories

   import com.google.gson.annotations.Expose
   import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

   class <Domain>Config {
       @Expose
       @ConfigOption(name = "Название", desc = "Описание")
       var mySetting = false
   }
   ```

2. **Зарегистрируй** в `Features.kt`:

   ```kotlin
   @Expose var <domain> = <Domain>Config()
   ```

3. **Категоризация в UI** — через `@Category` или `@Accordion` (см. существующие конфиги как образец).

---

## Как добавить новый миксин (Java)

1. **Создай файл** `src/main/java/set/starlev/injections/mixin/<area>/<Class>Mixin.java`:

   ```java
   package set.starlev.injections.mixin.<area>;

   import net.minecraft.<...>;
   import org.spongepowered.asm.mixin.Mixin;
   import org.spongepowered.asm.mixin.injection.At;
   import org.spongepowered.asm.mixin.injection.Inject;
   import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

   @Mixin(<TargetClass>.class)
   public class <Class>Mixin {
       @Inject(method = "<method>", at = @At("HEAD"))
       private void on<Method>(CallbackInfo ci) {
           // ...
       }
   }
   ```

2. **Зарегистрируй** в `src/main/resources/starredheltix.client.mixins.json` (добавь в массив `"mixins"` или в соответствующюю подсекцию).

3. **Если нужен доступ к приватному полю** — добавь `@Accessor` или `@Invoker` в `injections/accessors/`, а путь к приватному члену пропиши в `starredheltix.accesswidener`.

---

## Как добавить новую пати-команду (`!cmd`)

1. Открой `features/chat/PartyCommands.kt`.
2. Найди `init()` и точку регистрации команд (чаще всего через `CommandRegistry`).
3. Добавь алиасы команды (рус + англ).
4. Логика — отдельная функция, чтобы `init()` оставался коротким.

---

## Как добавить новый решатель (solver) в подземельях

1. Создай `features/combat/dungeons/solvers/<PuzzleName>.kt` (см. `TicTacToe.kt` как образец).
2. Чистая логика выносится в `utils/<puzzle>/<PuzzleUtils>.kt` — она покрывается юнит-тестами.
3. Объект-фича делает только ввод (детекция через `DungeonDetector`) + вывод (`RenderEvents.register { ... }`).
4. Добавь настройку `enabled` в `DungeonsConfig.kt`.

---

## Полезные проверки перед PR

- [ ] `./gradlew build` проходит без ошибок и предупреждений.
- [ ] Если добавил фичу — она в `FeatureBootstrap.kt` и в `docs/features.md`.
- [ ] Если добавил детектор — он в `FeatureBootstrap.kt` и в `docs/features.md`.
- [ ] Если добавил категорию конфига — `config reset` корректно её сбрасывает.
- [ ] Новые ключи конфига имеют `@Expose` (иначе не сериализуются).
- [ ] HUD-элементы корректно отключаются по флагу `enabled`.
- [ ] Логи только на русском, префикс `[StarredHeltix]`.
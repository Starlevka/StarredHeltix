# FastBoot: Stop Waiting, Start Playing 🚀

FastBoot is a lightweight utility mod that absolutely ruthlessly cuts down Minecraft's startup time by ripping out artificial delays and unoptimized synchronous garbage.

## Features

- **Zero Fades.** We completely killed the hardcoded 2-second fade-in animations. The splash screen and main menu buttons now appear the absolute millisecond they are ready. No smooth transitions, just instant access.
- **Lazy DFU (DataFixerUpper).** Vanilla Minecraft spends seconds on startup compiling 4,000+ upgrade schemas just in case you decide to load a world from 2018. FastBoot defers this process entirely. It will only generate these schemas if you *actually* try to load an old save.
- **Texture Stitching Rescue.** Fixes a massive bottleneck where the game would freeze for ages trying to compile texture atlases on the GPU.

Drop it into your `mods` folder and watch your massive modpacks launch like a bullet.

---

### Русское Описание (Russian Description)

**FastBoot: Хватит ждать, пора играть**

FastBoot — это легкий мод, который безжалостно режет время запуска Майнкрафта, выкидывая на помойку всё, что искусственно тормозит старт игры.

**Что он делает:**
- **К чёрту загрузочные анимации.** Мы вырезали зашитые в код 2-секундные плавные появления меню. Загрузочный экран и кнопки главного меню теперь появляются в ту же миллисекунду, как только загрузятся файлы. Никаких красивых угасаний — только мгновенный доступ.
- **Оптимизация DFU.** Ванилла тратит кучу времени на старте, чтобы скомпилировать больше 4,000 схем конвертации старых миров (DataFixerUpper). FastBoot заставляет движок лениться: игра сделает это только в том случае, если вы *реально* попытаетесь зайти в старый мир. Минус несколько секунд мертвого зависания обеспечено.
- **Сборка Текстур.** Мод так же лечит процесс текстурных атласов (когда игра надолго зависает на этапе Texture Stitching). 

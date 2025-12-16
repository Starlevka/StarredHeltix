# Сборка StarredHeltix мода

## Требования

- **Java 21+** - [Скачать](https://www.oracle.com/java/technologies/downloads/#java21)
- **Git** - [Скачать](https://git-scm.com/download/win)

## Способ 1: Восстановление Gradle (рекомендуется)

Если gradle wrapper был удален, выполни:

```bash
# В папке проекта:
./gradlew build
```

Если это не работает, скачай gradle-wrapper.jar отсюда:
https://services.gradle.org/distributions/gradle-8.11.1-bin.zip

И распакуй в `gradle/wrapper/`

## Способ 2: Установка локального Gradle

1. [Скачай Gradle 8.11.1](https://gradle.org/releases/)
2. Добавь в PATH переменную окружения
3. Выполни в проекте:

```bash
gradle build
```

## Сборка мода

После установки gradle:

```bash
# Полная сборка мода
gradle build

# Только сборка JAR файла
gradle jar

# Очистка кеша
gradle clean

# Сборка + очистка
gradle clean build
```

Готовый мод будет в: `build/libs/starredheltix-0.0.10.jar`

## Версия мода

- **Версия:** 0.0.10
- **Minecraft:** 1.21.10
- **Fabric Loader:** 0.18.2+
- **Java:** 21+

## Если есть ошибки:

1. Проверь версию Java: `java -version`
2. Очисти gradle кеш: `gradle clean`
3. Пересоберись: `gradle build`
4. Если всё ещё не работает - переустанови Gradle

## Установка мода

После сборки скопируй `starredheltix-0.0.10.jar` в папку:
```
.minecraft/mods/
```

Перезагрузись в Minecraft с профилем Fabric!

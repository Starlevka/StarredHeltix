# StarredHeltix

1.21.4 до версии 0.0.5

1.21.10 версия 0.0.5 и выше

Л мод для Heltix SkyBlock с крутыми фишками для комфортной игры

## Основные возможности
🎮 **QOL фишки**
- Команды пати: !ping, !fps, !coords, !time, !uptime, !dt, !rng, !boykisser и другие
- Блокировка слотов: Защита от случайного выбрасывания предметов (L для режима блокировки)
- Автоспринт: Автоматический спринт при движении
- Уведомления: Полный инвентарь, рыбалка с звуковыми сигналами
- Подсветка мобов: Выделение эндерменов и волков
- Вейпоинты: Автоматическое создание вейпоинтов из координат в чате
- Блокировка Title: Скрытие супер-редких сообщений (временно не работает)

⚡ **Таймеры**
- Таймер Древоточеца: Визуализация кулдауна с настройкой процента уменьшения
- Таймеры способностей: Киркобулус и Увеличение скорости копания из HOTM
- Таймер кровавой комнаты: Отсчет времени в подземельях

🧩 **Решатели головоломок**
- Three Weirdos: Автоматическое решение загадки трех незнакомцев

🔧 **Утилиты**
- Автообновление: Проверка и установка обновлений с GitHub
- Кастомные бинды: Настраиваемые горячие клавиши с поддержкой всех GLFW_KEY
- Напоминание о голосовании: Ежедневные уведомления
- Автовход: Команда /вход для быстрой авторизации
- Фильтры сообщений: Скрытие сообщений по префиксам

## Команды
### Основные
- `/starredheltix` - Главное меню настроек
- `/starredheltix toggle` - Включить/выключить мод
- `/starredheltix update` - Проверить обновления
- `/starredheltix reloadconfig` - Перезагрузить конфигурацию
- `/вход` - Автоматический ввод пароля
- `/яготовлёвал` - Отправить готовность в пати чат
- `/d` или `/в` - Быстрая команда /dh

### Настройки функций
- `/starredheltix treecap` - Настройки таймера Древоточеца
- `/starredheltix treecap toggle` - Включить/выключить таймер
- `/starredheltix treecap percentage <процент>` - Установить процент уменьшения
- `/starredheltix abilitycooldown` - Настройки таймеров способностей
- `/starredheltix abilitycooldown toggle` - Включить/выключить таймеры
- `/starredheltix abilitycooldown kirkobulus` - Переключить Киркобулус
- `/starredheltix abilitycooldown miningspeedboost` - Переключить Увеличение скорости копания
- `/starredheltix threeweirdos` - Переключить решатель Three Weirdos
- `/starredheltix slotlocking` - Переключить блокировку слотов
- `/starredheltix inventorywarning` - Переключить предупреждение инвентаря
- `/starredheltix fishingnotification` - Переключить уведомления рыбалки
- `/starredheltix autosprint` - Переключить автоспринт
- `/starredheltix bloodroom` - Переключить таймер кровавой комнаты
- `/starredheltix partycommands` - Переключить команды пати
- `/starredheltix endermanhighlighter` - Переключить подсветку эндерменов
- `/starredheltix wolfhighlighter` - Переключить подсветку волков
- `/starredheltix waypoints` - Переключить вейпоинты
- `/starredheltix titleblocking` - Переключить блокировку Title
- `/starredheltix autopartychat` - Переключить авто пати чат

### Фильтры сообщений
- `/starredheltix filter add <префикс>` - Добавить фильтр
- `/starredheltix filter remove <id>` - Удалить фильтр
- `/starredheltix filter list` - Показать все фильтры
- `/starredheltix filter clear` - Очистить все фильтры

### Конфигурация
- `/starredheltix config password <пароль>` - Установить пароль для автовхода
- `/starredheltix config readyphrase <фраза>` - Установить фразу готовности

### Кастомные бинды
- `/starredheltix binds create <имя> <команда>` - Создать бинд
- `/starredheltix binds delete <имя>` - Удалить бинд
- `/starredheltix binds list` - Показать все бинды
- `/starredheltix binds setkey <имя> <клавиша>` - Назначить клавишу (ESC для сброса)

### Голосование
- `/starredheltix voting toggle` - Переключить напоминания
- `/starredheltix voting dayreset` - Сбросить день голосования

### Обновления
- `/starredheltix update` - Проверить обновления
- `/starredheltix update open` - Открыть страницу проекта
- `/starredheltix update install` - Установить обновление

## Установка
1. Установите Fabric Loader для Minecraft 1.21.10
2. Скачайте последний релиз StarredHeltix
3. Поместите файл мода в папку `.minecraft/mods`
4. Запустите Minecraft с профилем Fabric

## Системные требования
- Minecraft 1.21.10
- Fabric Loader 0.17.3+
- Fabric API
- Java 21+

## Лицензия
Этот проект лицензирован по лицензии MIT - см. файл LICENSE для получения подробной информации.

**Версия:** 0.0.10
**Совместимость:** Minecraft 1.21.10 + Fabric
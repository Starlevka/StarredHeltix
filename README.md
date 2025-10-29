# StarredHeltix

Л мод для Heltix SkyBlock с крутыми фишками для **комфотрной игры**

## Основные возможности

### 🎮 Игровые функции
- **Команды пати**: !ping, !fps, !coords, !time, !uptime, !dt, !rng и другие
- **Блокировка слотов**: Защита от случайного выбрасывания предметов (L для режима блокировки)
- **Автоспринт**: Автоматический спринт при движении
- **Уведомления**: Полный инвентарь, рыбалка с звуковыми сигналами

### ⚡ Таймеры и визуализация
- **Таймер Древоточеца**: Визуализация кулдауна с настройкой процента уменьшения
- **Таймеры способностей**: Киркобулус и Увеличение скорости копания из HOTM
- **Таймер кровавой комнаты**: Отсчет времени в подземельях

### 🧩 Решатели головоломок
- **Three Weirdos**: Автоматическое решение загадки трех незнакомцев
- **Экспериментальный стол**: Помощь в решении экспериментов

### 🔧 Утилиты
- **Автообновление**: Проверка и установка обновлений с GitHub
- **Кастомные бинды**: Настраиваемые горячие клавиши
- **Напоминание о голосовании**: Ежедневные уведомления
- **Автовход**: Команда /вход для быстрой авторизации

## Команды

### Основные
- `/starredheltix` - Главное меню настроек
- `/starredheltix toggle` - Включить/выключить мод
- `/вход` - Автоматический ввод пароля
- `/яготовлёвал` - Отправить готовность в пати чат

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

### Фильтры сообщений
- `/starredheltix filter add <префикс>` - Добавить фильтр
- `/starredheltix filter remove <id>` - Удалить фильтр
- `/starredheltix filter list` - Показать все фильтры
- `/starredheltix filter clear` - Очистить все фильтры

### Конфигурация
- `/starredheltix config password <пароль>` - Установить пароль для автовхода
- `/starredheltix config readyphrase <фраза>` - Установить фразу готовности
- `/starredheltix reloadconfig` - Перезагрузить конфигурацию

### Кастомные бинды
- `/starredheltix binds create <имя> <команда>` - Создать бинд
- `/starredheltix binds delete <имя>` - Удалить бинд
- `/starredheltix binds list` - Показать все бинды
- `/starredheltix binds setkey <имя> <клавиша>` - Назначить клавишу

### Голосование
- `/starredheltix voting toggle` - Переключить напоминания
- `/starredheltix voting dayreset` - Сбросить день голосования

### Обновления
- `/starredheltix update` - Проверить обновления
- `/starredheltix update open` - Открыть страницу проекта
- `/starredheltix update install` - Установить обновление

### Отладка
- `/starredheltix debug` - Показать отладочную информацию
- `/starredheltix debug toggle` - Переключить режим отладки

## Установка

1. Установите [Fabric Loader](https://fabricmc.net/use/) для Minecraft 1.21.10
2. Скачайте последний [релиз StarredHeltix](https://github.com/Starlevka/StarredHeltix/releases)
3. Поместите файл мода в папку `.minecraft/mods`
4. Запустите Minecraft с профилем Fabric

## Системные требования

- Minecraft 1.21.10
- Fabric Loader 0.17.3+
- Fabric API
- Java 21+

## Лицензия

Этот проект лицензирован по лицензии MIT - см. файл [LICENSE](LICENSE) для получения подробной информации.

---

*Версия: 0.0.6*  
*Совместимость: Minecraft 1.21.10 + Fabric*
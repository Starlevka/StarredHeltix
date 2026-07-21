# Реестр фич StarredHeltix

Список всех реализованных фич мода с указанием файлов.
Если фича в реестре, но не упомянута здесь — добавь запись, чтобы документация не отставала от кода.

> Версия реестра: v0.1.1. Файлы указаны относительно `src/main/kotlin/set/starlev/`.

---

## 🎮 Инвентарь

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Кастомные кнопки в инвентаре | `features/inventory/InventoryButton.kt` | Кнопки, которые пользователь сам добавляет в GUI инвентаря |
| GUI управления кнопками | `features/inventory/InventoryButtonsGui.kt` | Экран настройки кнопок |
| Оверлей экипировки | `features/inventory/EquipmentOverlay.kt` | Подсветка надетой экипировки |
| Менеджер кнопок | `features/inventory/InventoryButtonsManager.kt` | Регистрация/удаление кнопок |

---

## ⛏️ Фермерство

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| HUD скорости Rancher'а | `features/farming/RancherSpeedHud.kt` | Текущая скорость Rancher Boots |
| Подсветка грибов | `features/farming/GlowingMushroomHighlight.kt` | Glow на светящихся грибах |
| HUD угла мотыги | `features/farming/HoeAngleHud.kt` | Подсказка угла мотыги для сбора урожая |
| HUD Crop Milestone | `features/farming/garden/CropMilestoneHud.kt` | Прогресс урожая из таба |
| HUD плотов | `features/farming/garden/PlotsHud.kt` | Состояние плотов Garden |

---

## 🏰 Подземелья

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Решатель F4 (лазеры) | `features/combat/dungeons/Floor4.kt` | Решатель головоломки с лазерами на 4 этаже |
| Решатель «Три незнакомца» | `features/combat/dungeons/solvers/ThreeWeirdos.kt` | Определяет правильного NPC по фразам |
| Подсветка сундука | `features/combat/dungeons/solvers/ThreeWeirdosChest.kt` | Подсвечивает правильный сундук |
| Крестики-нолики | `features/combat/dungeons/solvers/TicTacToe.kt` | Подсветка лучшего хода |
| Утилиты TicTacToe | `utils/tictactoe/TicTacToeUtils.kt` | Минимакс + предотвращение вилок |
| Таймер кровавой комнаты | `features/combat/dungeons/BloodRoomTimer.kt` | Таймер бонусной комнаты |
| Счётчик смертей | `features/combat/dungeons/DeathCounter.kt` | Считает смерти в данже |
| Счётчик очков | `features/combat/dungeons/ScoreCounter.kt` | 270+ / S+ подсчёт |
| Подсветка мини-боссов | `features/combat/dungeons/MiniBossHighlight.kt` | Потерянный путешественник, Теневой убийца и т.д. |
| Решатель «Криперы-лучи» | `features/combat/dungeons/solvers/CreeperBeams.kt` | Цветные лучи криперов |

---

## ⚔️ Бой

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Подсветка мобов | `features/combat/EntityHighlight.kt` | Эндермены, пауки, зомби, криперы, волки |
| Подсветка эндерменов отдельно | `features/combat/EndHighlight.kt` | Расширенные настройки для эндерменов |
| Slayer HUD | `features/combat/slayer/SlayerHud.kt` | HUD текущего квеста Slayer |

---

## 🌲 Лесорубство

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Таймер TreeCap | `features/foraging/TreeCapCooldown.kt` | Кулдаун Jungle Axe / Treecapitator |

---

## ⛏️ Шахтёрство

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Авто-комиссии | `features/mining/AutoCommissions.kt` | Автоматически принимает комиссии |
| HUD комиссий | `features/mining/CommissionsHud.kt` | Текущие комиссии |
| Dwarven Waypoints | `features/mining/DwarvenWaypoints.kt` | Вейпоинты локаций Гномьих шахт |
| Pickaxe Cooldown HUD | `features/mining/abilities/Kirkobulus.kt` | Кулдаун кирки |
| Speed Boost Cooldown HUD | `features/mining/abilities/MiningSpeed.kt` | Mining Speed Boost кулдаун |

---

## 🎣 Рыбалка

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Legendary Fishing Notifier | `features/fishing/MonsterNotifier.kt` | Уведомление о поклёвке и монстрах |

---

## 🔮 Зачарование

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Experiment Solver Manager | `features/enchanting/ExperimentSolverManager.kt` | Решатель Experimentation Table |

---

## 📊 Skyblock

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| SkillHUD | `features/skyblock/SkillXp.kt` | XP в текущем навыке |
| PetOverlay | `features/skyblock/PetOverlay.kt` | Подсветка активного пета |
| Кастомный Scoreboard | `features/skyblock/HudScoreboard.kt`, `features/skyblock/scoreboard/elements/*` | Bank, BPS, Cookie, CPS, FPS, Gems, Location, Ping, Purse, Slayer, Title |
| Museum Helper | `features/skyblock/Museum.kt` | Подсказки для сдачи экспонатов |

---

## 👁️ Визуальные

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Fullbright | `features/visual/Fullbright.kt` | Максимальная яркость без шейдеров |
| Mega NPC сундуки | `features/visual/MegaChestNPC.kt` | Увеличенные сундуки для NPC-квестов |
| Диалоги NPC | `features/overlays/Dialogues.kt` | Подсветка активных реплик |
| Ghost Frame / Frame | `features/visual/Frame.kt` | Подсветка пустых рамок |
| NPC рендерер | `features/visual/NPC.kt` | Логика спавна NPC-сущностей |

---

## 📈 Статистика

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| StatsTracker | `features/misc/info/*` | CPS/BPS и др. метрики |
| InfoHuds | `features/misc/info/*` | HUD для отображения CPS/BPS |

---

## 🔧 Misc

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| AutoSprint | (в `features/misc/`) | Автоспринт |
| Inventory History Log | `features/misc/InventoryHistoryLog.kt` | История изменений инвентаря |
| Waypoints | `features/misc/Waypoints.kt` | Создание вейпоинтов |
| Mouse Lock | `features/misc/MouseLock.kt` | Блокировка мыши в окне |
| Welcome Message | `features/misc/Welcome.kt` | Приветствие при заходе |
| Авто-поручения (King) | (в `features/misc/`) | Royal Pigeon авто |
| `/вход` команда | (в `features/misc/`) | Быстрый вход с паролем |
| ModUpdater | (в `features/misc/`) | Автообновление мода |
| ModMenu интеграция | `utils/ModMenuIntegration.kt` | Точка входа в ModMenu |
| DevToolsLogger | `utils/DevToolsLogger.kt` | Отладочный вывод в чат |

---

## 🎨 HUD система

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| HUD Editor | `hud/HudEditorScreen.kt` | Визуальный редактор позиций |
| HUD Manager | `hud/HudManager.kt` | Реестр и рендер HUD-элементов |
| HUD Element | `hud/HudElement.kt` | Базовый класс |

---

## 🔍 Детекторы (`utils/detectors`)

| Детектор | Файл | Что отслеживает |
|----------|------|-----------------|
| LocationDetector | `LocationDetector.kt` | Локация игрока на сервере |
| TimeDetector | `TimeDetector.kt` | Игровое/реальное время |
| MobHeadDisplayDetector | `MobHeadDisplayDetector.kt` | Голова моба в инвентаре |
| EnchantmentProgressDetector | `EnchantmentProgressDetector.kt` | Прогресс зачарования |
| ActionBarDetector | `ActionBarDetector.kt` | Строка action bar |
| BiomeDetector | `BiomeDetector.kt` | Текущий биом |
| ChatDetector | `ChatDetector.kt` | Чат-сообщения (детекция паттернов) |
| ContainerDetector | `ContainerDetector.kt` | Открытые контейнеры/меню |
| DungeonDetector | `DungeonDetector.kt` | Находимся ли в данже |
| EntityDeathDetector | `EntityDeathDetector.kt` | Смерти сущностей |
| HeldItemDetector | `HeldItemDetector.kt` | Предмет в руке |
| ItemLoreDetector | `ItemLoreDetector.kt` | Лор предмета |
| MuseumDetector | `MuseumDetector.kt` | В Museum ли игрок |
| PlayerHeldItemDetector | `PlayerHeldItemDetector.kt` | Предмет в руке игрока |
| ScoreboardDetector | `ScoreboardDetector.kt` | Текущий sidebar scoreboard |
| SkillXpDetector | `SkillXpDetector.kt` | XP текущего навыка |
| TabListDetector | `TabListDetector.kt` | Содержимое таба |
| TitleDetector | `TitleDetector.kt` | Title-сообщения |

---

## 💬 Чат и команды

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| Пати-команды | `features/chat/PartyCommands.kt` | `!promote`, `!kick`, `!invite`, `!ping`, `!fps`, `!time`, `!coords`, `!rng`, `!dt`, `!boykisser` |
| Фильтрация сообщений | (в `features/chat/`) | Скрытие шума |
| Кастомные бинды | `features/chat/CustomBind.kt`, `CustomBindManager.kt` | Привязка команд к клавишам |
| Макро-проверка | `features/chat/mod/MacroCheck.kt` | Anti-macro в чате |
| Команда `/starredheltix` | `commands/ConfigCommand.kt` | Все основные команды мода |

---

## 🔐 Секретные функции (без ИИ)

| Фича | Файл(ы) | Назначение |
|------|---------|-----------|
| MeowMusicRune | `secret/features/MeowMusicRune.kt` | Музыкальная руна |
| Povtorayshkins | `secret/features/Povtorayshkins.kt` | Повторяшки |
| SecretFunFeatures | `secret/features/SecretFunFeatures.kt` | Базовые секретные функции |
| Secret Config | `secret/config/SecretConfig.kt`, `secret/config/SecretMenuManager.kt` | Конфиг и меню секретных функций |

---

## 🛡️ Оптимизация

| Миксин | Папка | Что оптимизирует |
|--------|-------|------------------|
| Chunk Optimization | `injections/mixin/optimization/` | Чанки |
| FastBoot | `injections/mixin/optimization/` | Запуск клиента |
| Vec3f Interner | `injections/mixin/optimization/` | Память на Vec3f |
| DFU | `injections/mixin/lazydfu/` | Сериализация NBT |
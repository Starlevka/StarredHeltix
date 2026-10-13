package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.core.BlockPos

/**
 * JS API: Объект `client` — состояние клиента, меню, сервер, FPS/пинг.
 *
 * Поддержка входа на сервер из модуля (главное меню → сетевая игра),
 * статистика соединения и окна.
 */
class JSClient {

    private val mc: Minecraft get() = Minecraft.getInstance()

    // ==================== Игрок и мир (быстрые проверки) ====================

    /** Есть ли загруженный мир/сервер. */
    fun isInWorld(): Boolean = mc.level != null && mc.player != null

    /** Одиночный мир (интегрированный сервер). */
    fun isSingleplayer(): Boolean = mc.isLocalServer

    // ==================== Подключение к серверу ====================

    /**
     * Подключиться к серверу (как через меню "Сетевая игра").
     * client.connect("heltix.net") или client.connect("heltix.net", 25565).
     */
    fun connect(ip: String, port: Int = 25565) {
        mc.execute {
            if (ip.isBlank()) return@execute
            val address = if (ip.contains(":")) {
                ServerAddress.parseString(ip)
            } else {
                ServerAddress(ip, port)
            }
            ConnectScreen.startConnecting(
                JoinMultiplayerScreen(TitleScreen()),
                mc,
                address,
                ServerData("Server", ip, ServerData.Type.OTHER),
                false,
                null
            )
        }
    }

    /** Отключиться от сервера и вернуться в меню (кнопка "Отключиться"). */
    fun disconnect() {
        mc.execute {
            mc.disconnect(TitleScreen(), true)
        }
    }

    /** IP текущего сервера ("localhost" в одиночке, "" — вне мира). */
    fun getServerIP(): String {
        if (isSingleplayer()) return "localhost"
        return mc.currentServer?.ip ?: ""
    }

    /** Название сервера из списка серверов ("" — вне мира). */
    fun getServerName(): String = if (isSingleplayer()) "SinglePlayer" else mc.currentServer?.name ?: ""

    /** MOTD сервера ("" — вне мира). */
    fun getServerMOTD(): String =
        if (isSingleplayer()) "SinglePlayer" else mc.currentServer?.motd?.string ?: ""

    /**
     * Пинг до сервера в мс (из tab-листа). 5 — одиночка, -1 — вне мира.
     */
    fun getPing(): Long {
        if (isSingleplayer()) return 5L
        val p = mc.player ?: return -1L
        val conn = mc.connection ?: return -1L
        val ping = conn.getPlayerInfo(p.uuid)?.latency?.toLong() ?: mc.currentServer?.ping ?: -1L
        if (ping >= 0) samplePing(ping.toInt())
        return ping
    }

    // ==================== Статистика пинга (скользящее окно ~60 сек) ====================

    private val pingHistory = ArrayDeque<Int>()
    private var lastPingSampleMs = 0L

    /** Добавить замер в статистику (не чаще 1 раза в секунду). */
    private fun samplePing(ping: Int) {
        val now = System.currentTimeMillis()
        if (now - lastPingSampleMs < 1000) return
        lastPingSampleMs = now
        synchronized(pingHistory) {
            pingHistory.addLast(ping)
            while (pingHistory.size > 60) pingHistory.removeFirst()
        }
    }

    /**
     * Статистика пинга за последние ~60 секунд:
     * {current, avg, min, max, median, last, count} — или -1 вне мира.
     */
    fun getPingStats(): Map<String, Any> {
        val current = getPing()
        synchronized(pingHistory) {
            if (pingHistory.isEmpty()) {
                return mapOf(
                    "current" to current, "avg" to -1, "min" to -1,
                    "max" to -1, "median" to -1, "last" to -1, "count" to 0
                )
            }
            val sorted = pingHistory.sorted()
            val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2]
                else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            return mapOf(
                "current" to current,
                "avg" to pingHistory.sum() / pingHistory.size,
                "min" to sorted.first(),
                "max" to sorted.last(),
                "median" to median,
                "last" to sorted.last(),
                "count" to pingHistory.size
            )
        }
    }

    /** Пинги всех игроков из таб-листа: {ник: мс}. */
    fun getPlayerPings(): Map<String, Int> {
        val conn = mc.connection ?: return emptyMap()
        return conn.onlinePlayers.associate { it.profile.name to it.latency }
    }

    /** Очистить накопленную статистику пинга. */
    fun resetPingStats() {
        synchronized(pingHistory) { pingHistory.clear() }
        lastPingSampleMs = 0L
    }

    // ==================== Производительность ====================

    /** Текущий FPS (как в F3). */
    fun getFPS(): Int = mc.fps

    /** Использование памяти JVM в процентах (0-100). */
    fun getMemoryUsage(): Int {
        val rt = Runtime.getRuntime()
        val max = rt.maxMemory().toFloat()
        if (max <= 0) return 0
        return (((rt.totalMemory() - rt.freeMemory()) * 100) / max).toInt()
    }

    /** Максимум памяти JVM в байтах. */
    fun getMaxMemory(): Long = Runtime.getRuntime().maxMemory()

    /** Занято памяти JVM в байтах. */
    fun getUsedMemory(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // ==================== Окно и состояние клиента ====================

    /** Активно ли окно игры (не свёрнуто). */
    fun isWindowActive(): Boolean = mc.isWindowActive

    /** Открыто ли главное меню (титульный экран). */
    fun isInMainMenu(): Boolean = mc.gui.screen() is TitleScreen

    /** Открыто ли меню сетевой игры. */
    fun isInMultiplayerMenu(): Boolean = mc.gui.screen() is JoinMultiplayerScreen

    /** Открыто ли меню подключения (экран "Подключение к серверу"). */
    fun isInConnectScreen(): Boolean = mc.gui.screen() is ConnectScreen

    /** Открыт ли какой-либо экран (GUI). */
    fun isInGui(): Boolean = mc.gui.screen() != null

    /** Имя класса открытого экрана ("TitleScreen", "JoinMultiplayerScreen"... или "null"). */
    fun getCurrentScreenName(): String = mc.gui.screen()?.javaClass?.simpleName ?: "null"

    /** Закрыть текущий экран (если открыт). */
    fun closeScreen() {
        mc.execute { mc.gui.screen()?.onClose() }
    }

    /** Версия игры (launchedVersion). */
    fun getGameVersion(): String = mc.launchedVersion

    /** Позиция мыши X (в координатах GUI). */
    fun getMouseX(): Double {
        val window = mc.window
        val scaledWidth = window.guiScaledWidth.toDouble()
        val width = window.width.toDouble()
        if (width <= 0) return 0.0
        return mc.mouseHandler.xpos() / width * scaledWidth
    }

    /** Позиция мыши Y (в координатах GUI). */
    fun getMouseY(): Double {
        val window = mc.window
        val scaledHeight = window.guiScaledHeight.toDouble()
        val height = window.height.toDouble()
        if (height <= 0) return 0.0
        return mc.mouseHandler.ypos() / height * scaledHeight
    }

    /** Зажат ли Shift. */
    fun isShiftDown(): Boolean =
        org.lwjgl.glfw.GLFW.glfwGetKey(mc.window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) ==
            org.lwjgl.glfw.GLFW.GLFW_PRESS

    /** Зажат ли Ctrl. */
    fun isControlDown(): Boolean =
        org.lwjgl.glfw.GLFW.glfwGetKey(mc.window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) ==
            org.lwjgl.glfw.GLFW.GLFW_PRESS

    /** Зажат ли Alt. */
    fun isAltDown(): Boolean =
        org.lwjgl.glfw.GLFW.glfwGetKey(mc.window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) ==
            org.lwjgl.glfw.GLFW.GLFW_PRESS

    // ==================== Пакеты (sendPacket, безопасно — по имени) ====================

    /**
     * Отправить пакет серверу по имени и параметрам. Возвращает true, если пакет отправлен.
     *
     * Поддерживаемые пакеты (имя без "Serverbound"/"Packet", регистр не важен):
     *  - "swing" (hand: "main"|"off" — опционально) — махнуть рукой
     *  - "useItem" — использовать предмет (ПКМ)
     *  - "jump" (onGround: Bool) — прыжок/движение (MovePlayer), обычно не нужен
     *  - "playerAction" (action: "startDestroy"|"abortDestroy"|"stopDestroy"|"dropItem"|"dropAllItems"|"releaseUseItem"|"swapItem",
     *    x, y, z, face: "down"|"up"|"north"|"south"|"west"|"east")
     *  - "interact" (entityId, x, y, z, sneak: Bool) — взаимодействие с сущностью
     *  - "playerCommand" / "chat" (message: String) — чат/команда (см. также player.sendCommand)
     *
     * Примеры:
     *  client.sendPacket("swing")
     *  client.sendPacket("playerAction", "dropItem", px, py, pz, "down")
     *
     * Песочница не даёт создавать MC-объекты, поэтому конструкторы спрятаны здесь.
     * Отправленные пакеты попадут в events.onPacketSend как обычные.
     */
    fun sendPacket(name: String, vararg args: Any?): Boolean {
        val conn = mc.connection ?: return false
        val str = { i: Int -> (args.getOrNull(i) as? String) }
        val dbl = { i: Int -> (args.getOrNull(i) as? Number)?.toDouble() }
        val key = name.lowercase().removePrefix("serverbound").removeSuffix("packet").trim()

        val packet: net.minecraft.network.protocol.Packet<*> = when (key) {
            "swing" -> {
                val hand = if (str(0)?.lowercase() == "off")
                    net.minecraft.world.InteractionHand.OFF_HAND else net.minecraft.world.InteractionHand.MAIN_HAND
                net.minecraft.network.protocol.game.ServerboundSwingPacket(hand)
            }
            "useitem", "use" -> {
                val p = mc.player ?: return false
                net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    p.id, p.yRot, p.xRot
                )
            }
            "playeraction", "action" -> {
                val actionName = (str(0) ?: "stopDestroy").lowercase()
                val action = when (actionName) {
                    "startdestroy", "start" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                    "abortdestroy", "abort" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                    "dropitem", "drop" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM
                    "dropallitems", "dropall" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                    "releaseuseitem", "release" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM
                    "swapitem", "swap" -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                    else -> net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                }
                val x = dbl(1)?.toInt() ?: return false
                val y = dbl(2)?.toInt() ?: return false
                val z = dbl(3)?.toInt() ?: return false
                val dir = parseDirection(str(4) ?: "down") ?: return false
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(action, BlockPos(x, y, z), dir)
            }
            "interact", "attack" -> {
                val entityId = dbl(0)?.toInt() ?: return false
                val x = dbl(1) ?: return false
                val y = dbl(2) ?: return false
                val z = dbl(3) ?: return false
                val sneak = (args.getOrNull(4) as? Boolean) ?: false
                net.minecraft.network.protocol.game.ServerboundInteractPacket(
                    entityId,
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    net.minecraft.world.phys.Vec3(x, y, z),
                    sneak
                )
            }
            else -> return false
        }

        mc.execute { conn.send(packet) }
        return true
    }

    private fun parseDirection(name: String?): net.minecraft.core.Direction? = when (name?.lowercase()) {
        "down" -> net.minecraft.core.Direction.DOWN
        "up" -> net.minecraft.core.Direction.UP
        "north" -> net.minecraft.core.Direction.NORTH
        "south" -> net.minecraft.core.Direction.SOUTH
        "west" -> net.minecraft.core.Direction.WEST
        "east" -> net.minecraft.core.Direction.EAST
        else -> null
    }
}

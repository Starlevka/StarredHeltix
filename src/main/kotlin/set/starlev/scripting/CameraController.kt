package set.starlev.scripting

import net.minecraft.client.Minecraft

/**
 * Состояние кастомной камеры (JS API `camera`): камера может быть отцеплена
 * от игрока и управляться из JS — позиция и/или поворот подменяются в
 * CameraMixin.extractRenderState.
 *
 * Пока камера отцеплена, ввод движения игрока (WASD/прыжок/крадясь/спринт)
 * подавляется в KeyboardHandlerMixin — иначе персонаж уйдёт из-под камеры.
 * Опрос клавиш для управления камерой — keybind.isDown (GLFW, напрямую).
 *
 * Это API-уровень, а не конкретная фича: любые модули (freecam, кинематик-камеры,
 * кастомные виды) строятся поверх `camera`.
 */
object CameraController {
    /** Камера отцеплена от игрока (позиция и/или поворот переопределены). */
    @Volatile
    var detached = false
        private set

    /** Модуль, отцепивший камеру (SHScriptEngine.currentModule в момент enable). */
    @Volatile
    var ownerModule: String? = null
        private set

    @Volatile var x = 0.0
    @Volatile var y = 0.0
    @Volatile var z = 0.0

    /** Переопределение поворота (иначе камера наследует поворот игрока). */
    @Volatile var rotOverride = false
    @Volatile var yaw = 0.0f
    @Volatile var pitch = 0.0f

    /**
     * Отцепить камеру от игрока. Позиция берётся с текущей камеры (глаза игрока),
     * поворот остаётся от игрока, пока не задан setRot.
     */
    fun detach(): Boolean {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.level == null) return false
        if (!detached) {
            val pos = mc.gameRenderer.mainCamera().position()
            x = pos.x
            y = pos.y
            z = pos.z
            val player = mc.player!!
            yaw = player.yRot
            pitch = player.xRot
            rotOverride = false
        }
        ownerModule = SHScriptEngine.currentModule
        detached = true
        return true
    }

    /** Вернуть камеру к игроку. */
    fun attach() {
        detached = false
        rotOverride = false
        ownerModule = null
    }

    /** Выгрузка модуля: если камеру отцепил именно он — вернуть на место. */
    fun onModuleUnloaded(moduleName: String) {
        if (detached && ownerModule != null && ownerModule.equals(moduleName, ignoreCase = true)) {
            attach()
        }
    }
}

package set.starlev.render

import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class HudElement(
    val id: String
) {
    var x: Int = 0
    var y: Int = 0
    var scale: Float = 1.0f
    var showBackground: Boolean = true
    var customWidth: Int = 0
    var customHeight: Int = 0
    var isEditing = false
    protected var cachedGraphics: GuiGraphicsExtractor? = null
    private var initialized = false

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 3.0f
        const val SCALE_STEP = 0.1f
    }

    protected fun ensureInitialized() {
        if (!initialized) {
            x = getDefaultX()
            y = getDefaultY()
            scale = getDefaultScale()
            initialized = true
        }
    }

    open fun renderWithGraphics(graphics: GuiGraphicsExtractor) {
        ensureInitialized()
        cachedGraphics = graphics

        // Кламп для рендера (не трогаем сохранённые x/y, чтобы не ломать сейв) — не даёт уйти за экран при смене GUI Scale/разрешения
        val guiW = try { graphics.guiWidth() } catch (_: Exception) { 0 }
        val guiH = try { graphics.guiHeight() } catch (_: Exception) { 0 }
        val sw = getScaledWidth().coerceAtLeast(1)
        val sh = getScaledHeight().coerceAtLeast(1)
        val renderX = if (guiW > sw) x.coerceIn(0, guiW - sw) else x.coerceAtLeast(0)
        val renderY = if (guiH > sh) y.coerceIn(0, guiH - sh) else y.coerceAtLeast(0)

        // Применить масштабирование (MC 26.2: pose() -> Matrix3x2fStack) с клампнутыми координатами
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(renderX.toFloat(), renderY.toFloat())
        pose.scale(scale, scale)
        pose.translate(-renderX.toFloat(), -renderY.toFloat())

        // Временно подменяем x/y для drawBackground/isHovered внутри render() чтобы фон рисовался на клампнутой позиции
        val origX = x
        val origY = y
        x = renderX
        y = renderY
        try {
            render()
        } finally {
            x = origX
            y = origY
            pose.popMatrix()
            cachedGraphics = null
        }
    }

    abstract fun render()
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int

    /**
     * Отрисовать стандартный фон панели
     * @param width Ширина контента
     * @param height Высота контента
     * @param padding Внутренние отступы
     * @param centerAnchor Если true, фон будет центрирован относительно позиции x (используется для однострочных худов)
     */
    protected fun drawBackground(width: Int, height: Int, padding: Int = 4, centerAnchor: Boolean = false, shadow: Boolean = false, shadowBottom: Boolean = false) {
        val graphics = cachedGraphics ?: return
        if (!showBackground) return

        val x1 = if (centerAnchor) x - width / 2 - padding else x - padding
        val y1 = y - padding
        val x2 = if (centerAnchor) x + width / 2 + padding else x + width + padding
        val y2 = y + height + padding

        if (shadow) {
            // Рисуем тень (с небольшим смещением)
            if (shadowBottom) {
                // Смещение вниз для Slayer Scoreboard
                graphics.fill(x1, y2, x2, y2 + 2, 0x50000000)
            } else {
                graphics.fill(x1 + 2, y1 + 2, x2 + 2, y2 + 2, 0x50000000)
            }
        }

        // Тёмный полупрозрачный фон
        graphics.fill(x1, y1, x2, y2, 0x70000000)
    }

    /**
     * Отрисовать полоску прогресса
     */
    protected fun drawProgressBar(currentX: Int, currentY: Int, width: Int, height: Int, progress: Float, color: Int) {
        val graphics = cachedGraphics ?: return
        val filledWidth = (width * progress.coerceIn(0f, 1f)).toInt()

        // Фон полоски (темный полупрозрачный)
        graphics.fill(currentX, currentY, currentX + width, currentY + height, 0x80000000.toInt())
        // Заполненная часть
        graphics.fill(currentX, currentY, currentX + filledWidth, currentY + height, color)
    }

    /**
     * Цвет акцентной линии (можно переопределять в наследниках)
     */
    open fun getAccentColor(): Int = 0xFF55FF55.toInt()

    /**
     * Получить ширину с учётом масштаба
     */
    fun getScaledWidth(): Int = (getWidth() * scale).toInt()

    /**
     * Получить высоту с учётом масштаба
     */
    fun getScaledHeight(): Int = (getHeight() * scale).toInt()

    open fun getDefaultX(): Int = 2
    open fun getDefaultY(): Int = 2
    open fun getDefaultScale(): Float = 1.0f

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        val w = getScaledWidth()
        val h = getScaledHeight()
        if (w <= 0 || h <= 0) return false
        // Учитываем кламп как в renderWithGraphics — ховер должен совпадать с видимой позицией
        val guiW = try { net.minecraft.client.Minecraft.getInstance().window.guiScaledWidth } catch (_: Exception) { 0 }
        val guiH = try { net.minecraft.client.Minecraft.getInstance().window.guiScaledHeight } catch (_: Exception) { 0 }
        val hx = if (guiW > w) x.coerceIn(0, guiW - w) else x.coerceAtLeast(0)
        val hy = if (guiH > h) y.coerceIn(0, guiH - h) else y.coerceAtLeast(0)
        return mouseX >= hx && mouseX <= hx + w &&
               mouseY >= hy && mouseY <= hy + h
    }

    /**
     * Увеличить масштаб
     */
    fun increaseScale() {
        scale = (scale + SCALE_STEP).coerceAtMost(MAX_SCALE)
    }

    /**
     * Уменьшить масштаб
     */
    fun decreaseScale() {
        scale = (scale - SCALE_STEP).coerceAtLeast(MIN_SCALE)
    }

    fun markAsInitialized() {
        initialized = true
    }
}

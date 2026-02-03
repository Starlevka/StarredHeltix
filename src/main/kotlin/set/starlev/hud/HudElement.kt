package set.starlev.hud

import net.minecraft.client.gui.GuiGraphics

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
    protected var cachedGraphics: net.minecraft.client.gui.GuiGraphics? = null
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

    open fun renderWithGraphics(graphics: GuiGraphics) {
        ensureInitialized()
        cachedGraphics = graphics
        
        // Применить масштабирование (MC 1.21.10+ API)
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        pose.translate(-x.toFloat(), -y.toFloat())
        
        render()
        
        pose.popMatrix()
        cachedGraphics = null
    }

    abstract fun render()
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int

    /**
     * Отрисовать стандартный фон в стиле SkyHanni
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
    open fun getAccentColor(): Int = 0xFF55FF55.toInt() // В 1.21.10 .toInt() все еще может быть нужен для Long литералов

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
        return mouseX >= x && mouseX <= x + w &&
               mouseY >= y && mouseY <= y + h
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

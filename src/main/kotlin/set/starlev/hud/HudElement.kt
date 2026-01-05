package set.starlev.hud

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics

abstract class HudElement(
    val id: String
) {
    var x: Int = 0
    var y: Int = 0
    var scale: Float = 1.0f
    var isEditing = false
    protected var cachedGraphics: net.minecraft.client.gui.GuiGraphics? = null
    private var initialized = false
    
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 3.0f
        const val SCALE_STEP = 0.1f
    }
    
    private fun ensureInitialized() {
        if (!initialized) {
            x = getDefaultX()
            y = getDefaultY()
            scale = getDefaultScale()
            initialized = true
        }
    }

    fun renderWithGraphics(graphics: GuiGraphics) {
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
     */
    protected fun drawBackground(width: Int, height: Int, padding: Int = 4) {
        val graphics = cachedGraphics ?: return
        // Тёмный полупрозрачный фон (сделал чуть прозрачнее: 0x90 -> 0x70)
        graphics.fill(x - padding, y - padding, x + width + padding, y + height + padding, 0x70000000)
        // Тонкая вертикальная линия слева (акцентная)
        graphics.fill(x - padding, y - padding, x - padding + 2, y + height + padding, getAccentColor())
    }

    /**
     * Отрисовать полоску прогресса
     */
    protected fun drawProgressBar(currentX: Int, currentY: Int, width: Int, height: Int, progress: Float, color: Int) {
        val graphics = cachedGraphics ?: return
        val filledWidth = (width * progress.coerceIn(0f, 1f)).toInt()
        
        // Фон полоски (темный)
        graphics.fill(currentX, currentY, currentX + width, currentY + height, 0x60FFFFFF)
        // Заполненная часть
        graphics.fill(currentX, currentY, currentX + filledWidth, currentY + height, color)
    }

    /**
     * Цвет акцентной линии (можно переопределять в наследниках)
     */
    open fun getAccentColor(): Int = 0xFF55FF55.toInt() // По умолчанию зеленый

    /**
     * Получить ширину с учётом масштаба
     */
    fun getScaledWidth(): Int = (getWidth() * scale).toInt()
    
    /**
     * Получить высоту с учётом масштаба
     */
    fun getScaledHeight(): Int = (getHeight() * scale).toInt()
    
    open fun getDefaultX(): Int = 10
    open fun getDefaultY(): Int = 10
    open fun getDefaultScale(): Float = 1.0f

    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + getScaledWidth() &&
               mouseY >= y && mouseY <= y + getScaledHeight()
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

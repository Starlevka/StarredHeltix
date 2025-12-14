package set.starlev.hud

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

    fun renderWithGraphics(graphics: net.minecraft.client.gui.GuiGraphics) {
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

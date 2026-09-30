package set.starlev.render

class RenderEvents private constructor() {
    companion object {
        private val callbacks = mutableListOf<(RenderContext) -> Unit>()

        @JvmStatic
        fun register(callback: (RenderContext) -> Unit) {
            callbacks.add(callback)
        }

        @JvmStatic
        fun fireWorldRender(context: RenderContext) {
            RenderContext.current = context
            for (callback in callbacks) {
                try {
                    callback(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

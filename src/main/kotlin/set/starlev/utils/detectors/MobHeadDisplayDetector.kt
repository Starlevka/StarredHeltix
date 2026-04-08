package set.starlev.utils.detectors

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

object MobHeadDisplayDetector {

    data class HeadDisplays(
        val textDisplays: List<Component>,
        val itemDisplays: List<ItemStack>,
        val totalDisplays: Int
    )

    fun getHeadDisplays(entity: LivingEntity): HeadDisplays {
        val level = entity.level()
        val eyeY = entity.eyeY
        val box = entity.boundingBox
        val aabb = AABB(
            box.minX - 0.5,
            (eyeY - 0.3),
            box.minZ - 0.5,
            box.maxX + 0.5,
            (eyeY + 1.2),
            box.maxZ + 0.5
        )

        val displays = level.getEntitiesOfClass(Display::class.java, aabb) { it.isAlive }
        if (displays.isEmpty()) {
            return HeadDisplays(emptyList(), emptyList(), 0)
        }

        val text = ArrayList<Component>(2)
        val items = ArrayList<ItemStack>(2)

        for (display in displays) {
            if (display.vehicle != entity && display.distanceToSqr(entity) > 1.0) continue

            if (display is Display.TextDisplay) {
                val comp = readTextDisplayComponent(display)
                if (comp != null) text.add(comp)
                continue
            }

            if (display is Display.ItemDisplay) {
                val stack = readItemDisplayStack(display)
                if (stack != null && !stack.isEmpty) items.add(stack)
                continue
            }
        }

        return HeadDisplays(text, items, displays.size)
    }

    private fun readTextDisplayComponent(display: Display.TextDisplay): Component? {
        try {
            val method = display.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && it.returnType == Component::class.java
            } ?: return null
            return method.invoke(display) as? Component
        } catch (_: Throwable) {
            return null
        }
    }

    private fun readItemDisplayStack(display: Display.ItemDisplay): ItemStack? {
        try {
            val method = display.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && ItemStack::class.java.isAssignableFrom(it.returnType)
            } ?: return null
            return method.invoke(display) as? ItemStack
        } catch (_: Throwable) {
            return null
        }
    }
}


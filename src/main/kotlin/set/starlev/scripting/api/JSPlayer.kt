package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

class JSPlayer {
    private val client: Minecraft get() = Minecraft.getInstance()
    private val player get() = client.player

    fun exists(): Boolean = player != null
    fun getName(): String = player?.name?.string ?: ""
    fun getUUID(): String = player?.uuid?.toString() ?: ""
    fun getX(): Double = player?.x ?: 0.0
    fun getY(): Double = player?.y ?: 0.0
    fun getZ(): Double = player?.z ?: 0.0
    fun getPos(): Map<String, Double> {
        val p = player ?: return mapOf("x" to 0.0, "y" to 0.0, "z" to 0.0)
        return mapOf("x" to p.x, "y" to p.y, "z" to p.z)
    }
    fun getYaw(): Float = player?.yRot ?: 0f
    fun getPitch(): Float = player?.xRot ?: 0f
    private inline fun runImmediate(crossinline block: () -> Unit) {
        if (client.isSameThread) block() else client.execute { block() }
    }
    fun setRotation(yaw: Float, pitch: Float) {
        runImmediate { player?.let { it.yRot = yaw; it.xRot = pitch } }
    }
    fun getHealth(): Float = player?.health ?: 0f
    fun getMaxHealth(): Float = player?.maxHealth ?: 0f
    fun getAbsorption(): Float = player?.absorptionAmount ?: 0f
    fun getFoodLevel(): Int = player?.foodData?.foodLevel ?: 0
    fun getSaturationLevel(): Float = player?.foodData?.saturationLevel ?: 0f
    fun getAirSupply(): Int = player?.airSupply ?: 0
    fun getMaxAirSupply(): Int = player?.maxAirSupply ?: 0
    fun getExperienceLevel(): Int = player?.experienceLevel ?: 0
    fun getExperienceProgress(): Float = player?.experienceProgress ?: 0f
    fun isSneaking(): Boolean = player?.isShiftKeyDown ?: false
    fun isSprinting(): Boolean = player?.isSprinting ?: false
    fun setSprinting(sprinting: Boolean) { runImmediate { player?.isSprinting = sprinting } }
    fun isSwimming(): Boolean = player?.isSwimming ?: false
    fun isGliding(): Boolean = player?.isFallFlying ?: false
    fun isMoving(): Boolean {
        val p = player ?: return false
        val delta = p.deltaMovement
        return delta.x * delta.x + delta.z * delta.z > 0.0001
    }
    fun getVelocity(): Map<String, Double> {
        val v = player?.deltaMovement ?: Vec3.ZERO
        return mapOf("x" to v.x, "y" to v.y, "z" to v.z)
    }
    fun getHeldItemName(): String = player?.mainHandItem?.hoverName?.string ?: ""
    fun getOffHandItemName(): String = player?.offhandItem?.hoverName?.string ?: ""
    fun getPotionEffects(): List<Map<String, Any?>> {
        val living = player ?: return emptyList()
        return living.activeEffects.map { effect ->
            val effectType = effect.effect.value()
            mapOf("name" to effectType.descriptionId, "localizedName" to effectType.displayName.string, "amplifier" to effect.amplifier, "duration" to effect.duration, "isInfinite" to effect.isInfiniteDuration, "isAmbient" to effect.isAmbient)
        }
    }
    fun hasPotionEffect(name: String): Boolean {
        val living = player ?: return false
        return living.activeEffects.any {
            val id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(it.effect.value()).toString()
            id.equals(name, ignoreCase = true) || id.endsWith(":$name", ignoreCase = true)
        }
    }
    fun getBlockX(): Int = player?.blockX ?: 0
    fun getBlockY(): Int = player?.blockY ?: 0
    fun getBlockZ(): Int = player?.blockZ ?: 0
    fun swingHand() { runImmediate { player?.swing(net.minecraft.world.InteractionHand.MAIN_HAND) } }
    fun swingOffHand() { runImmediate { player?.swing(net.minecraft.world.InteractionHand.OFF_HAND) } }
    private fun <T> onMainThread(block: () -> T): T? {
        val p = player ?: return null
        return if (client.isSameThread) block() else {
            var result: T? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            client.execute { try { result = block() } finally { latch.countDown() } }
            try { latch.await(1, java.util.concurrent.TimeUnit.SECONDS) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
            result
        }
    }
    fun attack(): Boolean = onMainThread {
        val p = player ?: return@onMainThread false
        val gm = client.gameMode ?: return@onMainThread false
        val hit = client.hitResult as? net.minecraft.world.phys.EntityHitResult ?: return@onMainThread false
        gm.attack(p, hit.entity)
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
        true
    } ?: false
    fun useItem() {
        runImmediate {
            val p = player ?: return@runImmediate
            val gm = client.gameMode ?: return@runImmediate
            val hit = client.hitResult
            when (hit) {
                is net.minecraft.world.phys.BlockHitResult -> gm.useItemOn(p, net.minecraft.world.InteractionHand.MAIN_HAND, hit)
                else -> gm.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND)
            }
        }
    }
    fun interactEntity(maxDistance: Double = 4.5): Boolean = onMainThread {
        val p = player ?: return@onMainThread false
        val hit = client.hitResult as? net.minecraft.world.phys.EntityHitResult ?: return@onMainThread false
        val target = hit.entity
        if (p.distanceTo(target) > maxDistance) return@onMainThread false
        p.interactOn(target, net.minecraft.world.InteractionHand.MAIN_HAND, hit.location).consumesAction()
    } ?: false
    fun dropHeldItem(all: Boolean = false): Boolean = onMainThread {
        val p = player ?: return@onMainThread false
        val dropped = p.drop(all)
        if (dropped) p.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
        dropped
    } ?: false
    fun respawn() { runImmediate { player?.respawn() } }
    fun sendCommand(command: String) {
        val trimmed = command.removePrefix("/")
        runImmediate { player?.let { it.connection.sendCommand(trimmed) } }
    }
    fun sendChat(message: String) {
        runImmediate { player?.let { it.connection.sendChat(message) } }
    }
    fun getAttackCooldown(): Float = player?.getAttackStrengthScale(0.5f) ?: 1.0f
    fun lookAt(x: Double, y: Double, z: Double) {
        runImmediate {
            val p = player ?: return@runImmediate
            val dx = x - p.x
            val dy = y - (p.y + p.eyeHeight)
            val dz = z - p.z
            val horizontal = kotlin.math.sqrt(dx * dx + dz * dz)
            p.yRot = (Math.atan2(dz, dx) * 180.0 / Math.PI).toFloat() - 90f
            p.xRot = (-(Math.atan2(dy, horizontal) * 180.0 / Math.PI)).toFloat()
        }
    }
    fun isOnGround(): Boolean = player?.onGround() ?: false
    fun getGameMode(): String = client.gameMode?.playerMode?.getName() ?: ""
    fun getHeldItemIndex(): Int = player?.inventory?.selected ?: 0
    fun getBiome(): String {
        val p = player ?: return ""
        val level = client.level ?: return ""
        val holder = level.getBiome(p.blockPosition())
        return holder.unwrapKey().map { it.identifier().toString() }.orElse("")
    }
    fun getLightLevel(): Int {
        val p = player ?: return 0
        val level = client.level ?: return 0
        return level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p.blockPosition())
    }
    fun facing(): String {
        val yaw = (player?.yRot ?: return "").mod(360f)
        return when {
            yaw in 315f..360f || yaw in 0f..45f -> "юг"
            yaw in 45f..135f -> "запад"
            yaw in 135f..225f -> "север"
            else -> "восток"
        }
    }
    fun getXPLevel(): Int = player?.experienceLevel ?: 0
    fun getXPProgress(): Float = player?.experienceProgress ?: 0f
    fun getArmorPoints(): Int = player?.armorValue ?: 0
    fun getInput(): Map<String, Boolean> {
        val k = player?.input?.keyPresses ?: return emptyMap()
        return mapOf("forward" to k.forward(), "backward" to k.backward(), "left" to k.left(), "right" to k.right(), "jump" to k.jump(), "shift" to k.shift(), "sprint" to k.sprint())
    }
    fun isPressingForward(): Boolean = player?.input?.keyPresses?.forward() ?: false
    fun isPressingBackward(): Boolean = player?.input?.keyPresses?.backward() ?: false
    fun isPressingLeft(): Boolean = player?.input?.keyPresses?.left() ?: false
    fun isPressingRight(): Boolean = player?.input?.keyPresses?.right() ?: false
    fun isPressingJump(): Boolean = player?.input?.keyPresses?.jump() ?: false
    fun isPressingShift(): Boolean = player?.input?.keyPresses?.shift() ?: false
    fun isPressingSprint(): Boolean = player?.input?.keyPresses?.sprint() ?: false
    fun isPressingAttack(): Boolean = try { client.options.keyAttack.isDown() } catch (_: Exception) { false }
    fun isPressingUse(): Boolean = try { client.options.keyUse.isDown() } catch (_: Exception) { false }
    fun isPressingDrop(): Boolean = try { client.options.keyDrop.isDown() } catch (_: Exception) { false }
    fun isPressingSwap(): Boolean = try { client.options.keySwapOffhand.isDown() } catch (_: Exception) { false }
    fun setPressingAttack(down: Boolean) = runImmediate { try { client.options.keyAttack.setDown(down) } catch (_: Exception) {} }
    fun setPressingUse(down: Boolean) = runImmediate { try { client.options.keyUse.setDown(down) } catch (_: Exception) {} }
    fun setPressingDrop(down: Boolean) = runImmediate { try { client.options.keyDrop.setDown(down) } catch (_: Exception) {} }
    fun setPressingSwap(down: Boolean) = runImmediate { try { client.options.keySwapOffhand.setDown(down) } catch (_: Exception) {} }
    fun setMovement(forward: Boolean, backward: Boolean, left: Boolean, right: Boolean, jump: Boolean, shift: Boolean, sprint: Boolean) {
        runImmediate {
            val opts = client.options
            try { opts.keyUp.setDown(forward) } catch (_: Exception) {}
            try { opts.keyDown.setDown(backward) } catch (_: Exception) {}
            try { opts.keyLeft.setDown(left) } catch (_: Exception) {}
            try { opts.keyRight.setDown(right) } catch (_: Exception) {}
            try { opts.keyJump.setDown(jump) } catch (_: Exception) {}
            try { opts.keyShift.setDown(shift) } catch (_: Exception) {}
            try { opts.keySprint.setDown(sprint) } catch (_: Exception) {}
            val p = player ?: return@runImmediate
            val input = net.minecraft.world.entity.player.Input(forward, backward, left, right, jump, shift, sprint)
            try { p.input.keyPresses = input } catch (_: Exception) {}
            p.isSprinting = sprint
            try { p.connection.send(net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(input)) } catch (_: Exception) {}
        }
    }
    fun stopMovement() {
        runImmediate {
            val opts = client.options
            try { opts.keyUp.setDown(false) } catch (_: Exception) {}
            try { opts.keyDown.setDown(false) } catch (_: Exception) {}
            try { opts.keyLeft.setDown(false) } catch (_: Exception) {}
            try { opts.keyRight.setDown(false) } catch (_: Exception) {}
            try { opts.keyJump.setDown(false) } catch (_: Exception) {}
            try { opts.keyShift.setDown(false) } catch (_: Exception) {}
            try { opts.keySprint.setDown(false) } catch (_: Exception) {}
            try { opts.keyAttack.setDown(false) } catch (_: Exception) {}
            try { opts.keyUse.setDown(false) } catch (_: Exception) {}
            try { opts.keyDrop.setDown(false) } catch (_: Exception) {}
            try { opts.keySwapOffhand.setDown(false) } catch (_: Exception) {}
            try {
                val p = player ?: return@runImmediate
                val input = net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false)
                try { p.input.keyPresses = input } catch (_: Exception) {}
                p.isSprinting = false
                try { p.connection.send(net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(input)) } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }
    fun setPosAndRotation(yaw: Float, pitch: Float, forward: Boolean, backward: Boolean, left: Boolean, right: Boolean, jump: Boolean, shift: Boolean, sprint: Boolean) {
        setRotation(yaw, pitch)
        setMovement(forward, backward, left, right, jump, shift, sprint)
    }
}

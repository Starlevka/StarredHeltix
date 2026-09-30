package set.starlev.scripting.api

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

/**
 * JS API: Объект `particles` — клиентские частицы в мире.
 *
 * ```js
 * particles.spawn("minecraft:heart", 100, 65, 200);
 * particles.spawn("minecraft:flame", 100, 65, 200, 0, 0.1, 0, 5);   // 5 частиц
 * particles.sphere("minecraft:end_rod", 100, 65, 200, 2.0, 20);     // сфера радиуса 2
 * particles.line("minecraft:crit", 0,64,0, 10,70,10, 15);
 * ```
 */
class JSParticles(private val moduleName: String) {
    private val client: Minecraft get() = Minecraft.getInstance()

    /** Спавн n частиц с разбросом скорости (dx, dy, dz — начальная скорость). */
    fun spawn(name: String, x: Double, y: Double, z: Double, dx: Double = 0.0, dy: Double = 0.0, dz: Double = 0.0, count: Int = 1): Boolean {
        val level = client.level ?: return false
        val type = resolve(name) ?: return false
        client.execute {
            repeat(count.coerceIn(1, 500)) {
                level.addParticle(type, x, y, z, dx, dy, dz)
            }
        }
        return true
    }

    /** Частицы по сфере радиуса [radius] вокруг точки. */
    fun sphere(name: String, x: Double, y: Double, z: Double, radius: Double, count: Int = 16): Boolean {
        val level = client.level ?: return false
        val type = resolve(name) ?: return false
        client.execute {
            repeat(count.coerceIn(1, 500)) {
                val theta = Math.random() * Math.PI * 2
                val phi = Math.acos(2 * Math.random() - 1)
                val px = x + radius * Math.sin(phi) * Math.cos(theta)
                val py = y + radius * Math.sin(phi) * Math.sin(theta)
                val pz = z + radius * Math.cos(phi)
                level.addParticle(type, px, py, pz, 0.0, 0.0, 0.0)
            }
        }
        return true
    }

    /** Частицы вдоль линии между двумя точками. */
    fun line(name: String, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, count: Int = 10): Boolean {
        val level = client.level ?: return false
        val type = resolve(name) ?: return false
        client.execute {
            val n = count.coerceIn(1, 500)
            for (i in 0..n) {
                val t = i.toDouble() / n
                level.addParticle(type, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t, 0.0, 0.0, 0.0)
            }
        }
        return true
    }

    /** Частицы в цилиндре (для визуализации зон/аукционов). */
    fun cylinder(name: String, x: Double, y: Double, z: Double, radius: Double, height: Double, count: Int = 24): Boolean {
        val level = client.level ?: return false
        val type = resolve(name) ?: return false
        client.execute {
            repeat(count.coerceIn(1, 500)) {
                val angle = Math.random() * Math.PI * 2
                val r = radius * Math.sqrt(Math.random())
                level.addParticle(
                    type,
                    x + r * Math.cos(angle),
                    y + Math.random() * height,
                    z + r * Math.sin(angle),
                    0.0, 0.0, 0.0
                )
            }
        }
        return true
    }

    private fun resolve(name: String): SimpleParticleType? = try {
        val loc = Identifier.tryParse(if (name.contains(':')) name else "minecraft:$name") ?: return null
        BuiltInRegistries.PARTICLE_TYPE.getOptional(loc).orElse(null) as? SimpleParticleType
    } catch (e: Exception) {
        set.starlev.scripting.ScriptConsole.error(moduleName, "particles: неподдерживаемый тип '$name' (нужен simple particle)")
        null
    }
}

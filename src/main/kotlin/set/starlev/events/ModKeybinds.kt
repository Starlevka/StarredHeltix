package set.starlev.events

import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

/**
 * Отдельная категория биндов «StarredHeltix» в настройках управления.
 * Все кастомные кейбинды мода (конфиг, подсветка по клику, Leap 1-4) лежат здесь,
 * а не в ванильном «Разное» — так их проще найти и не путаются с ванильными.
 *
 * Регистрация одна на мод (повторный register того же id кинул бы
 * IllegalArgumentException) — остальные классы берут готовое [CATEGORY].
 */
object ModKeybinds {
    @JvmField
    val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("starredheltix", "starredheltix")
    )
}

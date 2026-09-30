package set.starlev.features.skyblock

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

/**
 * Тихая проверка обновлений мода при входе на сервер.
 * (раньше жил в WelcomeMessage — welcome-сообщения удалены)
 */
object UpdateOnJoin {
    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            set.starlev.utils.ModUpdater.checkUpdate(quiet = true)
        }
    }
}

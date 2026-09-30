package set.starlev.config.categories

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SoundConfig {
    @Expose
    @ConfigOption(name = "Звуки взрывов", desc = "Блокирует звуки взрывов (TNT, криперы и др.)")
    @ConfigEditorBoolean
    var disableExplosionSounds = false

    @Expose
    @ConfigOption(name = "Звуки фейерверков", desc = "Блокирует запуск и залпы фейерверков")
    @ConfigEditorBoolean
    var disableFireworkSounds = false

    @Expose
    @ConfigOption(name = "Звуки летучих мышей", desc = "Блокирует пищание летучих мышей")
    @ConfigEditorBoolean
    var disableBatSounds = false
}

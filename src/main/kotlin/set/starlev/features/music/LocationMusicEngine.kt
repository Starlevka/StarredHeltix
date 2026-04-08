package set.starlev.features.music

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import set.starlev.StarredHeltix
import set.starlev.config.categories.MusicConfig
import set.starlev.utils.detectors.LocationDetector
import kotlin.math.pow

object LocationMusicEngine {

    private val mc = Minecraft.getInstance()

    private var currentCategory: LocationMusicCategory? = null
    private var targetCategory: LocationMusicCategory? = null
    private var isPlaying = false
    private var isTransitioning = false

    private var lastLocationCheck = 0L
    private var transitionStartTime = 0L
    private var lastNoteTime = 0L
    private var beatInterval = 900L

    private var phraseIntensity = 0.5f
    private var targetIntensity = 0.5f
    private var intensityVelocity = 0f

    private var pendingCategory: LocationMusicCategory? = null
    private var pendingCategoryCount = 0

    private var currentSectionIndex = 0
    private var melodyIndex = 0
    private var harmonyIndex = 0
    private var rhythmIndex = 0
    private var ticksUntilNextNote = 2
    private var sectionChangeCounter = 0
    private val sectionChangeThreshold = 96

    private var currentVolume = 0f
    private var targetVolume = 0f
    private var transitionStartVolume = 0f

    private var phrasePosition = 0
    private val phraseLength = 48

    private var bassNoteIndex = 0

    private var phraseDynamics = 0.5f
    private var targetDynamics = 0.5f

    private val bassPatterns = mapOf(
        AtmosphereType.INTENSE to listOf(
            36, 36, 39, 36, 41, 41, 39, 36,
            36, 36, 43, 43, 41, 39, 36, 36
        ),
        AtmosphereType.CALM to listOf(
            48, 48, 50, 52, 53, 52, 50, 48,
            48, 50, 52, 53, 55, 53, 52, 50
        ),
        AtmosphereType.NATURE to listOf(
            45, 45, 47, 48, 50, 48, 47, 45,
            45, 47, 48, 50, 52, 50, 48, 47
        ),
        AtmosphereType.DEEP to listOf(
            33, 33, 36, 38, 40, 38, 36, 33,
            33, 36, 38, 40, 41, 40, 38, 36
        ),
        AtmosphereType.MYSTIC to listOf(
            28, 31, 33, 36, 38, 36, 33, 31,
            28, 31, 33, 36, 39, 36, 33, 31
        ),
        AtmosphereType.ETHEREAL to listOf(
            40, 40, 43, 45, 48, 45, 43, 40,
            40, 43, 45, 48, 51, 48, 45, 43
        ),
        AtmosphereType.SOCIAL to listOf(
            48, 48, 50, 52, 55, 52, 50, 48,
            48, 50, 52, 55, 57, 55, 52, 50
        )
    )

    fun init() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register { client ->
            val config = StarredHeltix.feature.music.locationMusic
            if (!config.enabled) {
                if (isPlaying) stopMusic()
                return@register
            }

            if (client.player == null || client.level == null) return@register

            updateLocation()
            updateMusic()
        }
    }

    private fun updateLocation() {
        val now = System.currentTimeMillis()
        if (now - lastLocationCheck < 500) return
        lastLocationCheck = now

        val locationName = LocationDetector.getCurrentLocationClean()
        if (locationName == null) return

        val newCategory = LocationMusicCategory.fromLocation(locationName)

        // Если локация неизвестна — не играем музыку
        if (newCategory == null) {
            if (isPlaying) stopMusic()
            pendingCategory = null
            pendingCategoryCount = 0
            return
        }

        val categoryConfig = getCategoryConfig(newCategory)

        // Если категория отключена в конфиге — не запускаем музыку для неё
        if (categoryConfig != null && !categoryConfig.enabled) {
            if (currentCategory == newCategory && isPlaying) stopMusic()
            pendingCategory = null
            pendingCategoryCount = 0
            return
        }

        if (newCategory == pendingCategory) {
            pendingCategoryCount++
        } else {
            pendingCategory = newCategory
            pendingCategoryCount = 1
        }

        if (pendingCategoryCount >= 2 && newCategory != currentCategory) {
            switchCategory(newCategory)
            pendingCategory = null
            pendingCategoryCount = 0
        }
    }

    private fun switchCategory(newCategory: LocationMusicCategory) {
        val config = StarredHeltix.feature.music.locationMusic

        if (currentCategory == null) {
            targetCategory = newCategory
            currentCategory = newCategory
            resetMelody()
            currentVolume = config.musicVolume / 1000f
            targetVolume = currentVolume
            isPlaying = true
            isTransitioning = false
        } else if (newCategory != currentCategory) {
            startTransition(newCategory)
        }
    }

    private fun startTransition(newCategory: LocationMusicCategory?) {
        if (isTransitioning && targetCategory == newCategory) return


        isTransitioning = true
        transitionStartTime = System.currentTimeMillis()
        transitionStartVolume = currentVolume
        targetCategory = newCategory

        if (newCategory == null) {
            isPlaying = false
        }
    }

    private fun updateMusic() {
        val config = StarredHeltix.feature.music.locationMusic
        val now = System.currentTimeMillis()

        if (isTransitioning) {
            val elapsed = (now - transitionStartTime) / 1000f
            val fadeDuration = config.fadeDuration

            if (elapsed >= fadeDuration) {
                isTransitioning = false
                if (targetCategory != null) {
                    currentCategory = targetCategory
                    resetMelody()
                    currentVolume = config.musicVolume / 1000f
                    targetVolume = currentVolume
                    isPlaying = true
                } else {
                    isPlaying = false
                    currentVolume = 0f
                    return
                }
            } else {
                currentVolume = transitionStartVolume * (1f - elapsed / fadeDuration)
            }
            return
        }

        if (!isPlaying || currentCategory == null) return

        if (currentVolume < targetVolume) {
            currentVolume = minOf(currentVolume + 0.1f, targetVolume)
        } else if (currentVolume > targetVolume) {
            currentVolume = maxOf(currentVolume - 0.1f, targetVolume)
        }

        if (currentVolume <= 0.01f) return

        if (now - lastNoteTime >= beatInterval) {
        val rhythm = currentCategory!!.rhythmPattern
            rhythmIndex = (rhythmIndex + 1) % rhythm.size
            ticksUntilNextNote = rhythm[rhythmIndex]

            phrasePosition = (phrasePosition + 1) % phraseLength
            sectionChangeCounter++

            val section = currentCategory!!.sections[currentSectionIndex]
            val progress = phrasePosition.toFloat() / phraseLength
            targetIntensity = section.dynamics.intensityStart + (section.dynamics.intensityEnd - section.dynamics.intensityStart) * progress

            val leitmotifIndex = melodyIndex % section.leitmotif.size
            val leitmotifNote = section.leitmotif[leitmotifIndex]

            val isRest = section.rests.contains(phrasePosition)
            if (!isRest) {
                playNote(currentCategory!!, section, leitmotifNote)
            }

            if (sectionChangeCounter >= sectionChangeThreshold) {
                sectionChangeCounter = 0
                currentSectionIndex = (currentSectionIndex + 1) % currentCategory!!.sections.size
                melodyIndex = 0
                harmonyIndex = 0
                phrasePosition = 0
                bassNoteIndex = 0

                val newSection = currentCategory!!.sections[currentSectionIndex]
                targetDynamics = newSection.dynamics.intensityStart
                phraseDynamics = targetDynamics
                beatInterval = when (newSection.structure) {
                    StructureType.CHORUS -> 700L
                    StructureType.VERSE -> 800L
                    StructureType.BRIDGE -> 900L
                    else -> 1000L
                }
            }
        }

        intensityVelocity += (targetIntensity - phraseIntensity) * 0.05f
        intensityVelocity *= 0.9f
        phraseIntensity += intensityVelocity
    }

    private fun playNote(category: LocationMusicCategory, section: Section, leitmotifNote: Int) {
        val player = mc.player ?: return
        val baseNotes = section.baseNotes
        val harmonyNotes = section.harmonyNotes

        val noteIndex = melodyIndex % baseNotes.size
        val midiNote = baseNotes[noteIndex]
        val configVolume = (StarredHeltix.feature.music.locationMusic.musicVolume / 1000f).coerceAtMost(1.0f)
        val accent = if (phrasePosition == 0) 1.15f else 1.0f
        val volume = currentVolume * phraseDynamics * phraseIntensity * accent * configVolume

        if (volume <= 0.01f) return

        val instrument = selectInstrument(category, section.structure, phrasePosition, leitmotifNote)
        val pitch = midiToPitch(leitmotifNote)
        playNoteWithDecay(instrument.sound, leitmotifNote, volume, 500L)

        val bassPattern = bassPatterns[category.atmosphere] ?: listOf(36, 36, 39, 36)
        val bassMidi = bassPattern[bassNoteIndex % bassPattern.size]
        val bassVolume = volume * 0.6f * (0.8f + phraseIntensity * 0.4f)
        playLayeredNote(SoundEvents.NOTE_BLOCK_BASS.value(), bassMidi, bassVolume, 700L, 8L)
        bassNoteIndex++

        val drumIntensity = (phraseIntensity * 1.2f).coerceAtMost(1f)
        val drumSound = when (category.atmosphere) {
            AtmosphereType.INTENSE -> when (phrasePosition % 8) {
                0 -> SoundEvents.NOTE_BLOCK_BASEDRUM.value()
                4 -> SoundEvents.NOTE_BLOCK_BASEDRUM.value()
                2 -> SoundEvents.NOTE_BLOCK_SNARE.value()
                6 -> SoundEvents.NOTE_BLOCK_SNARE.value()
                else -> null
            }
            AtmosphereType.DEEP -> when (phrasePosition % 8) {
                0, 4 -> SoundEvents.NOTE_BLOCK_BASEDRUM.value()
                2 -> SoundEvents.NOTE_BLOCK_HAT.value()
                else -> null
            }
            AtmosphereType.MYSTIC, AtmosphereType.ETHEREAL -> when (phrasePosition % 8) {
                0, 4 -> SoundEvents.NOTE_BLOCK_HAT.value()
                2 -> SoundEvents.NOTE_BLOCK_CHIME.value()
                else -> null
            }
            else -> when (phrasePosition % 8) {
                0, 4 -> SoundEvents.NOTE_BLOCK_HAT.value()
                else -> null
            }
        }
        drumSound?.let {
            val drumVolume = (volume * 0.4f * drumIntensity * 6.0f).coerceAtMost(2.0f)
            mc.soundManager.play(SimpleSoundInstance.forUI(it, 1.0f, drumVolume))
        }

        if (phrasePosition % 2 == 0 && phraseIntensity > 0.4f) {
            val harmIndex = harmonyIndex % harmonyNotes.size
            val harmNote = harmonyNotes[harmIndex]
            harmonyIndex = (harmonyIndex + 1) % harmonyNotes.size

            val harmInstrument = when (section.structure) {
                StructureType.CHORUS -> SoundEvents.NOTE_BLOCK_BELL.value()
                StructureType.VERSE -> SoundEvents.NOTE_BLOCK_HARP.value()
                else -> SoundEvents.NOTE_BLOCK_CHIME.value()
            }
            val harmVolume = volume * 0.45f * phraseIntensity

            playLayeredNote(harmInstrument, harmNote, harmVolume, 550L, 12L)
        }

        if (section.structure == StructureType.CHORUS && phrasePosition % 4 == 0) {
            val chordRoot = harmonyNotes[harmonyIndex % harmonyNotes.size]
            val chordType = when (category.atmosphere) {
                AtmosphereType.INTENSE -> listOf(0, 3, 7)
                AtmosphereType.MYSTIC -> listOf(0, 4, 7, 10)
                else -> listOf(0, 4, 7)
            }
            chordType.forEachIndexed { idx, interval ->
                val chordNote = chordRoot + interval
                val chordVol = volume * 0.35f * (1f - idx * 0.15f)
                playNoteWithDecay(SoundEvents.NOTE_BLOCK_HARP.value(), chordNote, chordVol, 600L, idx * 20L)
            }
        }

        melodyIndex = (melodyIndex + 1) % baseNotes.size
        lastNoteTime = System.currentTimeMillis()
    }

    private fun playNoteWithDecay(sound: net.minecraft.sounds.SoundEvent, midiNote: Int, volume: Float, decayMs: Long, delayMs: Long = 0L) {
        val pitch = midiToPitch(midiNote)
        val amplifiedVolume = (volume * 6.0f).coerceAtMost(2.0f)
        mc.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, amplifiedVolume))
    }

    private fun playLayeredNote(sound: net.minecraft.sounds.SoundEvent, midiNote: Int, volume: Float, decayMs: Long, layerDelay: Long = 15L) {
        val pitch = midiToPitch(midiNote)
        val amplifiedVolume = (volume * 6.0f).coerceAtMost(2.0f)
        mc.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, amplifiedVolume))
    }

    private fun selectInstrument(category: LocationMusicCategory, structure: StructureType, phrasePos: Int, note: Int): Instrument {
        return when (category.atmosphere) {
            AtmosphereType.INTENSE -> when (structure) {
                StructureType.CHORUS -> if (phrasePos % 2 == 0) Instrument.PLING else Instrument.GUITAR
                StructureType.VERSE -> Instrument.HARP
                else -> Instrument.BASS
            }
            AtmosphereType.CALM -> when (structure) {
                StructureType.INTRO -> Instrument.FLUTE
                StructureType.CHORUS -> Instrument.BELL
                else -> if (phrasePos % 3 == 0) Instrument.FLUTE else Instrument.HARP
            }
            AtmosphereType.NATURE -> when (structure) {
                StructureType.CHORUS -> Instrument.BELL
                StructureType.VERSE -> Instrument.GUITAR
                else -> Instrument.FLUTE
            }
            AtmosphereType.DEEP -> when (structure) {
                StructureType.CHORUS -> Instrument.IRON_XYLOPHONE
                StructureType.BRIDGE -> Instrument.BIT
                else -> Instrument.BASS
            }
            AtmosphereType.MYSTIC -> when (structure) {
                StructureType.CHORUS -> Instrument.CHIME
                StructureType.VERSE -> Instrument.BELL
                else -> Instrument.IRON_XYLOPHONE
            }
            AtmosphereType.ETHEREAL -> when (structure) {
                StructureType.CHORUS -> Instrument.BELL
                StructureType.VERSE -> Instrument.CHIME
                else -> Instrument.HARP
            }
            AtmosphereType.SOCIAL -> when (structure) {
                StructureType.CHORUS -> Instrument.BANJO
                StructureType.VERSE -> Instrument.GUITAR
                else -> Instrument.HARP
            }
        }
    }

    private fun midiToPitch(midiNote: Int): Float {
        return 2.0f.pow((midiNote - 69) / 12.0f)
    }

    private fun getCategoryConfig(category: LocationMusicCategory): MusicConfig.LocationMusicConfig.LocationMusicEntryConfig? {
        return when (category) {
            LocationMusicCategory.COMBAT -> StarredHeltix.feature.music.locationMusic.combat
            LocationMusicCategory.FARMING -> StarredHeltix.feature.music.locationMusic.farming
            LocationMusicCategory.FORAGING -> StarredHeltix.feature.music.locationMusic.foraging
            LocationMusicCategory.MINING -> StarredHeltix.feature.music.locationMusic.mining
            LocationMusicCategory.ENDER -> StarredHeltix.feature.music.locationMusic.ender
            LocationMusicCategory.MYSTERIOUS -> StarredHeltix.feature.music.locationMusic.mysterious
            LocationMusicCategory.HUB -> StarredHeltix.feature.music.locationMusic.hub
            LocationMusicCategory.ISLAND -> StarredHeltix.feature.music.locationMusic.island
        }
    }

    private fun resetMelody() {
        currentSectionIndex = 0
        melodyIndex = 0
        harmonyIndex = 0
        rhythmIndex = 0
        ticksUntilNextNote = 2
        sectionChangeCounter = 0
        phrasePosition = 0
        bassNoteIndex = 0

        currentCategory?.sections?.firstOrNull()?.let {
            targetDynamics = it.dynamics.intensityStart
            phraseDynamics = targetDynamics
            beatInterval = when (it.structure) {
                StructureType.CHORUS -> 700L
                StructureType.VERSE -> 800L
                StructureType.BRIDGE -> 900L
                else -> 1000L
            }
        }
    }

    private fun stopMusic() {
        if (isPlaying) startTransition(null)
    }

    fun getCurrentCategory(): LocationMusicCategory? = currentCategory
    fun isMusicPlaying(): Boolean = isPlaying && currentVolume > 0.01f
    fun getCurrentVolume(): Float = currentVolume

    private enum class Instrument(val sound: net.minecraft.sounds.SoundEvent) {
        HARP(SoundEvents.NOTE_BLOCK_HARP.value()),
        BASS(SoundEvents.NOTE_BLOCK_BASS.value()),
        BELL(SoundEvents.NOTE_BLOCK_BELL.value()),
        FLUTE(SoundEvents.NOTE_BLOCK_FLUTE.value()),
        CHIME(SoundEvents.NOTE_BLOCK_CHIME.value()),
        PLING(SoundEvents.NOTE_BLOCK_PLING.value()),
        GUITAR(SoundEvents.NOTE_BLOCK_GUITAR.value()),
        BASEDRUM(SoundEvents.NOTE_BLOCK_BASEDRUM.value()),
        SNARE(SoundEvents.NOTE_BLOCK_SNARE.value()),
        HAT(SoundEvents.NOTE_BLOCK_HAT.value()),
        XYLOPHONE(SoundEvents.NOTE_BLOCK_XYLOPHONE.value()),
        IRON_XYLOPHONE(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE.value()),
        DIDGERIDOO(SoundEvents.NOTE_BLOCK_DIDGERIDOO.value()),
        BIT(SoundEvents.NOTE_BLOCK_BIT.value()),
        BANJO(SoundEvents.NOTE_BLOCK_BANJO.value())
    }
}

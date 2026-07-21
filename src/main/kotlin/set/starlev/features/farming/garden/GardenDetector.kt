package set.starlev.features.farming.garden

import set.starlev.utils.detectors.ScoreboardDetector

object GardenDetector {

    private var cachedInGarden: Boolean = false
    private var lastPollTime: Long = 0
    private const val POLL_INTERVAL_MS = 2000L

    fun inGarden(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPollTime < POLL_INTERVAL_MS) return cachedInGarden
        lastPollTime = now
        cachedInGarden = detectGarden()
        return cachedInGarden
    }

    private fun detectGarden(): Boolean {
        val lines = ScoreboardDetector.getScoreboardText()
        for (line in lines) {
            val clean = line.replace(Regex("(?i)§[0-9a-fk-orlnmxz]"), "").trim()
            if ((clean.contains('⏣') || clean.contains('ф')) && clean.contains("Сад", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun resetCache() {
        cachedInGarden = false
        lastPollTime = 0
    }
}

package set.starlev.secret.features.ai

import set.starlev.utils.detectors.ScoreboardDetector
import set.starlev.utils.detectors.TabListDetector

object AiGameInfo {

    private fun findInLines(lines: List<String>, keywords: List<String>): String {
        for (line in lines) {
            if (keywords.any { line.contains(it, ignoreCase = true) }) {
                // Return everything after the keyword and colon
                val parts = line.split(":")
                return if (parts.size > 1) parts[1].trim() else line.trim()
            }
        }
        return ""
    }
}

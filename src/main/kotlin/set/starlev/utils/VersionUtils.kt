package set.starlev.utils

/**
 * Единое сравнение semver-подобных версий "1.2.3" — замена дубликатов в ModUpdater и ModuleUpdater.
 */
object VersionUtils {
    /** true если candidate новее current. Неверные части = 0, суффиксы отбрасываются через `replace("[^0-9.]")`. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.replace(Regex("[^0-9.]"), "").split('.').map { it.trim().toIntOrNull() ?: 0 }
        val b = current.replace(Regex("[^0-9.]"), "").split('.').map { it.trim().toIntOrNull() ?: 0 }
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai != bi) return ai > bi
        }
        return false
    }
}

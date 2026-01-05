package set.starlev.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object PersonalBestManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val folder = File("starredheltix")
    private val file = File(folder, "personal_bests.json")
    
    private var records: MutableMap<String, Double> = mutableMapOf()

    init {
        load()
    }

    private fun load() {
        if (!folder.exists()) folder.mkdirs()
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<MutableMap<String, Double>>() {}.type
                records = gson.fromJson(json, type) ?: mutableMapOf()
            } catch (e: Exception) {
                records = mutableMapOf()
            }
        }
    }

    private fun save() {
        if (!folder.exists()) folder.mkdirs()
        try {
            file.writeText(gson.toJson(records))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPB(bossName: String, tier: String): Double? {
        val key = "$bossName $tier"
        return records[key]
    }

    fun updatePB(bossName: String, tier: String, time: Double): Pair<Boolean, Double?> {
        val key = "$bossName $tier"
        val oldPB = records[key]
        
        if (oldPB == null || time < oldPB) {
            records[key] = time
            save()
            return true to oldPB
        }
        
        return false to oldPB
    }
}

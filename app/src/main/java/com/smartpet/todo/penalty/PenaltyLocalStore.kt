package com.smartpet.todo.penalty

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class PenaltyLocalStore(
    private val rootDir: File,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {
    private val settingsFile = File(rootDir, "penalty-settings.json")
    private val profilesFile = File(rootDir, "penalty-profiles.json")
    private val triggersFile = File(rootDir, "penalty-triggers.json")

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    @Synchronized
    fun loadSettings(): PenaltySettings {
        if (!settingsFile.exists()) return PenaltySettings()
        return runCatching {
            gson.fromJson(settingsFile.readText(), PenaltySettings::class.java) ?: PenaltySettings()
        }.getOrDefault(PenaltySettings())
    }

    @Synchronized
    fun saveSettings(settings: PenaltySettings) {
        settingsFile.writeText(gson.toJson(settings))
    }

    @Synchronized
    fun loadProfiles(): Map<String, PenaltyProfile> {
        if (!profilesFile.exists()) return emptyMap()
        val type = object : TypeToken<List<PenaltyProfile>>() {}.type
        val list = runCatching {
            gson.fromJson<List<PenaltyProfile>>(profilesFile.readText(), type) ?: emptyList()
        }.getOrDefault(emptyList())
        return list.associateBy { it.taskId }
    }

    @Synchronized
    fun saveProfile(profile: PenaltyProfile) {
        val next = loadProfiles().toMutableMap()
        next[profile.taskId] = profile.copy(updatedAt = System.currentTimeMillis())
        profilesFile.writeText(gson.toJson(next.values.sortedBy { it.taskId }))
    }

    @Synchronized
    fun saveProfiles(profiles: Collection<PenaltyProfile>) {
        profilesFile.writeText(gson.toJson(profiles.sortedBy { it.taskId }))
    }

    @Synchronized
    fun removeProfile(taskId: String) {
        val next = loadProfiles().toMutableMap()
        next.remove(taskId)
        saveProfiles(next.values)
    }

    @Synchronized
    fun loadTriggers(): Map<String, PenaltyTriggerRecord> {
        if (!triggersFile.exists()) return emptyMap()
        val type = object : TypeToken<List<PenaltyTriggerRecord>>() {}.type
        val list = runCatching {
            gson.fromJson<List<PenaltyTriggerRecord>>(triggersFile.readText(), type) ?: emptyList()
        }.getOrDefault(emptyList())
        return list.associateBy { it.taskId }
    }

    @Synchronized
    fun markTriggered(record: PenaltyTriggerRecord) {
        val next = loadTriggers().toMutableMap()
        next[record.taskId] = record
        triggersFile.writeText(gson.toJson(next.values.sortedBy { it.taskId }))
    }

    @Synchronized
    fun clearTriggered(taskId: String) {
        val next = loadTriggers().toMutableMap()
        next.remove(taskId)
        triggersFile.writeText(gson.toJson(next.values.sortedBy { it.taskId }))
    }
}

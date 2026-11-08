package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StremioProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val enabled: Boolean = true,
    val streamerNumber: Int
)

val DEFAULT_STREAM_PROVIDERS = listOf(
    StremioProviderConfig(
        id = "provider_1",
        name = "HD Hub",
        baseUrl = "https://hdhub.thevolecitor.qzz.io/eyJ0b3Jib3giOiJ1bnNldCIsInF1YWxpdGllcyI6IjIxNjBwLDEwODBwLDcyMHAiLCJzb3J0IjoiZGVzYyJ9",
        enabled = true,
        streamerNumber = 1
    ),
    StremioProviderConfig(
        id = "provider_2",
        name = "PenguPlay",
        baseUrl = "https://pengu.uk",
        enabled = true,
        streamerNumber = 2
    ),
    StremioProviderConfig(
        id = "provider_3",
        name = "YAS Stream",
        baseUrl = "https://yastream.tamthai.de/eyJjYXRhbG9ncyI6WyJraXNza2guc2VyaWVzLktvcmVhbiIsImtpc3NraC5zZXJpZXMuU2VhcmNoIiwia2lzc2toLm1vdmllLlNlYXJjaCIsIm9uZXRvdWNodHYuc2VyaWVzLlNlYXJjaCIsImlkcmFtYS5zZXJpZXMuaURyYW1hIiwiaWRyYW1hLnNlcmllcy5TZWFyY2giXSwiY2F0YWxvZyI6WyJraXNza2giXSwic3RyZWFtIjpbImtpc3NraCIsIm9uZXRvdWNodHYiXSwibnNmdyI6ZmFsc2UsImluZm8iOmZhbHNlLCJwb3N0ZXIiOiJycGRiIiwibWZwVXJsIjoiIiwidGJLZXkiOiIiLCJtZnBQYXNzIjoiIn0=",
        enabled = true,
        streamerNumber = 3
    )
)

const val NETFLIX_ADDON_PROVIDER_URL = "https://7a82163c306e-stremio-netflix-catalog-addon.baby-beamup.club"

data class UserProfile(
    val name: String = "Cinema Fan",
    val customAvatarPath: String? = null,
    val defaultAction: String = "player",
    val autoplayTrailer: Boolean = true,
    val trailerMuted: Boolean = false,
    val preferredQuality: String = "Auto",
    val librarySortOrder: String = "custom",
    val libraryTileSize: String = "compact",
    val accentColorHex: String = "#1A73E8", // Google Blue default
    val hdhubEnabled: Boolean = true,
    val penguEnabled: Boolean = true,
    val isYoutubeConnected: Boolean = false,
    val youtubeAccountName: String? = null,
    val showTrailerDebug: Boolean = false,
    val runForegroundService: Boolean = false,
    val startOnBoot: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val useNetflixAsProvider: Boolean = false
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cinestream_user_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _providersFlow = MutableStateFlow(loadProviders())
    val providersFlow: StateFlow<List<StremioProviderConfig>> = _providersFlow.asStateFlow()

    private fun loadProfile(): UserProfile {
        return UserProfile(
            name = prefs.getString("user_name", "Cinema Fan") ?: "Cinema Fan",
            customAvatarPath = prefs.getString("custom_avatar_path", null),
            defaultAction = prefs.getString("default_action", "player") ?: "player",
            autoplayTrailer = prefs.getBoolean("autoplay_trailer", true),
            trailerMuted = prefs.getBoolean("trailer_muted", false),
            preferredQuality = prefs.getString("preferred_quality", "Auto") ?: "Auto",
            librarySortOrder = prefs.getString("library_sort_order", "custom") ?: "custom",
            accentColorHex = prefs.getString("accent_color_hex", "#1A73E8") ?: "#1A73E8",
            hdhubEnabled = prefs.getBoolean("hdhub_enabled", true),
            penguEnabled = prefs.getBoolean("pengu_enabled", true),
            libraryTileSize = prefs.getString("library_tile_size", "compact") ?: "compact",
            isYoutubeConnected = prefs.getBoolean("yt_connected", false),
            youtubeAccountName = prefs.getString("yt_account_name", null),
            showTrailerDebug = prefs.getBoolean("show_trailer_debug", false),
            runForegroundService = prefs.getBoolean("run_foreground_service", false),
            startOnBoot = prefs.getBoolean("start_on_boot", false),
            hapticFeedbackEnabled = prefs.getBoolean("haptic_feedback_enabled", true),
            useNetflixAsProvider = prefs.getBoolean("use_netflix_as_provider", false)
        )
    }

    fun getProviders(): List<StremioProviderConfig> = _providersFlow.value

    private fun loadProviders(): List<StremioProviderConfig> {
        val jsonStr = prefs.getString("custom_stream_providers", null)
        if (jsonStr.isNullOrBlank()) return DEFAULT_STREAM_PROVIDERS
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<StremioProviderConfig>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StremioProviderConfig(
                        id = obj.optString("id", "provider_${i + 1}"),
                        name = obj.optString("name", "Provider ${i + 1}"),
                        baseUrl = obj.optString("baseUrl", ""),
                        enabled = obj.optBoolean("enabled", true),
                        streamerNumber = obj.optInt("streamerNumber", i + 1)
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_STREAM_PROVIDERS else list
        } catch (e: Exception) {
            DEFAULT_STREAM_PROVIDERS
        }
    }

    fun saveProviders(providers: List<StremioProviderConfig>) {
        try {
            val arr = org.json.JSONArray()
            providers.forEach { p ->
                val obj = org.json.JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("baseUrl", p.baseUrl)
                    put("enabled", p.enabled)
                    put("streamerNumber", p.streamerNumber)
                }
                arr.put(obj)
            }
            prefs.edit().putString("custom_stream_providers", arr.toString()).apply()
            _providersFlow.value = providers

            val hasNetflix = providers.any { it.enabled && it.baseUrl.contains("netflix", ignoreCase = true) }
            prefs.edit().putBoolean("use_netflix_as_provider", hasNetflix).apply()
            _userProfile.value = _userProfile.value.copy(useNetflixAsProvider = hasNetflix)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetDefaultProviders(): List<StremioProviderConfig> {
        saveProviders(DEFAULT_STREAM_PROVIDERS)
        return DEFAULT_STREAM_PROVIDERS
    }

    fun addProvider(name: String, baseUrl: String): StremioProviderConfig {
        val current = getProviders().toMutableList()
        val nextNumber = (current.maxOfOrNull { it.streamerNumber } ?: current.size) + 1
        val newProvider = StremioProviderConfig(
            id = "custom_provider_${System.currentTimeMillis()}",
            name = name.ifBlank { "Provider $nextNumber" },
            baseUrl = baseUrl.trim(),
            enabled = true,
            streamerNumber = nextNumber
        )
        current.add(newProvider)
        saveProviders(current)
        return newProvider
    }

    fun updateProvider(id: String, name: String, baseUrl: String, enabled: Boolean) {
        val current = getProviders().map {
            if (it.id == id) it.copy(name = name.trim(), baseUrl = baseUrl.trim(), enabled = enabled)
            else it
        }
        saveProviders(current)
    }

    fun toggleProvider(id: String, enabled: Boolean) {
        val current = getProviders().map {
            if (it.id == id) it.copy(enabled = enabled)
            else it
        }
        saveProviders(current)
    }

    fun removeProvider(id: String) {
        val current = getProviders().filterNot { it.id == id }
        saveProviders(current)
    }

    fun toggleNetflixAddonProvider(enabled: Boolean) {
        val current = getProviders().toMutableList()
        val netflixIdx = current.indexOfFirst { it.baseUrl.contains("netflix", ignoreCase = true) }
        if (netflixIdx >= 0) {
            val existing = current[netflixIdx]
            current[netflixIdx] = existing.copy(enabled = enabled)
        } else if (enabled) {
            val nextNumber = (current.maxOfOrNull { it.streamerNumber } ?: current.size) + 1
            current.add(
                StremioProviderConfig(
                    id = "netflix_addon_provider",
                    name = "Netflix Add-on",
                    baseUrl = NETFLIX_ADDON_PROVIDER_URL,
                    enabled = true,
                    streamerNumber = nextNumber
                )
            )
        }
        saveProviders(current)
    }

    fun updateRunForegroundService(enabled: Boolean) {
        prefs.edit().putBoolean("run_foreground_service", enabled).apply()
        _userProfile.value = _userProfile.value.copy(runForegroundService = enabled)
    }

    fun updateStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean("start_on_boot", enabled).apply()
        _userProfile.value = _userProfile.value.copy(startOnBoot = enabled)
    }

    fun updateShowTrailerDebug(show: Boolean) {
        prefs.edit().putBoolean("show_trailer_debug", show).apply()
        _userProfile.value = _userProfile.value.copy(showTrailerDebug = show)
    }

    fun updateAccentColor(hex: String) {
        val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
        prefs.edit().putString("accent_color_hex", cleanHex).apply()
        _userProfile.value = _userProfile.value.copy(accentColorHex = cleanHex)
    }

    fun updateName(name: String) {
        prefs.edit().putString("user_name", name.trim()).apply()
        _userProfile.value = _userProfile.value.copy(name = name.trim())
    }

    fun updateCustomAvatarPath(path: String?) {
        if (path != null) {
            prefs.edit().putString("custom_avatar_path", path).apply()
        } else {
            prefs.edit().remove("custom_avatar_path").apply()
        }
        _userProfile.value = _userProfile.value.copy(customAvatarPath = path)
    }

    fun updateDefaultAction(action: String) {
        prefs.edit().putString("default_action", action).apply()
        _userProfile.value = _userProfile.value.copy(defaultAction = action)
    }

    fun updateAutoplayTrailer(autoplay: Boolean) {
        prefs.edit().putBoolean("autoplay_trailer", autoplay).apply()
        _userProfile.value = _userProfile.value.copy(autoplayTrailer = autoplay)
    }

    fun updateTrailerMuted(muted: Boolean) {
        prefs.edit().putBoolean("trailer_muted", muted).apply()
        _userProfile.value = _userProfile.value.copy(trailerMuted = muted)
    }

    fun updatePreferredQuality(quality: String) {
        prefs.edit().putString("preferred_quality", quality).apply()
        _userProfile.value = _userProfile.value.copy(preferredQuality = quality)
    }

    fun updateLibrarySortOrder(sortOrder: String) {
        prefs.edit().putString("library_sort_order", sortOrder).apply()
        _userProfile.value = _userProfile.value.copy(librarySortOrder = sortOrder)
    }

    fun updateLibraryTileSize(size: String) {
        val clean = size.lowercase().trim()
        prefs.edit().putString("library_tile_size", clean).apply()
        _userProfile.value = _userProfile.value.copy(libraryTileSize = clean)
    }

    fun updateHdhubEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("hdhub_enabled", enabled).apply()
        _userProfile.value = _userProfile.value.copy(hdhubEnabled = enabled)
    }

    fun updatePenguEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pengu_enabled", enabled).apply()
        _userProfile.value = _userProfile.value.copy(penguEnabled = enabled)
    }

    fun updateYoutubeConnected(connected: Boolean, accountName: String? = null) {
        prefs.edit().putBoolean("yt_connected", connected).putString("yt_account_name", accountName).apply()
        _userProfile.value = _userProfile.value.copy(
            isYoutubeConnected = connected,
            youtubeAccountName = accountName
        )
    }

    fun saveCustomMeta(id: String, jsonStr: String) {
        prefs.edit().putString("custom_meta_$id", jsonStr).apply()
    }

    fun getCustomMeta(id: String): String? {
        return prefs.getString("custom_meta_$id", null)
    }

    fun saveCustomStreams(id: String, jsonStr: String) {
        prefs.edit().putString("custom_streams_$id", jsonStr).apply()
    }

    fun getCustomStreams(id: String): String? {
        return prefs.getString("custom_streams_$id", null)
    }

    fun saveCustomSlideshow(id: String, jsonStr: String) {
        prefs.edit().putString("custom_slideshow_$id", jsonStr).apply()
    }

    fun getCustomSlideshow(id: String): String? {
        return prefs.getString("custom_slideshow_$id", null)
    }

    fun hideStream(id: String, streamKey: String) {
        val set = prefs.getStringSet("hidden_streams_$id", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(streamKey)
        prefs.edit().putStringSet("hidden_streams_$id", set).apply()
    }

    fun getHiddenStreams(id: String): Set<String> {
        return prefs.getStringSet("hidden_streams_$id", emptySet()) ?: emptySet()
    }

    fun clearCustomStreams(id: String) {
        prefs.edit().remove("custom_streams_$id").apply()
    }

    fun clearCustomMeta(id: String) {
        prefs.edit().remove("custom_meta_$id").apply()
    }

    fun clearCustomSlideshow(id: String) {
        prefs.edit().remove("custom_slideshow_$id").apply()
    }

    fun clearHiddenStreams(id: String) {
        prefs.edit().remove("hidden_streams_$id").apply()
    }

    fun saveCustomHomeCategories(jsonStr: String) {
        prefs.edit().putString("custom_home_categories", jsonStr).apply()
    }

    fun clearCustomHomeCategories() {
        prefs.edit().remove("custom_home_categories").apply()
    }

    fun getCustomHomeCategories(): String? {
        return prefs.getString("custom_home_categories", null)
    }

    fun saveCustomHomeHeroes(jsonStr: String) {
        prefs.edit().putString("custom_home_heroes", jsonStr).apply()
    }

    fun getCustomHomeHeroes(): String? {
        return prefs.getString("custom_home_heroes", null)
    }

    fun clearCustomHomeHeroes() {
        prefs.edit().remove("custom_home_heroes").apply()
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback_enabled", enabled).apply()
        _userProfile.value = _userProfile.value.copy(hapticFeedbackEnabled = enabled)
    }

    fun exportAllSettingsJson(): String {
        val root = org.json.JSONObject()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            when (value) {
                is String -> root.put(key, value)
                is Boolean -> root.put(key, value)
                is Int -> root.put(key, value)
                is Long -> root.put(key, value)
                is Float -> root.put(key, value.toDouble())
                is Set<*> -> {
                    val arr = org.json.JSONArray()
                    value.forEach { arr.put(it) }
                    root.put(key, arr)
                }
            }
        }
        return root.toString(2)
    }

    fun importAllSettingsJson(jsonStr: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonStr)
            val editor = prefs.edit()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = root.get(key)
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is org.json.JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            set.add(value.getString(i))
                        }
                        editor.putStringSet(key, set)
                    }
                }
            }
            editor.apply()
            _userProfile.value = loadProfile()
            _providersFlow.value = loadProviders()
            true
        } catch (e: Exception) {
            false
        }
    }
}

package com.example.data.repository

import com.example.data.local.SavedMediaDao
import com.example.data.local.SavedMediaEntity
import com.example.data.model.StremioCatalogMeta
import com.example.data.model.StremioStream
import com.example.data.pref.UserPreferences
import com.example.data.remote.StremioClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@kotlinx.serialization.Serializable
data class CategorySection(
    val id: String,
    val title: String,
    val platformName: String,
    val type: String,
    val items: List<StremioCatalogMeta>
)

class MediaRepository(
    private val client: StremioClient,
    private val savedDao: SavedMediaDao,
    private val userPreferences: UserPreferences
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile
    private var cachedCategories: List<CategorySection>? = null

    private val movieDetailsCache = java.util.concurrent.ConcurrentHashMap<String, StremioCatalogMeta>()
    private val customStreamsMap = java.util.concurrent.ConcurrentHashMap<String, List<StremioStream>>()
    private val customMetaMap = java.util.concurrent.ConcurrentHashMap<String, StremioCatalogMeta>()

    fun hideStream(id: String, stream: StremioStream) {
        val key = "${stream.streamerNumber}_${stream.name}_${stream.title}_${stream.playableUrl}"
        userPreferences.hideStream(id, key)
    }

    fun isStreamHidden(id: String, stream: StremioStream): Boolean {
        val key = "${stream.streamerNumber}_${stream.name}_${stream.title}_${stream.playableUrl}"
        val hiddenForId = userPreferences.getHiddenStreams(id)
        if (hiddenForId.contains(key)) return true
        if (id.contains(":")) {
            val hiddenForParent = userPreferences.getHiddenStreams(id.substringBefore(":"))
            if (hiddenForParent.contains(key)) return true
        }
        return false
    }

    fun saveCustomHomeCategories(categories: List<CategorySection>) {
        cachedCategories = categories
        runCatching {
            val json = jsonParser.encodeToString(categories)
            userPreferences.saveCustomHomeCategories(json)
        }
    }

    fun getCustomHomeCategories(): List<CategorySection>? {
        cachedCategories?.let { return it }
        val saved = userPreferences.getCustomHomeCategories() ?: return null
        return runCatching {
            jsonParser.decodeFromString<List<CategorySection>>(saved)
        }.getOrNull()?.also { cachedCategories = it }
    }

    fun saveCustomHomeHeroes(heroes: List<StremioCatalogMeta>) {
        runCatching {
            val json = jsonParser.encodeToString(heroes)
            userPreferences.saveCustomHomeHeroes(json)
        }
    }

    fun getCustomHomeHeroes(): List<StremioCatalogMeta>? {
        val saved = userPreferences.getCustomHomeHeroes() ?: return null
        return runCatching {
            jsonParser.decodeFromString<List<StremioCatalogMeta>>(saved)
        }.getOrNull()
    }

    fun saveCustomStreamEdits(id: String, streams: List<StremioStream>) {
        customStreamsMap[id] = streams
        runCatching {
            val json = jsonParser.encodeToString(streams)
            userPreferences.saveCustomStreams(id, json)
        }
    }

    fun getCustomStreamEdits(id: String): List<StremioStream>? {
        if (customStreamsMap.containsKey(id)) return customStreamsMap[id]
        val savedJson = userPreferences.getCustomStreams(id) ?: return null
        return runCatching {
            jsonParser.decodeFromString<List<StremioStream>>(savedJson)
        }.getOrNull()?.also {
            customStreamsMap[id] = it
        }
    }

    fun saveCustomMetaEdits(meta: StremioCatalogMeta) {
        customMetaMap[meta.id] = meta
        customMetaMap[meta.displayImdbId] = meta
        val norm = meta.name.trim().lowercase()
        if (norm.isNotBlank()) {
            customMetaMap["name_$norm"] = meta
        }
        movieDetailsCache[meta.id] = meta
        movieDetailsCache[meta.displayImdbId] = meta
        runCatching {
            val json = jsonParser.encodeToString(meta)
            userPreferences.saveCustomMeta(meta.id, json)
            userPreferences.saveCustomMeta(meta.displayImdbId, json)
            if (norm.isNotBlank()) {
                userPreferences.saveCustomMeta("name_$norm", json)
            }
        }
    }

    fun getCustomMeta(id: String): StremioCatalogMeta? {
        if (customMetaMap.containsKey(id)) return customMetaMap[id]
        val savedJson = userPreferences.getCustomMeta(id) ?: return null
        return runCatching {
            jsonParser.decodeFromString<StremioCatalogMeta>(savedJson)
        }.getOrNull()?.also {
            customMetaMap[id] = it
            movieDetailsCache[id] = it
        }
    }

    fun getCustomMetaByName(name: String): StremioCatalogMeta? {
        val norm = name.trim().lowercase()
        if (norm.isBlank()) return null
        val key = "name_$norm"
        if (customMetaMap.containsKey(key)) return customMetaMap[key]
        val savedJson = userPreferences.getCustomMeta(key) ?: return null
        return runCatching {
            jsonParser.decodeFromString<StremioCatalogMeta>(savedJson)
        }.getOrNull()?.also {
            customMetaMap[key] = it
            movieDetailsCache[key] = it
        }
    }

    fun saveHeroCustomImage(hero: StremioCatalogMeta, newImageUrl: String): StremioCatalogMeta {
        val updated = hero.copy(
            poster = newImageUrl,
            background = newImageUrl
        )
        saveCustomMetaEdits(updated)
        saveCustomSlideshow(updated.id, listOf(newImageUrl))
        saveCustomSlideshow(updated.displayImdbId, listOf(newImageUrl))
        val norm = updated.name.trim().lowercase()
        if (norm.isNotBlank()) {
            saveCustomSlideshow("name_$norm", listOf(newImageUrl))
        }

        // Also update in saved custom home heroes if present
        val savedHeroes = getCustomHomeHeroes()
        if (savedHeroes != null && savedHeroes.isNotEmpty()) {
            val updatedHeroes = savedHeroes.map {
                if (it.id == hero.id || it.displayImdbId == hero.displayImdbId || it.name.equals(hero.name, ignoreCase = true)) {
                    it.copy(poster = newImageUrl, background = newImageUrl)
                } else it
            }
            saveCustomHomeHeroes(updatedHeroes)
        }

        // Update in cachedCategories if present
        cachedCategories = (cachedCategories ?: getCustomHomeCategories())?.map { cat ->
            cat.copy(
                items = cat.items.map { item ->
                    if (item.id == hero.id || item.displayImdbId == hero.displayImdbId || item.name.equals(hero.name, ignoreCase = true)) {
                        item.copy(poster = newImageUrl, background = newImageUrl)
                    } else item
                }
            )
        }
        cachedCategories?.let { saveCustomHomeCategories(it) }
        return updated
    }

    fun saveCustomSlideshow(id: String, images: List<String>) {
        runCatching {
            val json = jsonParser.encodeToString(images)
            userPreferences.saveCustomSlideshow(id, json)
        }
    }

    fun getCustomSlideshow(id: String): List<String>? {
        val savedJson = userPreferences.getCustomSlideshow(id) ?: return null
        return runCatching {
            jsonParser.decodeFromString<List<String>>(savedJson)
        }.getOrNull()
    }

    fun restoreMediaOriginal(id: String, displayImdbId: String) {
        customStreamsMap.remove(id)
        customStreamsMap.remove(displayImdbId)
        customMetaMap.remove(id)
        customMetaMap.remove(displayImdbId)
        val norm = id.trim().lowercase()
        customMetaMap.remove("name_$norm")
        movieDetailsCache.remove(id)
        movieDetailsCache.remove(displayImdbId)
        movieDetailsCache.remove("name_$norm")
        userPreferences.clearCustomStreams(id)
        userPreferences.clearCustomStreams(displayImdbId)
        userPreferences.clearCustomMeta(id)
        userPreferences.clearCustomMeta(displayImdbId)
        userPreferences.clearCustomMeta("name_$norm")
        userPreferences.clearCustomSlideshow(id)
        userPreferences.clearCustomSlideshow(displayImdbId)
        userPreferences.clearCustomSlideshow("name_$norm")
        userPreferences.clearHiddenStreams(id)
        userPreferences.clearHiddenStreams(displayImdbId)
    }

    fun restoreHomeOriginal() {
        cachedCategories = null
        userPreferences.clearCustomHomeCategories()
        userPreferences.clearCustomHomeHeroes()
    }

    fun getPermanentTop8HeroList(): List<StremioCatalogMeta> {
        return listOf(
            StremioCatalogMeta(
                id = "tt3909224",
                name = "The Seven Deadly Sins",
                type = "series",
                poster = "poster_seven_deadly_sins",
                background = "poster_seven_deadly_sins",
                year = "2014",
                imdbRating = "7.8",
                genre = listOf("Anime", "Action", "Adventure"),
                description = "The Seven Deadly Sins are a band of knights in the land of Britannia who disbanded ten years earlier after being framed for plotting a coup against the Liones Kingdom. Princess Elizabeth embarks on a journey to find the Sins to help reclaim her kingdom.",
                videos = com.example.data.model.PreloadedMediaData.getSevenDeadlySinsEpisodes()
            ),
            StremioCatalogMeta(
                id = "tt2632424",
                name = "The Originals",
                type = "series",
                poster = "poster_the_originals",
                background = "poster_the_originals",
                year = "2013",
                imdbRating = "8.3",
                genre = listOf("Drama", "Fantasy", "Horror"),
                description = "A family of power-hungry thousand-year-old vampires look to take back the city that they built and dominate all those who have done them wrong in the supernatural cauldron of New Orleans.",
                videos = com.example.data.model.PreloadedMediaData.getTheOriginalsEpisodes()
            ),
            StremioCatalogMeta(
                id = "tt21209876",
                name = "Solo Leveling",
                type = "series",
                poster = "poster_solo_leveling",
                background = "https://images.metahub.space/background/medium/tt21209876/img",
                year = "2024",
                imdbRating = "8.4",
                genre = listOf("Anime", "Action", "Fantasy"),
                description = "In a world where hunters, humans who possess magical powers, must battle deadly monsters to protect mankind from annihilation, a notoriously weak hunter named Sung Jinwoo finds himself in a seamless struggle for survival inside a mysterious double dungeon.",
                videos = com.example.data.model.PreloadedMediaData.getSoloLevelingEpisodes()
            ),
            StremioCatalogMeta(
                id = "tt0998631",
                name = "2012",
                type = "movie",
                poster = "poster_2012",
                background = "https://images.metahub.space/background/medium/tt0998631/img",
                year = "2009",
                imdbRating = "5.8",
                genre = listOf("Action", "Adventure", "Sci-Fi"),
                description = "Dr. Adrian Helmsley, part of a worldwide geophysical team investigating the effect on Earth of radiation from unprecedented solar storms, learns that the Earth's core is heating up. He warns U.S. President Thomas Wilson that the crust of the planet is becoming unstable, leading to a catastrophic global apocalypse."
            ),
            StremioCatalogMeta(
                id = "tt1663662",
                name = "Pacific Rim",
                type = "movie",
                poster = "poster_pacific_rim",
                background = "https://images.metahub.space/background/medium/tt1663662/img",
                year = "2013",
                imdbRating = "6.9",
                genre = listOf("Action", "Adventure", "Sci-Fi"),
                description = "When legions of monstrous creatures, known as Kaiju, started rising from the sea, a war began that would take millions of lives and consume humanity's resources. To combat the giant Kaiju, human scientists designed colossal robots called Jaegers."
            ),
            StremioCatalogMeta(
                id = "tt0409591",
                name = "Naruto",
                type = "series",
                poster = "poster_naruto",
                background = "https://images.metahub.space/background/medium/tt0409591/img",
                year = "2002",
                imdbRating = "8.4",
                genre = listOf("Anime", "Action", "Adventure"),
                description = "Naruto Uzumaki, a hyperactive and knuckle-headed ninja, lives in Konohagakure, the Hidden Leaf Village. Moments before his birth, a huge demon known as the Nine-Tailed Fox attacked the village and caused havoc, forcing the leader to seal the spirit inside newborn Naruto.",
                videos = com.example.data.model.PreloadedMediaData.getNarutoEpisodes()
            ),
            StremioCatalogMeta(
                id = "tt10430968",
                name = "Jujutsu Kaisen",
                type = "series",
                poster = "poster_jujutsu_kaisen",
                background = "https://images.metahub.space/background/medium/tt10430968/img",
                year = "2020",
                imdbRating = "8.6",
                genre = listOf("Anime", "Action", "Supernatural"),
                description = "Yuji Itadori, a kind-hearted teenager, joins his school's Occult Club for fun, but discovers that its members are actual sorcerers who can manipulate energy between beings for their own use. He swallows a cursed finger to protect his friends and becomes possessed by Ryomen Sukuna, the King of Curses.",
                videos = com.example.data.model.PreloadedMediaData.getJujutsuKaisenEpisodes()
            ),
            StremioCatalogMeta(
                id = "tt0460228",
                name = "A Haunting",
                type = "series",
                poster = "poster_a_haunting",
                background = "https://images.metahub.space/background/medium/tt0460228/img",
                year = "2005",
                imdbRating = "7.9",
                genre = listOf("Horror", "Drama", "Mystery"),
                description = "An American paranormal television series depicting true accounts of terrifying paranormal activity, demonic possessions, haunted locations, and dark supernatural encounters.",
                videos = com.example.data.model.PreloadedMediaData.getAHauntingEpisodes()
            )
        )
    }

    private fun isExcludedFromHeroSlideshow(item: StremioCatalogMeta): Boolean {
        val fullText = "${item.name} ${item.genre?.joinToString(" ")} ${item.description}".lowercase()
        return fullText.contains("documentary") ||
               fullText.contains("biography") ||
               fullText.contains("autobiography") ||
               fullText.contains("docuseries") ||
               fullText.contains("talk-show") ||
               fullText.contains("game-show") ||
               fullText.contains("reality-tv") ||
               fullText.contains("news") ||
               fullText.contains("behind the scenes") ||
               fullText.contains("making of") ||
               item.genre?.any { g ->
                   g.equals("Documentary", ignoreCase = true) ||
                   g.equals("Biography", ignoreCase = true) ||
                   g.equals("History", ignoreCase = true)
               } == true
    }

    private fun heroPriorityScore(item: StremioCatalogMeta): Int {
        var score = 0
        val year = item.year?.toIntOrNull() ?: 2020
        if (year >= 2026) score += 70
        else if (year >= 2025) score += 55
        else if (year >= 2024) score += 40
        else if (year >= 2023) score += 20

        val genres = item.genre?.map { it.lowercase() } ?: emptyList()
        val text = "${item.name} ${item.description}".lowercase()

        // Prioritize thriller, action, horror, and anime
        if (genres.any { it.contains("anime") } || text.contains("anime")) score += 50
        if (genres.any { it.contains("thriller") } || text.contains("thriller")) score += 45
        if (genres.any { it.contains("action") } || text.contains("action")) score += 40
        if (genres.any { it.contains("horror") } || text.contains("horror")) score += 40
        if (genres.any { it.contains("sci-fi") || it.contains("supernatural") || it.contains("mystery") }) score += 25

        val rating = item.imdbRating?.toDoubleOrNull() ?: 0.0
        score += (rating * 3).toInt()
        return score
    }

    fun build20HeroList(categories: List<CategorySection>): List<StremioCatalogMeta> {
        val savedHeroes = getCustomHomeHeroes()
        val baseList: List<StremioCatalogMeta> = if (savedHeroes != null && savedHeroes.isNotEmpty()) {
            savedHeroes
        } else {
            val permanentList = getPermanentTop8HeroList()
            val permanentIds = permanentList.map { it.id }.toSet()

            val dynamicCandidates = mutableListOf<StremioCatalogMeta>()

            // Extract valid dynamic candidates from loaded categories
            // Must NOT be documentary or biography, and must prioritize thriller, action, horror, anime & latest releases
            val poolFromCategories = categories.flatMap { it.items }
                .filter { it.id !in permanentIds && !isExcludedFromHeroSlideshow(it) }
                .distinctBy { it.id }
                .sortedByDescending { heroPriorityScore(it) }

            for (item in poolFromCategories) {
                if (dynamicCandidates.size >= 13) break
                if (dynamicCandidates.none { it.id == item.id || it.name.equals(item.name, ignoreCase = true) }) {
                    val hdCand = if ((item.background.isNullOrBlank() || item.background == item.poster) && item.displayImdbId.startsWith("tt")) {
                        item.copy(background = "https://images.metahub.space/background/medium/${item.displayImdbId}/img")
                    } else item
                    dynamicCandidates.add(hdCand)
                }
            }

            // Fill remaining slots up to 13 using curated latest blockbuster thriller/action/horror/anime hits
            for (fallback in com.example.data.model.PreloadedMediaData.LATEST_ACTION_THRILLER_HORROR_ANIME) {
                if (dynamicCandidates.size >= 13) break
                if (dynamicCandidates.none { it.id == fallback.id || it.name.equals(fallback.name, ignoreCase = true) } &&
                    permanentList.none { it.id == fallback.id || it.name.equals(fallback.name, ignoreCase = true) }) {
                    dynamicCandidates.add(fallback)
                }
            }

            // Exactly 8 permanent + 13 dynamic latest releases = 21 items total
            val fullList = mutableListOf<StremioCatalogMeta>()
            fullList.addAll(permanentList)
            fullList.addAll(dynamicCandidates.take(13))
            fullList
        }

        // Always resolve any custom edits (custom images, names, posters) for all heroes in the list
        return baseList.map { hero ->
            val custom = getCustomMeta(hero.id)
                ?: getCustomMeta(hero.displayImdbId)
                ?: getCustomMetaByName(hero.name)
            if (custom != null) {
                hero.copy(
                    poster = custom.poster?.takeIf { it.isNotBlank() } ?: hero.poster,
                    background = custom.background?.takeIf { it.isNotBlank() } ?: hero.background,
                    name = custom.name.takeIf { it.isNotBlank() } ?: hero.name,
                    description = custom.description ?: hero.description,
                    imdbRating = custom.imdbRating ?: hero.imdbRating,
                    year = custom.year ?: hero.year,
                    videos = custom.videos ?: hero.videos
                )
            } else {
                hero
            }
        }
    }

    fun getCategoriesFlow(forceRefresh: Boolean = false): Flow<List<CategorySection>> = flow {
        val saved = getCustomHomeCategories()
        if (!forceRefresh && !saved.isNullOrEmpty()) {
            cachedCategories = saved
            emit(saved)
            return@flow
        }

        if (!forceRefresh && !cachedCategories.isNullOrEmpty()) {
            emit(cachedCategories!!)
            return@flow
        }

        val accumulated = mutableListOf<CategorySection>()

        val visibleBatch = coroutineScope {
            val nfxMoviesDeferred = async { client.getCatalogItems("movie", "nfx").getOrDefault(emptyList()) }
            val nfxSeriesDeferred = async { client.getCatalogItems("series", "nfx").getOrDefault(emptyList()) }
            val nfxMovies = nfxMoviesDeferred.await()
            val nfxSeries = nfxSeriesDeferred.await()

            val list = mutableListOf<CategorySection>()
            if (nfxMovies.isNotEmpty()) {
                list.add(CategorySection("nfx_movie", "Netflix — Popular Movies & Hits", "Netflix", "movie", nfxMovies))
            }
            if (nfxSeries.isNotEmpty()) {
                list.add(CategorySection("nfx_series", "Netflix — Trending Series & Binge", "Netflix", "series", nfxSeries))
            }
            list
        }

        if (visibleBatch.isNotEmpty()) {
            accumulated.addAll(visibleBatch)
            cachedCategories = accumulated.toList()
            emit(accumulated.toList())
        }

        val batchA = coroutineScope {
            val atpMoviesDeferred = async { client.getCatalogItems("movie", "atp").getOrDefault(emptyList()) }
            val atpSeriesDeferred = async { client.getCatalogItems("series", "atp").getOrDefault(emptyList()) }
            val dnpMoviesDeferred = async { client.getCatalogItems("movie", "dnp").getOrDefault(emptyList()) }
            val dnpSeriesDeferred = async { client.getCatalogItems("series", "dnp").getOrDefault(emptyList()) }

            val atpMovies = atpMoviesDeferred.await()
            val atpSeries = atpSeriesDeferred.await()
            val dnpMovies = dnpMoviesDeferred.await()
            val dnpSeries = dnpSeriesDeferred.await()

            val list = mutableListOf<CategorySection>()
            if (atpMovies.isNotEmpty()) {
                list.add(CategorySection("atp_movie", "Apple TV+ — Award-Winning Cinema", "Apple TV+", "movie", atpMovies))
            }
            if (atpSeries.isNotEmpty()) {
                list.add(CategorySection("atp_series", "Apple TV+ — Acclaimed Original Series", "Apple TV+", "series", atpSeries))
            }
            if (dnpMovies.isNotEmpty()) {
                list.add(CategorySection("dnp_movie", "Disney+ — Premier Blockbusters & Animation", "Disney+", "movie", dnpMovies))
            }
            if (dnpSeries.isNotEmpty()) {
                list.add(CategorySection("dnp_series", "Disney+ — Hit Franchises & Series", "Disney+", "series", dnpSeries))
            }
            list
        }

        if (batchA.isNotEmpty()) {
            accumulated.addAll(batchA)
            cachedCategories = accumulated.toList()
            emit(accumulated.toList())
        }

        val batchB = coroutineScope {
            val ampMoviesDeferred = async { client.getCatalogItems("movie", "amp").getOrDefault(emptyList()) }
            val ampSeriesDeferred = async { client.getCatalogItems("series", "amp").getOrDefault(emptyList()) }
            val hbmMoviesDeferred = async { client.getCatalogItems("movie", "hbm").getOrDefault(emptyList()) }
            val hbmSeriesDeferred = async { client.getCatalogItems("series", "hbm").getOrDefault(emptyList()) }

            val ampMovies = ampMoviesDeferred.await()
            val ampSeries = ampSeriesDeferred.await()
            val hbmMovies = hbmMoviesDeferred.await()
            val hbmSeries = hbmSeriesDeferred.await()

            val list = mutableListOf<CategorySection>()
            if (ampMovies.isNotEmpty()) {
                list.add(CategorySection("amp_movie", "Prime Video — Top Movies & Blockbusters", "Prime Video", "movie", ampMovies))
            }
            if (ampSeries.isNotEmpty()) {
                list.add(CategorySection("amp_series", "Prime Video — Exclusive Prime Series", "Prime Video", "series", ampSeries))
            }
            if (hbmMovies.isNotEmpty()) {
                list.add(CategorySection("hbm_movie", "HBO Max — Box Office Hits & Cinema", "HBO Max", "movie", hbmMovies))
            }
            if (hbmSeries.isNotEmpty()) {
                list.add(CategorySection("hbm_series", "HBO Max — Acclaimed Prestige Drama", "HBO Max", "series", hbmSeries))
            }
            list
        }

        if (batchB.isNotEmpty()) {
            accumulated.addAll(batchB)
            cachedCategories = accumulated.toList()
            emit(accumulated.toList())
        }
    }

    private val permanentHeroMap: Map<String, StremioCatalogMeta> by lazy {
        getPermanentTop8HeroList().associateBy { it.id }
    }

    suspend fun prefetchMovieDetails(items: List<StremioCatalogMeta>) = withContext(Dispatchers.IO) {
        items.take(8).forEach { item ->
            if (!movieDetailsCache.containsKey(item.id)) {
                val res = client.getMovieDetails(item.type, item.displayImdbId)
                res.getOrNull()?.let { movieDetailsCache[item.id] = it }
            }
        }
    }

    fun getCachedMovieDetails(id: String): StremioCatalogMeta? {
        val custom = getCustomMeta(id)
        if (custom != null) return custom
        val perm = permanentHeroMap[id]
        val cached = movieDetailsCache[id]
        if (perm != null) {
            return (cached ?: perm).copy(poster = perm.poster, background = perm.background)
        }
        return cached
    }

    suspend fun getMovieDetails(type: String, id: String): Result<StremioCatalogMeta> {
        val custom = getCustomMeta(id)
        if (custom != null) return Result.success(custom)

        val permItem = permanentHeroMap[id]
        if (movieDetailsCache.containsKey(id)) {
            val cached = movieDetailsCache[id]!!
            if (permItem != null) {
                val locked = cached.copy(poster = permItem.poster, background = permItem.background)
                return Result.success(locked)
            }
            return Result.success(cached)
        }

        val result = client.getMovieDetails(type, id)
        val remote = result.getOrNull()
        if (permItem != null) {
            val finalVideos = if (!remote?.videos.isNullOrEmpty()) remote?.videos else permItem.videos
            val combined = (remote ?: permItem).copy(
                id = permItem.id,
                name = permItem.name,
                type = permItem.type,
                poster = permItem.poster,
                background = permItem.background,
                description = remote?.description?.takeIf { it.isNotBlank() } ?: permItem.description,
                year = remote?.year ?: permItem.year,
                imdbRating = remote?.imdbRating ?: permItem.imdbRating,
                genre = remote?.genre ?: permItem.genre,
                videos = finalVideos
            )
            movieDetailsCache[id] = combined
            return Result.success(combined)
        }

        val enriched = if (remote != null) {
            if (remote.videos.isNullOrEmpty()) {
                val nameLower = remote.name.lowercase()
                when {
                    id == "tt3909224" || nameLower.contains("seven deadly sins") ->
                        remote.copy(videos = com.example.data.model.PreloadedMediaData.getSevenDeadlySinsEpisodes())
                    id == "tt2632424" || nameLower.contains("the originals") ->
                        remote.copy(videos = com.example.data.model.PreloadedMediaData.getTheOriginalsEpisodes())
                    id == "tt21209876" || nameLower.contains("solo leveling") ->
                        remote.copy(videos = com.example.data.model.PreloadedMediaData.getSoloLevelingEpisodes())
                    id == "tt0409591" || nameLower.contains("naruto") ->
                        remote.copy(videos = com.example.data.model.PreloadedMediaData.getNarutoEpisodes())
                    id == "tt10430968" || nameLower.contains("jujutsu kaisen") ->
                        remote.copy(videos = com.example.data.model.PreloadedMediaData.getJujutsuKaisenEpisodes())
                    else -> remote
                }
            } else remote
        } else null

        enriched?.let { movieDetailsCache[id] = it }
        return if (enriched != null) Result.success(enriched) else result
    }

    suspend fun resolveTrailerInfo(meta: StremioCatalogMeta): com.example.data.model.TrailerResolutionInfo = withContext(Dispatchers.IO) {
        client.resolveTrailerDetailed(
            imdbId = meta.displayImdbId,
            title = meta.name,
            year = meta.year,
            type = meta.type,
            metaTrailers = meta.trailers ?: emptyList()
        )
    }

    suspend fun resolveTrailerId(meta: StremioCatalogMeta): String? = withContext(Dispatchers.IO) {
        val info = resolveTrailerInfo(meta)
        info.youtubeId
    }

    fun getStreamsFlow(type: String, id: String): Flow<List<StremioStream>> = flow {
        val custom = getCustomStreamEdits(id)
        if (custom != null) {
            val filtered = custom.filterNot { isStreamHidden(id, it) }
            emit(filtered)
            return@flow
        }
        val providers = userPreferences.getProviders()
        client.getStreamsProgressive(
            type = type,
            id = id,
            providers = providers
        ).collect { streams ->
            val updatedCustom = getCustomStreamEdits(id)
            if (updatedCustom != null) {
                val filtered = updatedCustom.filterNot { isStreamHidden(id, it) }
                emit(filtered)
            } else {
                val filtered = streams.filterNot { isStreamHidden(id, it) }
                emit(filtered)
            }
        }
    }

    fun getStreamsProgressive(type: String, id: String): Flow<List<StremioStream>> = getStreamsFlow(type, id)

    suspend fun search(query: String, type: String = "movie"): Result<List<StremioCatalogMeta>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        val isAll = type.equals("all", ignoreCase = true)
        val isSeries = type.equals("series", ignoreCase = true)

        // 1. Search locally cached Netflix & streaming catalog items
        val catalogPool = (cachedCategories ?: getCustomHomeCategories() ?: emptyList())
            .flatMap { it.items }
            .filter { item ->
                val typeMatch = isAll || item.type.equals(type, ignoreCase = true)
                val text = "${item.name} ${item.genre?.joinToString(" ")} ${item.description}".lowercase()
                typeMatch && (text.contains(cleanQuery) || cleanQuery.split(" ").all { text.contains(it) })
            }

        // 2. Search permanent heroes & latest pool
        val staticPool = (getPermanentTop8HeroList() + com.example.data.model.PreloadedMediaData.LATEST_ACTION_THRILLER_HORROR_ANIME)
            .filter { item ->
                val typeMatch = isAll || item.type.equals(type, ignoreCase = true)
                val text = "${item.name} ${item.genre?.joinToString(" ")} ${item.description}".lowercase()
                typeMatch && (text.contains(cleanQuery) || cleanQuery.split(" ").all { text.contains(it) })
            }

        // 3. Search remote Cinemeta & TMDB catalogs
        val remoteResults = if (isAll) {
            coroutineScope {
                val moviesDeferred = async { client.searchCatalog(query, "movie").getOrDefault(emptyList()) }
                val seriesDeferred = async { client.searchCatalog(query, "series").getOrDefault(emptyList()) }
                (moviesDeferred.await() + seriesDeferred.await())
            }
        } else {
            client.searchCatalog(query, type).getOrDefault(emptyList())
        }

        val merged = (staticPool + catalogPool + remoteResults)
            .filter { if (isAll) true else it.type.equals(type, ignoreCase = true) }
            .distinctBy { it.displayImdbId }
            .sortedWith(
                compareByDescending<StremioCatalogMeta> {
                    val n = it.name.lowercase().trim()
                    when {
                        n == cleanQuery -> 100
                        n.startsWith(cleanQuery) -> 80
                        cleanQuery.contains("seven deadly") || cleanQuery.contains("7 deadly") -> {
                            if (n.contains("seven deadly sins")) 95 else 20
                        }
                        n.contains(cleanQuery) -> 60
                        else -> 10
                    }
                }.thenByDescending { it.imdbRating?.toDoubleOrNull() ?: 0.0 }
            )

        Result.success(merged)
    }

    fun observeSavedMedia(sortOrder: String = "custom"): Flow<List<SavedMediaEntity>> {
        return when {
            sortOrder.endsWith("_custom") || sortOrder == "custom" -> savedDao.getAllByCustomOrder()
            sortOrder == "date_desc" -> savedDao.getAllByDateDesc()
            sortOrder == "date_asc" -> savedDao.getAllByDateAsc()
            sortOrder == "title_asc" -> savedDao.getAllByTitleAsc()
            sortOrder == "rating_desc" -> savedDao.getAllByRatingDesc()
            sortOrder == "rating_asc" -> savedDao.getAllByRatingAsc()
            else -> savedDao.getAllByCustomOrder()
        }
    }

    fun isSaved(id: String, displayImdbId: String = id, title: String = ""): Flow<Boolean> =
        savedDao.isSavedFlow(id, displayImdbId, title)

    suspend fun toggleSave(meta: StremioCatalogMeta): Boolean {
        val exists = savedDao.exists(meta.id, meta.displayImdbId, meta.name)
        if (exists) {
            savedDao.deleteByIdOrTitle(meta.id, meta.displayImdbId, meta.name)
            return false
        } else {
            savedDao.shiftCustomOrderIndices()
            saveMedia(meta, 0)
            return true
        }
    }

    suspend fun saveMedia(meta: StremioCatalogMeta, customOrder: Int = 0) {
        movieDetailsCache[meta.id] = meta
        movieDetailsCache[meta.displayImdbId] = meta
        val entity = SavedMediaEntity(
            id = meta.id,
            title = meta.name,
            type = meta.type,
            poster = meta.poster,
            backdrop = meta.background,
            year = meta.year,
            imdbRating = meta.imdbRating,
            description = meta.description,
            genres = meta.genre?.joinToString(", "),
            customOrderIndex = customOrder,
            addedTimestamp = System.currentTimeMillis()
        )
        savedDao.insert(entity)
    }

    suspend fun deleteSaved(id: String) {
        savedDao.deleteById(id)
    }

    suspend fun updateCustomOrder(items: List<SavedMediaEntity>) {
        val updated = items.mapIndexed { index, entity ->
            entity.copy(customOrderIndex = index)
        }
        savedDao.updateAll(updated)
    }
}

package com.example.data.model

object PreloadedMediaData {

    /**
     * Complete season and episode catalogue for The Seven Deadly Sins (Nanatsu no Taizai)
     * IMDb tt3909224 — 4 full seasons with 24 episodes each (96 episodes total)
     */
    fun getSevenDeadlySinsEpisodes(): List<EpisodeItem> {
        val list = mutableListOf<EpisodeItem>()

        val s1Titles = listOf(
            "The Seven Deadly Sins", "The Sword of the Holy Knight", "The Sin in the Sleeping Forest",
            "A Pure Maiden's Dream", "Even If You Should Die", "The Poem of Beginnings",
            "A Touching Reunion", "The Fearsome Pursuer", "Dark Pulse",
            "The Vaizel Fight Festival", "A Long-Held Feeling", "The Terrifying Cannon",
            "The Apostle of Destruction", "A Reader of Books", "Unholy Knight",
            "The Despaired Legends", "The First Sacrifice", "Even If I Give My Life For It",
            "The Fairy King Waited in Vain", "The Courageous Charm", "The Imminent Threat",
            "What I Can Do For You", "Despair Descends", "The Heroes"
        )

        val s2Titles = listOf(
            "Revival of the Commandments", "Existence and Proof", "Sacred Treasure Lostvayne",
            "The Ten Commandments on the Move", "Overwhelming Violence", "The Great Holy Knight Atones",
            "Where Memories Fall", "The Sacred Maiden of the Forest", "What Lies Ahead",
            "The Maze of Death", "Those Who Gather", "The Great Fight Festival Begins",
            "A Blood-Stained Destiny", "The Master of the Sun", "A Dark Reunion",
            "Death of the Captain", "The Sun Returned", "Meliodas Awakes",
            "Love Is a Maiden's Power", "Hope and Despair", "Certain Death",
            "The Two Knights", "The Hero Returns", "To Our Captive Friend"
        )

        val s3Titles = listOf(
            "Imperial Wrath of the Gods", "Memories of the Holy War", "Let There Be Light",
            "The Ten Commandments vs. The Four Archangels", "Emotional Maelstrom", "We Call That Love",
            "Deadly Sins Unite!", "The Devastated Puppet", "Cursed Lovers",
            "The Way We Live", "The Odious Resentment Does Not Sleep", "Love Is the Maiden's Power",
            "The Almighty vs. The Greatest Evil", "A New Threat", "To Our Captive Friend",
            "The Salvation of Britannia", "The Voice Calling Your Name", "The King Sings Alone",
            "The Struggle", "Mortal Enemies", "What the Witch Had Always Wanted",
            "A Taste of Chaos", "An Everlasting Kingdom", "The Final Departure"
        )

        val s4Titles = listOf(
            "From Purgatory", "Encounter with the Unknown", "A Sincere Feeling",
            "The Victims of the Holy War", "The Tragedy of the Archangel", "The End of the Long Holy War",
            "The Door to Hope", "The Door to Despair", "That Which We Lack",
            "The Sun That Never Sets", "The Fate of the Brothers", "The Final Battle",
            "The Voice Calling Your Name", "Farewell, Seven Deadly Sins", "Fated Brothers",
            "The Final Battle for Britannia", "The Song of the Demon King", "Escanor's Last Stand",
            "The Ultimate Sacrifice", "Britannia Reborn", "What Merlin Sought",
            "Arthur, King of Chaos", "An Everlasting Realm", "Heirs"
        )

        for ((idx, title) in s1Titles.withIndex()) {
            val num = idx + 1
            list.add(
                EpisodeItem(
                    id = "tt3909224:1:$num",
                    name = "Episode $num: $title",
                    season = 1,
                    number = num,
                    overview = "Meliodas and Elizabeth journey across Britannia in search of the scattered Seven Deadly Sins.",
                    thumbnail = "https://episodes.metahub.space/tt3909224/1/$num/w780.jpg",
                    rating = "8.1",
                    released = "2014"
                )
            )
        }

        for ((idx, title) in s2Titles.withIndex()) {
            val num = idx + 1
            list.add(
                EpisodeItem(
                    id = "tt3909224:2:$num",
                    name = "Episode $num: $title",
                    season = 2,
                    number = num,
                    overview = "The Ten Commandments awaken from their ancient seal, plunging the realm into war.",
                    thumbnail = "https://episodes.metahub.space/tt3909224/2/$num/w780.jpg",
                    rating = "8.3",
                    released = "2018"
                )
            )
        }

        for ((idx, title) in s3Titles.withIndex()) {
            val num = idx + 1
            list.add(
                EpisodeItem(
                    id = "tt3909224:3:$num",
                    name = "Episode $num: $title",
                    season = 3,
                    number = num,
                    overview = "The tragic truth of the ancient Holy War and the curse of Meliodas and Elizabeth is revealed.",
                    thumbnail = "https://episodes.metahub.space/tt3909224/3/$num/w780.jpg",
                    rating = "8.0",
                    released = "2019"
                )
            )
        }

        for ((idx, title) in s4Titles.withIndex()) {
            val num = idx + 1
            list.add(
                EpisodeItem(
                    id = "tt3909224:4:$num",
                    name = "Episode $num: $title",
                    season = 4,
                    number = num,
                    overview = "The epic climax against the Demon King and the awakening of Chaos.",
                    thumbnail = "https://episodes.metahub.space/tt3909224/4/$num/w780.jpg",
                    rating = "8.2",
                    released = "2021"
                )
            )
        }

        return list
    }

    /**
     * The Originals (tt2632424) — 5 complete seasons
     */
    fun getTheOriginalsEpisodes(): List<EpisodeItem> {
        val list = mutableListOf<EpisodeItem>()
        val seasonCounts = listOf(22, 22, 22, 13, 13)
        for ((sIdx, count) in seasonCounts.withIndex()) {
            val seasonNum = sIdx + 1
            for (ep in 1..count) {
                list.add(
                    EpisodeItem(
                        id = "tt2632424:$seasonNum:$ep",
                        name = "Season $seasonNum Episode $ep",
                        season = seasonNum,
                        number = ep,
                        overview = "The Mikaelson family fights to reclaim and protect their rule in the supernatural heart of New Orleans.",
                        thumbnail = "https://episodes.metahub.space/tt2632424/$seasonNum/$ep/w780.jpg",
                        rating = "8.4",
                        released = (2013 + sIdx).toString()
                    )
                )
            }
        }
        return list
    }

    /**
     * Solo Leveling (tt21209876) — Season 1
     */
    fun getSoloLevelingEpisodes(): List<EpisodeItem> {
        val epTitles = listOf(
            "I'm Used to It", "If I Had One More Chance", "It's Like a Game",
            "I've Gotta Get Stronger", "A Pretty Good Deal", "The Real Hunt Begins",
            "Let's See Where This Goes", "This Is Frustrating", "You've Been Hiding Your Skills",
            "What Is This, a Picnic?", "A Knight Who Defends an Empty Throne", "Arise"
        )
        return epTitles.mapIndexed { idx, title ->
            val num = idx + 1
            EpisodeItem(
                id = "tt21209876:1:$num",
                name = "Episode $num: $title",
                season = 1,
                number = num,
                overview = "Sung Jinwoo discovers an unprecedented system allowing him to level up his abilities boundlessly.",
                thumbnail = "https://episodes.metahub.space/tt21209876/1/$num/w780.jpg",
                rating = "8.8",
                released = "2024"
            )
        }
    }

    /**
     * Naruto (tt0409591)
     */
    fun getNarutoEpisodes(): List<EpisodeItem> {
        val list = mutableListOf<EpisodeItem>()
        for (s in 1..5) {
            for (ep in 1..26) {
                list.add(
                    EpisodeItem(
                        id = "tt0409591:$s:$ep",
                        name = "Season $s • Episode $ep",
                        season = s,
                        number = ep,
                        overview = "Naruto Uzumaki trains alongside Sasuke and Sakura under Kakashi-sensei.",
                        thumbnail = "https://episodes.metahub.space/tt0409591/$s/$ep/w780.jpg",
                        rating = "8.4",
                        released = "2002"
                    )
                )
            }
        }
        return list
    }

    /**
     * Jujutsu Kaisen (tt10430968)
     */
    fun getJujutsuKaisenEpisodes(): List<EpisodeItem> {
        val list = mutableListOf<EpisodeItem>()
        for (ep in 1..24) {
            list.add(
                EpisodeItem(
                    id = "tt10430968:1:$ep",
                    name = "Season 1 • Episode $ep",
                    season = 1,
                    number = ep,
                    overview = "Yuji Itadori enters Tokyo Jujutsu High to learn the control of cursed energy.",
                    thumbnail = "https://episodes.metahub.space/tt10430968/1/$ep/w780.jpg",
                    rating = "8.6",
                    released = "2020"
                )
            )
        }
        for (ep in 1..23) {
            list.add(
                EpisodeItem(
                    id = "tt10430968:2:$ep",
                    name = "Season 2: Shibuya Incident • Episode $ep",
                    season = 2,
                    number = ep,
                    overview = "The monumental Shibuya Incident begins as Gojo Satoru confronts the cursed spirits.",
                    thumbnail = "https://episodes.metahub.space/tt10430968/2/$ep/w780.jpg",
                    rating = "9.1",
                    released = "2023"
                )
            )
        }
        return list
    }

    /**
     * A Haunting (tt0460228)
     */
    fun getAHauntingEpisodes(): List<EpisodeItem> {
        val list = mutableListOf<EpisodeItem>()
        for (s in 1..4) {
            for (ep in 1..10) {
                list.add(
                    EpisodeItem(
                        id = "tt0460228:$s:$ep",
                        name = "Season $s • Episode $ep",
                        season = s,
                        number = ep,
                        overview = "Chilling eyewitness paranormal accounts and terrifying supernatural encounters.",
                        thumbnail = "https://episodes.metahub.space/tt0460228/$s/$ep/w780.jpg",
                        rating = "7.9",
                        released = "2005"
                    )
                )
            }
        }
        return list
    }

    /**
     * 13 Latest Blockbuster Releases (2024-2026) prioritizing Thriller, Action, Horror, and Anime!
     * Strictly NO documentaries, NO biographies, NO autobiographies.
     */
    val LATEST_ACTION_THRILLER_HORROR_ANIME: List<StremioCatalogMeta> = listOf(
        StremioCatalogMeta(
            id = "tt27995113",
            name = "Black Doves",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt27995113/img",
            background = "https://images.metahub.space/background/medium/tt27995113/img",
            year = "2024",
            imdbRating = "7.3",
            genre = listOf("Action", "Thriller", "Drama"),
            description = "A spy embarking on a passionate affair faces danger when her lover is killed in London's dangerous underworld."
        ),
        StremioCatalogMeta(
            id = "tt15435876",
            name = "The Penguin",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt15435876/img",
            background = "https://images.metahub.space/background/medium/tt15435876/img",
            year = "2024",
            imdbRating = "8.8",
            genre = listOf("Crime", "Drama", "Thriller"),
            description = "Following the events of The Batman, Oswald Cobblepot fights to seize control of Gotham City's criminal underworld."
        ),
        StremioCatalogMeta(
            id = "tt11126994",
            name = "Arcane",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt11126994/img",
            background = "https://images.metahub.space/background/medium/tt11126994/img",
            year = "2024",
            imdbRating = "9.0",
            genre = listOf("Animation", "Action", "Sci-Fi"),
            description = "Set in the utopian region of Piltover and the oppressed underground of Zaun, the story follows the origins of two iconic League champions."
        ),
        StremioCatalogMeta(
            id = "tt18412256",
            name = "Alien: Romulus",
            type = "movie",
            poster = "https://images.metahub.space/poster/medium/tt18412256/img",
            background = "https://images.metahub.space/background/medium/tt18412256/img",
            year = "2024",
            imdbRating = "7.2",
            genre = listOf("Horror", "Sci-Fi", "Thriller"),
            description = "While scavenging the deep ends of a derelict space station, a group of young space colonists come face to face with the most terrifying life form in the universe."
        ),
        StremioCatalogMeta(
            id = "tt6263850",
            name = "Deadpool & Wolverine",
            type = "movie",
            poster = "https://images.metahub.space/poster/medium/tt6263850/img",
            background = "https://images.metahub.space/background/medium/tt6263850/img",
            year = "2024",
            imdbRating = "7.7",
            genre = listOf("Action", "Comedy", "Sci-Fi"),
            description = "Deadpool's peaceful existence comes crashing down when the Time Variance Authority recruits him to help safeguard the multiverse."
        ),
        StremioCatalogMeta(
            id = "tt10919420",
            name = "Squid Game",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt10919420/img",
            background = "https://images.metahub.space/background/medium/tt10919420/img",
            year = "2024",
            imdbRating = "8.0",
            genre = listOf("Action", "Mystery", "Thriller"),
            description = "Hundreds of cash-strapped players accept a strange invitation to compete in children's games with deadly high stakes."
        ),
        StremioCatalogMeta(
            id = "tt9335498",
            name = "Demon Slayer: Kimetsu no Yaiba",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt9335498/img",
            background = "https://images.metahub.space/background/medium/tt9335498/img",
            year = "2024",
            imdbRating = "8.6",
            genre = listOf("Anime", "Action", "Fantasy"),
            description = "Tanjiro Kamado trains rigorously alongside the Hashira to prepare for the final confrontation against Muzan Kibutsuji."
        ),
        StremioCatalogMeta(
            id = "tt2788316",
            name = "Shōgun",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt2788316/img",
            background = "https://images.metahub.space/background/medium/tt2788316/img",
            year = "2024",
            imdbRating = "8.7",
            genre = listOf("Action", "Adventure", "Drama"),
            description = "When a mysterious European ship is found marooned in a nearby fishing village, Lord Yoshii Toranaga discovers secrets that could tip the scales of power."
        ),
        StremioCatalogMeta(
            id = "tt29268110",
            name = "Smile 2",
            type = "movie",
            poster = "https://images.metahub.space/poster/medium/tt29268110/img",
            background = "https://images.metahub.space/background/medium/tt29268110/img",
            year = "2024",
            imdbRating = "7.0",
            genre = listOf("Horror", "Mystery", "Thriller"),
            description = "About to embark on a world tour, global pop sensation Skye Riley begins experiencing increasingly terrifying and inexplicable events."
        ),
        StremioCatalogMeta(
            id = "tt23289160",
            name = "Godzilla Minus One",
            type = "movie",
            poster = "https://images.metahub.space/poster/medium/tt23289160/img",
            background = "https://images.metahub.space/background/medium/tt23289160/img",
            year = "2024",
            imdbRating = "7.8",
            genre = listOf("Action", "Adventure", "Horror"),
            description = "Post war Japan is at its lowest point when a colossal, radioactive monster emerges to wreak catastrophic devastation."
        ),
        StremioCatalogMeta(
            id = "tt12637874",
            name = "Fallout",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt12637874/img",
            background = "https://images.metahub.space/background/medium/tt12637874/img",
            year = "2024",
            imdbRating = "8.4",
            genre = listOf("Action", "Adventure", "Sci-Fi"),
            description = "In a future, post-apocalyptic Los Angeles brought about by nuclear decimation, citizens must live in underground bunkers to protect themselves."
        ),
        StremioCatalogMeta(
            id = "tt13616990",
            name = "Chainsaw Man",
            type = "series",
            poster = "https://images.metahub.space/poster/medium/tt13616990/img",
            background = "https://images.metahub.space/background/medium/tt13616990/img",
            year = "2024",
            imdbRating = "8.4",
            genre = listOf("Anime", "Action", "Horror"),
            description = "Following a betrayal, young Denji fuses with his pet devil Pochita to become Chainsaw Man, a devil-hunting force."
        ),
        StremioCatalogMeta(
            id = "tt15239678",
            name = "Dune: Part Two",
            type = "movie",
            poster = "https://images.metahub.space/poster/medium/tt15239678/img",
            background = "https://images.metahub.space/background/medium/tt15239678/img",
            year = "2024",
            imdbRating = "8.5",
            genre = listOf("Action", "Adventure", "Sci-Fi"),
            description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family."
        )
    )
}

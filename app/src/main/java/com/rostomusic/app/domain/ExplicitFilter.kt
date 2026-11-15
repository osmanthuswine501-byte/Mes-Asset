package com.rostomusic.app.domain

/**
 * Masks swear words in displayed lyrics, keeping the first letter: "merde" becomes "m****".
 * French and English roots; a root marked with * also matches longer words that start with it.
 */
object ExplicitFilter {
    private val ROOTS = listOf(
        // English
        "fuck*", "motherfuck*", "shit*", "bullshit", "bitch*", "asshole*", "cunt*", "dick", "dicks", "pussy", "pussies",
        "nigga*", "nigger*", "whore*", "slut*", "bastard*", "cock", "cocks", "hoe", "hoes", "dickhead*", "twat*", "wanker*",
        // French
        "putain*", "pute", "putes", "merde*", "merdique*", "connard*", "connasse*", "salope*", "salaud*", "enculé*", "encule*",
        "bâtard*", "batard*", "niquer", "nique", "niques", "niquez", "niqué*", "ntm", "fdp", "bite", "bites", "chatte*",
        "couille*", "enfoiré*", "enfoire*", "foutre", "pd", "tapette*", "pétasse*", "petasse*", "bouffon*", "trouduc*",
    )

    private val pattern: Regex = run {
        val parts = ROOTS.map { root ->
            val stem = Regex.escape(root.removeSuffix("*"))
            if (root.endsWith("*")) "$stem\\p{L}*" else stem
        }
        Regex("(?<![\\p{L}])(?:${parts.joinToString("|")})(?![\\p{L}])", setOf(RegexOption.IGNORE_CASE))
    }

    fun mask(text: String): String =
        if (text.isEmpty()) text
        else pattern.replace(text) { m -> m.value.first() + "*".repeat(m.value.length - 1) }

    fun mask(lyrics: Lyrics): Lyrics = lyrics.copy(
        lines = lyrics.lines.map { line ->
            line.copy(text = mask(line.text), words = line.words.map { it.copy(text = mask(it.text)) })
        },
    )
}

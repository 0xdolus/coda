package com.coda.music.util

private val NOISE_PATTERNS = listOf(
    Regex("""\(?\[?official\s*(music\s*)?video\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\(?\[?official\s*audio\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\(?\[?lyrics?\s*(video)?\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\(?\[?audio\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\(?\[?hd\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\(?\[?4k\)?\]?""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*topic$""", RegexOption.IGNORE_CASE),
    Regex("""\s*\|\s*.*$"""),
)

private val BRACKET_NOISE = Regex("""[\(\[][^\)\]]*[\)\]]""")

fun String.toCleanTrackTitle(): String {
    var cleaned = this
    NOISE_PATTERNS.forEach { pattern -> cleaned = cleaned.replace(pattern, "") }
    cleaned = cleaned.replace(BRACKET_NOISE, "")
    return cleaned
        .replace(Regex("""\s{2,}"""), " ")
        .trim(' ', '-', '|', ':')
        .trim()
}

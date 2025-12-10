// Main.kt
// Kotlin Morse Code Translator

import java.time.*

data class Translation(
    val timestamp: Long,
    val mode: Mode,
    val input: String,
    val output: String
)

enum class Mode { ENCODE, DECODE }

class MorseTranslator {

    // Immutable base map
    private val baseMap: Map<Char, String> = mapOf(
        'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
        'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
        'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
        'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
        'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
        'Y' to "-.--",  'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
        '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
        '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '!' to "-.-.--", '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-",
        '&' to ".-...",  ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
        '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
        '$' to "...-..-", '@' to ".--.-.", ' ' to "/"
    )

    // Mutable Morse map
    private val morseMap: MutableMap<Char, String> = baseMap.toMutableMap()

    // Cached inverse map (rebuilt when mappings change)
    private var inverseMap: MutableMap<String, Char> = buildInverseMap()

    private fun buildInverseMap(): MutableMap<String, Char> =
        morseMap.entries.associate { it.value to it.key }.toMutableMap()
    // History of translations
    private var history: MutableList<Translation> = mutableListOf()

    fun encodeToMorse(input: String): String {
        val result = input
            .uppercase()
            .map { ch -> morseMap[ch] ?: "?" }
            .joinToString(" ")

        history.add(Translation(System.currentTimeMillis(), Mode.ENCODE, input, result))
        return result
    }

    fun decodeFromMorse(input: String): String {
        val tokens = input.trim().split(Regex("\\s+"))
        val sb = StringBuilder()

        for (token in tokens) {
            if (token == "/") {
                sb.append(' ')
                continue
            }
            sb.append(inverseMap[token] ?: '?')
        }

        val output = sb.toString()
        history.add(Translation(System.currentTimeMillis(), Mode.DECODE, input, output))
        return output
    }

    fun addOrUpdateMapping(char: Char, morse: String) {
        val key = char.uppercaseChar()

        // Validate morse format
        if (!morse.matches(Regex("[.-]+"))) {
            throw IllegalArgumentException("Morse sequence can only contain '.' and '-'")
        }

        morseMap[key] = morse
        inverseMap = buildInverseMap()
    }

    fun removeMapping(char: Char): Boolean {
        val key = char.uppercaseChar()
        if (key == ' ') return false

        val removed = morseMap.remove(key) != null
        if (removed) inverseMap = buildInverseMap()
        return removed
    }

    fun currentMapping(): Map<Char, String> = morseMap.toMap()
    fun getHistory(): List<Translation> = history.toList()
    fun clearHistory() { history.clear() }
}

// ------------------ UI Helpers ------------------

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

fun printHeader() {
    println("====================================")
    println("   Kotlin Morse Code Translator")
    println("====================================")
}

fun printMenu() {
    println()
    println("Menu:")
    println("1) Encode text → Morse")
    println("2) Decode Morse → text")
    println("3) Show mappings sample")
    println("4) Add / Update mapping")
    println("5) Remove mapping")
    println("6) Show history")
    println("7) Clear history")
    println("0) Exit")
    print("Choose an option: ")
}

fun sampleMappings(map: Map<Char, String>, limit: Int = 20) {
    println("Mappings (sample):")
    map.entries
        .sortedBy { it.key }
        .take(limit)
        .forEach { (k, v) -> println("  '$k' -> $v") }

    println("Total mappings: ${map.size}")
}

// ------------------ MAIN ------------------

fun main() {
    val translator = MorseTranslator()
    val appVersion = "1.1"
    var running = true

    printHeader()
    println("Version: $appVersion")

    while (running) {
        printMenu()
        when (readLine()?.trim()) {
            "1" -> {
                print("Enter text: ")
                println("Morse: ${translator.encodeToMorse(readLine().orEmpty())}")
            }
            "2" -> {
                println("Enter Morse (tokens with spaces, '/' between words):")
                print("Morse: ")
                println("Text: ${translator.decodeFromMorse(readLine().orEmpty())}")
            }
            "3" -> {
                sampleMappings(translator.currentMapping())
            }
            "4" -> {
                print("Enter character: ")
                val input = readLine().orEmpty()
                if (input.length != 1) {
                    println("Provide exactly one character.")
                } else {
                    print("Enter Morse sequence: ")
                    val morse = readLine().orEmpty().trim()
                    try {
                        translator.addOrUpdateMapping(input[0], morse)
                        println("Mapping updated.")
                    } catch (e: IllegalArgumentException) {
                        println("Error: ${e.message}")
                    }
                }
            }
            "5" -> {
                print("Remove mapping for char: ")
                val input = readLine().orEmpty()
                if (input.length != 1) {
                    println("Provide exactly one character.")
                } else {
                    val ok = translator.removeMapping(input[0])
                    println(if (ok) "Removed." else "Mapping not found or protected.")
                }
            }
            "6" -> {
                val hist = translator.getHistory()
                if (hist.isEmpty()) {
                    println("History empty.")
                } else {
                    println("History:")
                    hist.forEachIndexed { i, t ->
                        println("${i + 1}) [${t.timestamp.toLocalDateTime()}] ${t.mode} | \"${t.input}\" -> \"${t.output}\"")
                    }
                }
            }
            "7" -> {
                translator.clearHistory()
                println("History cleared.")
            }
            "0" -> {
                println("Goodbye!")
                running = false
            }
            else -> println("Invalid option.")
        }
    }
}

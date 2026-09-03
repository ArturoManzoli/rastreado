package io.github.arturomanzoli.rastreado.domain

/**
 * Recognises the tracking-code shapes an AliExpress parcel bound for Brazil actually carries.
 *
 * Two matter in practice: the AliExpress/Cainiao forwarder code the buyer is given up front (two
 * letters such as `LP` or `AP` and a 14-digit run) and the UPU S10 code the last-mile carrier
 * assigns once the parcel lands (two letters, nine digits, then the `BR` country suffix).
 */
object TrackingCode {

    private val S10 = Regex("""\b([A-Z]{2}\d{9}[A-Z]{2})\b""")
    private val LOGISTICS_ID = Regex("""\b([A-Z]{2}\d{12,16})\b""")
    private val ALIEXPRESS = Regex("""\b(AE[A-Z0-9]{10,24})\b""")
    private val GENERIC = Regex("""\b([A-Z]{2,3}\d{8,20}|\d{13,20})\b""")

    /** The shapes a carrier really issues, as opposed to anything code-like on a screen. */
    private val KNOWN_SHAPES = listOf(S10, LOGISTICS_ID, ALIEXPRESS)

    /** Ordered most specific first so the best candidate wins. */
    private val PATTERNS = KNOWN_SHAPES + GENERIC

    /** Uppercases and drops the separators a label or a human might introduce. */
    fun normalize(raw: String): String =
        raw.uppercase().filter { it.isLetterOrDigit() }

    fun isPlausible(raw: String): Boolean {
        val code = normalize(raw)
        if (code.length !in 8..30) return false
        return PATTERNS.any { it.matches(code) }
    }

    /**
     * True when [raw] is a shape a carrier issues, rather than only matching the catch-all.
     *
     * Lets a reader tell a code it read cleanly from one it merely found code-like, which is the
     * difference between `LP00838460055517` and the `LPO0838460055517` a misread `0` produces.
     */
    fun isKnownShape(raw: String): Boolean {
        val code = normalize(raw)
        return KNOWN_SHAPES.any { it.matches(code) }
    }

    /**
     * Every distinct code found in [text], most specific shape first.
     *
     * Word boundaries are applied to the text as written, so a code split across lines is not
     * joined up; that is deliberate, since joining unrelated lines invents codes that were never
     * on screen.
     */
    fun findAll(text: String): List<String> {
        val haystack = text.uppercase()
        return PATTERNS
            .flatMap { pattern -> pattern.findAll(haystack).map { it.groupValues[1] }.toList() }
            .distinct()
    }

    fun findFirst(text: String): String? = findAll(text).firstOrNull()

    /**
     * Pulls the last-mile code out of Cainiao's `realMailNo`, which arrives as a localised label
     * and the code separated by a tab, for example
     * `"Último número de rastreamento:\tNN368369905BR"`.
     */
    fun extractLastMileCode(realMailNo: String?): String? {
        val text = realMailNo?.takeIf { it.isNotBlank() } ?: return null
        return findAll(text).firstOrNull()
    }
}

package io.github.arturomanzoli.rastreado.domain

/**
 * Recovers a tracking code and a product name from the text recognised in a photo of an
 * AliExpress order screen.
 */
object OcrFieldExtractor {

    data class Result(
        val code: String? = null,
        val productName: String? = null,
    )

    /** Where the code and the name appear to be, for the user to confirm or drag elsewhere. */
    data class Suggestion(val code: PhotoRegion, val name: PhotoRegion)

    /**
     * Glyphs a text recogniser confuses inside an otherwise numeric run. Every repair is kept only
     * where it improves the match, so a correctly-read code is never rewritten.
     */
    private val DIGIT_REPAIRS = mapOf('O' to '0', 'Q' to '0', 'D' to '0', 'I' to '1', 'L' to '1', 'S' to '5', 'B' to '8')

    /** Chrome that surrounds the product title on an order screen, in both languages. */
    private val LABEL_NOISE = listOf(
        "rastrear", "rastreamento", "pedido", "pedidos", "detalhes", "entrega", "entregue",
        "comprar novamente", "avaliar", "ajuda", "loja", "frete", "total", "quantidade",
        "confirmar", "cancelar", "devolucao", "reembolso", "pagamento", "endereco",
        "track", "tracking", "order", "details", "buy again", "review", "help", "store",
        "shipping", "total", "quantity", "confirm", "cancel", "return", "refund", "address",
    )

    private val PRICE = Regex("""(r\$|us\$|\$|€)\s*\d""", RegexOption.IGNORE_CASE)

    fun extract(recognisedText: String): Result {
        val lines = recognisedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val codes = findCodes(recognisedText)
        return Result(
            code = codes.firstOrNull(),
            productName = findProductName(lines, codes),
        )
    }

    /** The text of the recognised lines [region] covers, wrapped lines joined into one string. */
    fun textIn(lines: List<OcrLine>, region: PhotoRegion): String =
        lines
            .filter { region.coversRow(it.bounds) }
            .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
            .joinToString(" ") { it.text.trim() }
            .trim()

    /** The first tracking code in [text], repairing confusable glyphs only if none is found. */
    fun codeIn(text: String): String? = findCodes(text).firstOrNull()

    fun nameIn(text: String): String? =
        text.take(MAX_NAME_LENGTH).trim().takeIf { it.isNotBlank() }

    /**
     * Aims a region at each field using the same heuristics the whole-photo read used, so the
     * usual case is confirming two boxes rather than drawing them. Falls back to two bands in the
     * middle of the photo when a field is not found, which is still somewhere to drag from.
     */
    fun suggestRegions(lines: List<OcrLine>): Suggestion {
        val found = extract(lines.joinToString("\n") { it.text })
        return Suggestion(
            code = lineHolding(lines, found.code)?.bounds?.grownBy(CODE_PADDING, ROW_PADDING)
                ?: PhotoRegion.of(left = 0.1f, top = 0.35f, width = 0.8f, height = 0.1f),
            name = lineHolding(lines, found.productName)?.bounds?.grownBy(0f, ROW_PADDING)
                ?: PhotoRegion.of(left = 0.1f, top = 0.55f, width = 0.8f, height = 0.14f),
        )
    }

    /** Matches on the repaired text too, since a repaired code never appears in the raw line. */
    private fun lineHolding(lines: List<OcrLine>, value: String?): OcrLine? {
        val needle = value?.takeIf { it.isNotBlank() } ?: return null
        return lines.firstOrNull { needle in it.text } ?: lines.firstOrNull {
            needle in TrackingCode.normalize(repairDigits(it.text))
        }
    }

    private fun findCodes(text: String): List<String> {
        val direct = TrackingCode.findAll(text).map(::repairedToKnownShape).distinct()
        if (direct.isNotEmpty()) return direct
        return TrackingCode.findAll(repairDigits(text))
    }

    /**
     * Rewrites the confusable glyphs in a candidate's digit run, keeping the result only when it
     * lands on a shape a carrier issues.
     *
     * A misread digit next to the letter prefix, such as the `O` in `LPO0838460055517`, is beyond
     * [repairDigits] (nothing flanks it on the left) and still passes as a three-letter prefix on
     * the catch-all shape, so the code is stored wrong and the carrier reports nothing for it.
     * Requiring the repair to reach a real shape is what makes it safe to try: a genuine
     * three-letter prefix like `NLB123456789` repairs to no shape at all and is left alone.
     */
    private fun repairedToKnownShape(candidate: String): String {
        if (TrackingCode.isKnownShape(candidate)) return candidate
        val repaired = candidate.take(CARRIER_PREFIX_LENGTH) +
            candidate.drop(CARRIER_PREFIX_LENGTH).map { DIGIT_REPAIRS[it] ?: it }.joinToString("")
        return if (TrackingCode.isKnownShape(repaired)) repaired else candidate
    }

    /**
     * Rewrites confusable glyphs only where they sit between digits, which is where a tracking
     * code's numeric run lives. A word like "LOJA" is left alone because none of its letters are
     * digit-flanked.
     */
    private fun repairDigits(text: String): String {
        val chars = text.uppercase().toCharArray()
        for (index in chars.indices) {
            val repair = DIGIT_REPAIRS[chars[index]] ?: continue
            val prev = chars.getOrNull(index - 1)
            val next = chars.getOrNull(index + 1)
            if (prev?.isDigit() == true && next?.isDigit() == true) chars[index] = repair
        }
        return String(chars)
    }

    /**
     * Picks the line that reads like a product title: the longest one that is not a code, a price
     * or interface chrome. AliExpress titles are long, keyword-stuffed and therefore the most
     * distinctive text on the screen.
     */
    private fun findProductName(lines: List<String>, codes: List<String>): String? =
        lines
            .filter { line -> isTitleCandidate(line, codes) }
            .maxByOrNull { it.length }
            ?.take(MAX_NAME_LENGTH)
            ?.trim()

    private fun isTitleCandidate(line: String, codes: List<String>): Boolean {
        if (line.length < MIN_NAME_LENGTH) return false
        val normalized = line.lowercase()
        if (LABEL_NOISE.any { it == normalized }) return false
        if (PRICE.containsMatchIn(line)) return false
        if (codes.any { it in TrackingCode.normalize(line) }) return false
        val letters = line.count { it.isLetter() }
        return letters >= line.length / 2
    }

    private const val MIN_NAME_LENGTH = 12
    private const val MAX_NAME_LENGTH = 120

    /** Both families this app sees, Cainiao logistics ids and S10, name the carrier in two letters. */
    private const val CARRIER_PREFIX_LENGTH = 2

    /** A code sits on one short line, so its suggested box is widened to be draggable. */
    private const val CODE_PADDING = 0.03f
    private const val ROW_PADDING = 0.015f
}

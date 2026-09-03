package io.github.arturomanzoli.rastreado.domain

/**
 * Recovers a tracking code and a product name from the text recognised in a photo of an
 * AliExpress order screen.
 */
object OcrFieldExtractor {

    data class Result(
        val code: String? = null,
        val productName: String? = null,
        val otherCodes: List<String> = emptyList(),
    )

    /**
     * Glyphs a text recogniser confuses inside an otherwise numeric run. Applied only when the
     * unrepaired text yields no code, so a correctly-read code is never rewritten.
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
            otherCodes = codes.drop(1),
        )
    }

    private fun findCodes(text: String): List<String> {
        val direct = TrackingCode.findAll(text)
        if (direct.isNotEmpty()) return direct
        return TrackingCode.findAll(repairDigits(text))
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
}

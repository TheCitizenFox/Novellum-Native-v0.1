package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult

data class CopyRange(
    val start: Int,
    val endExclusive: Int
) {
    fun clamped(textLength: Int): CopyRange {
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = endExclusive.coerceIn(safeStart, textLength)
        return CopyRange(safeStart, safeEnd)
    }
}

fun isTapOnText(textLayoutResult: TextLayoutResult?, localPos: Offset): Boolean {
    if (textLayoutResult == null || textLayoutResult.lineCount == 0) return false
    if (localPos.y < 0f || localPos.y > textLayoutResult.size.height.toFloat()) return false

    val line = textLayoutResult.getLineForVerticalPosition(localPos.y)
    if (line !in 0 until textLayoutResult.lineCount) return false

    val lineTop = textLayoutResult.getLineTop(line)
    val lineBottom = textLayoutResult.getLineBottom(line)
    if (localPos.y < lineTop || localPos.y > lineBottom) return false

    val lineLeft = textLayoutResult.getLineLeft(line)
    val lineRight = textLayoutResult.getLineRight(line)
    val horizontalAllowance = 24f
    return localPos.x >= lineLeft - horizontalAllowance &&
        localPos.x <= lineRight + horizontalAllowance
}

private fun lineEndWithoutTrailingNewline(
    text: String,
    textLayoutResult: TextLayoutResult,
    line: Int
): Int {
    var end = textLayoutResult.getLineEnd(line, visibleEnd = false).coerceIn(0, text.length)
    while (end > 0 && end <= text.length && (text[end - 1] == '\n' || text[end - 1] == '\r')) {
        end--
    }
    return end
}

fun getLineRangeForOffset(
    text: String,
    textLayoutResult: TextLayoutResult,
    offset: Offset
): CopyRange? {
    if (!isTapOnText(textLayoutResult, offset)) return null
    val line = textLayoutResult.getLineForVerticalPosition(offset.y)
    if (line !in 0 until textLayoutResult.lineCount) return null

    val start = textLayoutResult.getLineStart(line).coerceIn(0, text.length)
    val end = lineEndWithoutTrailingNewline(text, textLayoutResult, line)
    if (end <= start) return null
    return CopyRange(start, end)
}

fun getParagraphRangeForOffset(
    text: String,
    textLayoutResult: TextLayoutResult,
    offset: Offset
): CopyRange? {
    if (!isTapOnText(textLayoutResult, offset)) return null
    if (text.isEmpty()) return null

    val rawOffset = textLayoutResult.getOffsetForPosition(offset).coerceIn(0, text.length)
    val charOffset = if (rawOffset == text.length) (text.length - 1).coerceAtLeast(0) else rawOffset

    val start = text.lastIndexOf('\n', startIndex = charOffset).let { if (it < 0) 0 else it + 1 }
    val end = text.indexOf('\n', startIndex = charOffset).let { if (it < 0) text.length else it }

    if (end <= start) return null
    return CopyRange(start, end)
}

fun extendRangeOneLine(
    text: String,
    textLayoutResult: TextLayoutResult,
    currentRange: CopyRange
): CopyRange {
    val current = currentRange.clamped(text.length)
    if (current.start >= current.endExclusive || textLayoutResult.lineCount == 0) return current

    val lastSelectedOffset = (current.endExclusive - 1).coerceIn(0, (text.length - 1).coerceAtLeast(0))
    val lastLine = textLayoutResult.getLineForOffset(lastSelectedOffset)
    val nextLine = lastLine + 1
    if (nextLine >= textLayoutResult.lineCount) return current

    val nextEnd = lineEndWithoutTrailingNewline(text, textLayoutResult, nextLine)
    if (nextEnd <= current.endExclusive) return current
    return CopyRange(current.start, nextEnd)
}

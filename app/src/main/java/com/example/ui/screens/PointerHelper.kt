package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult

fun isTapOnText(textLayoutResult: TextLayoutResult?, localPos: Offset): Boolean {
    if (textLayoutResult == null) return false
    val line = textLayoutResult.getLineForVerticalPosition(localPos.y)
    val lineTop = textLayoutResult.getLineTop(line)
    val lineBottom = textLayoutResult.getLineBottom(line)
    if (localPos.y < lineTop || localPos.y > lineBottom) return false 
    val lineLeft = textLayoutResult.getLineLeft(line)
    val lineRight = textLayoutResult.getLineRight(line)
    if (localPos.x < lineLeft - 30f || localPos.x > lineRight + 30f) return false
    return true
}

fun getLineRangeForOffset(textLayoutResult: TextLayoutResult, offset: Offset): IntRange? {
    val line = textLayoutResult.getLineForVerticalPosition(offset.y)
    if (line < 0 || line >= textLayoutResult.lineCount) return null
    val start = textLayoutResult.getLineStart(line)
    val end = textLayoutResult.getLineEnd(line)
    return start..end
}

fun getParagraphRangeForOffset(text: String, textLayoutResult: TextLayoutResult, offset: Offset): IntRange? {
    val charOffset = textLayoutResult.getOffsetForPosition(offset)
    if (charOffset < 0 || charOffset > text.length) return null
    
    val start = text.lastIndexOf('\n', charOffset).let { if (it == -1) 0 else it + 1 }
    var end = text.indexOf('\n', charOffset)
    if (end == -1) end = text.length else end += 1 // Include the newline if it exists
    return start..end
}

fun extendRangeOneLine(textLayoutResult: TextLayoutResult, currentRange: IntRange): IntRange {
    val endCharOffset = currentRange.last
    val lastLine = textLayoutResult.getLineForOffset(if (endCharOffset > 0) endCharOffset - 1 else 0)
    
    if (lastLine + 1 < textLayoutResult.lineCount) {
        val nextLineEnd = textLayoutResult.getLineEnd(lastLine + 1)
        return currentRange.first..nextLineEnd
    }
    return currentRange
}

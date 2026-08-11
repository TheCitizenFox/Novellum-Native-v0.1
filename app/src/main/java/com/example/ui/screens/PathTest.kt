package com.example.ui.screens
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Path
fun testPath(res: TextLayoutResult): Path {
    return res.getPathForRange(0, 1)
}

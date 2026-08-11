import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.Path
fun check(t: TextLayoutResult): Path {
    return t.getPathForRange(0, 1)
}

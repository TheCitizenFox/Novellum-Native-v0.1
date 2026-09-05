package com.example.ui.features.workspace

import com.example.data.entity.ChapterEntity
import com.example.data.entity.ProjectEntity
import com.example.data.entity.SceneEntity
import kotlin.math.ceil

/**
 * Allocation-light word counter for UI telemetry. This runs frequently enough
 * that splitting an entire scene/manuscript into temporary Strings would be an
 * unnecessary source of typing and autosave jank on a tablet.
 */
internal fun wordCount(text: String): Int {
    var count = 0
    var insideWord = false
    for (character in text) {
        if (character.isWhitespace()) {
            insideWord = false
        } else if (!insideWord) {
            count += 1
            insideWord = true
        }
    }
    return count
}

internal fun readingMinutes(words: Int): Int = if (words <= 0) 0 else ceil(words / 250.0).toInt()

internal fun formatCompactWords(words: Int): String = when {
    words >= 1_000_000 -> "%.1fm".format(words / 1_000_000f)
    words >= 10_000 -> "%.1fk".format(words / 1_000f)
    words >= 1_000 -> "%.1fk".format(words / 1_000f)
    else -> words.toString()
}


internal fun projectDisplayTitle(project: ProjectEntity): String {
    val title = project.title.trim()
    val defaultTitle = title.isBlank() ||
        title.equals("Project", ignoreCase = true) ||
        Regex("(?i)^Project\\s+\\d+$").matches(title)
    return if (defaultTitle) "Blank Project" else title
}

internal fun chapterDisplayTitle(chapter: ChapterEntity, position: Int): String {
    val title = chapter.title.trim()
    val number = position + 1
    val defaultTitle = title.equals("Chapter", ignoreCase = true) ||
        Regex("(?i)^Chapter\\s+\\d+$").matches(title)
    return if (title.isBlank() || defaultTitle) "Chapter $number" else "Ch $number · $title"
}

internal fun chapterHeadingTitle(chapter: ChapterEntity, position: Int): String {
    val title = chapter.title.trim()
    val number = position + 1
    val defaultTitle = title.equals("Chapter", ignoreCase = true) ||
        Regex("(?i)^Chapter\\s+\\d+$").matches(title)
    return if (title.isBlank() || defaultTitle) "Chapter $number" else title
}

internal fun sceneDisplayTitle(scene: SceneEntity, position: Int): String {
    val title = scene.title.trim()
    val number = position + 1
    val defaultTitle = title.equals("Scene", ignoreCase = true) ||
        Regex("(?i)^Scene\\s+\\d+$").matches(title)
    return if (title.isBlank() || defaultTitle) "Scene $number" else "Sc $number · $title"
}

internal fun sceneCustomTitle(scene: SceneEntity): String? {
    val title = scene.title.trim()
    val defaultTitle = title.equals("Scene", ignoreCase = true) ||
        Regex("(?i)^Scene\\s+\\d+$").matches(title)
    return title.takeIf { it.isNotBlank() && !defaultTitle }
}

internal fun String.safeDocumentName(fallback: String): String {
    val cleaned = trim()
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .replace(Regex("\\s+"), "_")
        .trim('_', '.')
    return cleaned.ifEmpty { fallback }
}

package com.example.ui.features.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

internal enum class WorkspaceIcon {
    Brand,
    Editor,
    Cards,
    Vault,
    Library,
    Manuscript,
    History,
    Settings,
    PanelLeft,
    PanelRight,
    More,
    Add,
    Search,
    Filter,
    ChevronRight,
    ChevronDown,
    Folder,
    Document,
    Quote,
    BulletedList,
    NumberedList,
    Undo,
    Redo,
    Split,
    Clean,
    Backup,
    Export,
    Stats,
    Project,
    Chapter,
    Words,
    Close,
    Check
}

@Composable
internal fun NovellumIcon(
    icon: WorkspaceIcon,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawWorkspaceIcon(icon, tint)
    }
}

private fun DrawScope.drawWorkspaceIcon(icon: WorkspaceIcon, tint: Color) {
    val unit = min(size.width, size.height)
    val ox = (size.width - unit) / 2f
    val oy = (size.height - unit) / 2f
    fun at(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)
    val stroke = unit * 0.075f
    val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (icon) {
        WorkspaceIcon.Brand -> {
            drawCircle(tint.copy(alpha = 0.28f), unit * 0.16f, at(.5f, .5f), style = Stroke(stroke * .55f))
            drawLine(tint, at(.5f, .08f), at(.5f, .92f), stroke * .68f, StrokeCap.Round)
            drawLine(tint, at(.08f, .5f), at(.92f, .5f), stroke * .68f, StrokeCap.Round)
            drawLine(tint, at(.22f, .22f), at(.78f, .78f), stroke * .42f, StrokeCap.Round)
            drawLine(tint, at(.78f, .22f), at(.22f, .78f), stroke * .42f, StrokeCap.Round)
        }
        WorkspaceIcon.Editor -> {
            val path = Path().apply {
                moveTo(at(.19f, .78f).x, at(.19f, .78f).y)
                lineTo(at(.28f, .52f).x, at(.28f, .52f).y)
                lineTo(at(.68f, .12f).x, at(.68f, .12f).y)
                lineTo(at(.86f, .30f).x, at(.86f, .30f).y)
                lineTo(at(.46f, .70f).x, at(.46f, .70f).y)
                close()
            }
            drawPath(path, tint, style = style)
            drawLine(tint, at(.60f, .20f), at(.78f, .38f), stroke, StrokeCap.Round)
            drawLine(tint, at(.18f, .84f), at(.43f, .73f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Cards -> {
            drawRoundRect(tint, at(.15f, .25f), Size(unit * .58f, unit * .58f), CornerRadius(unit * .07f), style = style)
            drawRoundRect(tint, at(.31f, .12f), Size(unit * .55f, unit * .56f), CornerRadius(unit * .07f), style = style)
            drawLine(tint, at(.42f, .31f), at(.74f, .31f), stroke * .72f, StrokeCap.Round)
        }
        WorkspaceIcon.Vault -> {
            drawRoundRect(tint, at(.18f, .23f), Size(unit * .64f, unit * .61f), CornerRadius(unit * .07f), style = style)
            drawLine(tint, at(.18f, .39f), at(.82f, .39f), stroke, StrokeCap.Round)
            drawLine(tint, at(.36f, .14f), at(.36f, .29f), stroke, StrokeCap.Round)
            drawLine(tint, at(.64f, .14f), at(.64f, .29f), stroke, StrokeCap.Round)
            drawCircle(tint, unit * .035f, at(.5f, .61f))
        }
        WorkspaceIcon.Library -> {
            drawRoundRect(tint, at(.16f, .16f), Size(unit * .28f, unit * .69f), CornerRadius(unit * .04f), style = style)
            drawRoundRect(tint, at(.56f, .16f), Size(unit * .28f, unit * .69f), CornerRadius(unit * .04f), style = style)
            drawLine(tint, at(.44f, .25f), at(.56f, .20f), stroke * .65f, StrokeCap.Round)
            drawLine(tint, at(.44f, .76f), at(.56f, .81f), stroke * .65f, StrokeCap.Round)
        }
        WorkspaceIcon.Manuscript, WorkspaceIcon.Document -> {
            val path = Path().apply {
                moveTo(at(.24f, .12f).x, at(.24f, .12f).y)
                lineTo(at(.62f, .12f).x, at(.62f, .12f).y)
                lineTo(at(.80f, .30f).x, at(.80f, .30f).y)
                lineTo(at(.80f, .86f).x, at(.80f, .86f).y)
                lineTo(at(.24f, .86f).x, at(.24f, .86f).y)
                close()
            }
            drawPath(path, tint, style = style)
            drawLine(tint, at(.62f, .13f), at(.62f, .31f), stroke * .72f, StrokeCap.Round)
            drawLine(tint, at(.62f, .31f), at(.79f, .31f), stroke * .72f, StrokeCap.Round)
            drawLine(tint, at(.36f, .50f), at(.68f, .50f), stroke * .65f, StrokeCap.Round)
            drawLine(tint, at(.36f, .65f), at(.64f, .65f), stroke * .65f, StrokeCap.Round)
        }
        WorkspaceIcon.History -> {
            drawArc(tint, 42f, 286f, false, at(.16f, .16f), Size(unit * .68f, unit * .68f), style = style)
            drawLine(tint, at(.18f, .23f), at(.18f, .43f), stroke, StrokeCap.Round)
            drawLine(tint, at(.18f, .23f), at(.37f, .23f), stroke, StrokeCap.Round)
            drawLine(tint, at(.50f, .30f), at(.50f, .53f), stroke, StrokeCap.Round)
            drawLine(tint, at(.50f, .53f), at(.66f, .62f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Settings -> {
            drawCircle(tint, unit * .14f, at(.5f, .5f), style = style)
            for (i in 0..3) {
                val horizontal = i % 2 == 0
                if (horizontal) {
                    drawLine(tint, at(.10f, .5f), at(.26f, .5f), stroke, StrokeCap.Round)
                    drawLine(tint, at(.74f, .5f), at(.90f, .5f), stroke, StrokeCap.Round)
                } else {
                    drawLine(tint, at(.5f, .10f), at(.5f, .26f), stroke, StrokeCap.Round)
                    drawLine(tint, at(.5f, .74f), at(.5f, .90f), stroke, StrokeCap.Round)
                }
            }
            drawLine(tint, at(.22f, .22f), at(.32f, .32f), stroke, StrokeCap.Round)
            drawLine(tint, at(.68f, .68f), at(.78f, .78f), stroke, StrokeCap.Round)
            drawLine(tint, at(.78f, .22f), at(.68f, .32f), stroke, StrokeCap.Round)
            drawLine(tint, at(.32f, .68f), at(.22f, .78f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.PanelLeft, WorkspaceIcon.PanelRight, WorkspaceIcon.Split -> {
            drawRoundRect(tint, at(.13f, .18f), Size(unit * .74f, unit * .64f), CornerRadius(unit * .06f), style = style)
            val x = when (icon) {
                WorkspaceIcon.PanelLeft -> .38f
                WorkspaceIcon.PanelRight -> .62f
                else -> .50f
            }
            drawLine(tint, at(x, .20f), at(x, .80f), stroke * .75f, StrokeCap.Round)
        }
        WorkspaceIcon.More -> {
            drawCircle(tint, unit * .055f, at(.5f, .24f))
            drawCircle(tint, unit * .055f, at(.5f, .5f))
            drawCircle(tint, unit * .055f, at(.5f, .76f))
        }
        WorkspaceIcon.Add -> {
            drawLine(tint, at(.2f, .5f), at(.8f, .5f), stroke, StrokeCap.Round)
            drawLine(tint, at(.5f, .2f), at(.5f, .8f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Search -> {
            drawCircle(tint, unit * .25f, at(.43f, .43f), style = style)
            drawLine(tint, at(.61f, .61f), at(.84f, .84f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Filter -> {
            val path = Path().apply {
                moveTo(at(.13f, .20f).x, at(.13f, .20f).y)
                lineTo(at(.87f, .20f).x, at(.87f, .20f).y)
                lineTo(at(.59f, .51f).x, at(.59f, .51f).y)
                lineTo(at(.59f, .82f).x, at(.59f, .82f).y)
                lineTo(at(.41f, .73f).x, at(.41f, .73f).y)
                lineTo(at(.41f, .51f).x, at(.41f, .51f).y)
                close()
            }
            drawPath(path, tint, style = style)
        }
        WorkspaceIcon.ChevronRight -> {
            drawLine(tint, at(.35f, .22f), at(.65f, .5f), stroke, StrokeCap.Round)
            drawLine(tint, at(.65f, .5f), at(.35f, .78f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.ChevronDown -> {
            drawLine(tint, at(.22f, .35f), at(.5f, .65f), stroke, StrokeCap.Round)
            drawLine(tint, at(.5f, .65f), at(.78f, .35f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Folder, WorkspaceIcon.Chapter -> {
            val path = Path().apply {
                moveTo(at(.12f, .29f).x, at(.12f, .29f).y)
                lineTo(at(.39f, .29f).x, at(.39f, .29f).y)
                lineTo(at(.47f, .19f).x, at(.47f, .19f).y)
                lineTo(at(.84f, .19f).x, at(.84f, .19f).y)
                lineTo(at(.88f, .78f).x, at(.88f, .78f).y)
                lineTo(at(.12f, .78f).x, at(.12f, .78f).y)
                close()
            }
            drawPath(path, tint, style = style)
        }
        WorkspaceIcon.Quote -> {
            drawArc(tint, 200f, 210f, false, at(.14f, .22f), Size(unit * .27f, unit * .35f), style = style)
            drawArc(tint, 200f, 210f, false, at(.50f, .22f), Size(unit * .27f, unit * .35f), style = style)
            drawLine(tint, at(.20f, .52f), at(.16f, .72f), stroke, StrokeCap.Round)
            drawLine(tint, at(.56f, .52f), at(.52f, .72f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.BulletedList -> {
            listOf(.25f, .5f, .75f).forEach { y ->
                drawCircle(tint, unit * .035f, at(.18f, y))
                drawLine(tint, at(.34f, y), at(.84f, y), stroke * .72f, StrokeCap.Round)
            }
        }
        WorkspaceIcon.NumberedList -> {
            listOf(.25f, .5f, .75f).forEachIndexed { index, y ->
                drawCircle(tint.copy(alpha = .35f), unit * .075f, at(.18f, y), style = Stroke(stroke * .45f))
                drawLine(tint, at(.34f, y), at(.84f, y), stroke * .72f, StrokeCap.Round)
                drawCircle(tint, unit * .018f, at(.18f, y + (index - 1) * .006f))
            }
        }
        WorkspaceIcon.Undo, WorkspaceIcon.Redo -> {
            val reverse = icon == WorkspaceIcon.Redo
            val left = if (reverse) .78f else .22f
            val right = if (reverse) .22f else .78f
            drawArc(tint, if (reverse) 210f else 30f, if (reverse) 250f else -250f, false, at(.22f, .24f), Size(unit * .58f, unit * .52f), style = style)
            drawLine(tint, at(left, .25f), at(left, .52f), stroke, StrokeCap.Round)
            drawLine(tint, at(left, .25f), at(right + if (reverse) .34f else -.34f, .25f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Clean -> {
            drawLine(tint, at(.22f, .78f), at(.75f, .25f), stroke, StrokeCap.Round)
            drawLine(tint, at(.18f, .25f), at(.18f, .47f), stroke * .62f, StrokeCap.Round)
            drawLine(tint, at(.07f, .36f), at(.29f, .36f), stroke * .62f, StrokeCap.Round)
            drawLine(tint, at(.72f, .55f), at(.72f, .79f), stroke * .62f, StrokeCap.Round)
            drawLine(tint, at(.60f, .67f), at(.84f, .67f), stroke * .62f, StrokeCap.Round)
        }
        WorkspaceIcon.Backup -> {
            drawRoundRect(tint, at(.17f, .52f), Size(unit * .66f, unit * .28f), CornerRadius(unit * .06f), style = style)
            drawLine(tint, at(.5f, .14f), at(.5f, .61f), stroke, StrokeCap.Round)
            drawLine(tint, at(.31f, .34f), at(.5f, .14f), stroke, StrokeCap.Round)
            drawLine(tint, at(.69f, .34f), at(.5f, .14f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Export -> {
            drawRoundRect(tint, at(.17f, .50f), Size(unit * .66f, unit * .31f), CornerRadius(unit * .05f), style = style)
            drawLine(tint, at(.5f, .12f), at(.5f, .61f), stroke, StrokeCap.Round)
            drawLine(tint, at(.31f, .39f), at(.5f, .61f), stroke, StrokeCap.Round)
            drawLine(tint, at(.69f, .39f), at(.5f, .61f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Stats -> {
            drawLine(tint, at(.16f, .80f), at(.16f, .55f), stroke * 1.5f, StrokeCap.Round)
            drawLine(tint, at(.40f, .80f), at(.40f, .33f), stroke * 1.5f, StrokeCap.Round)
            drawLine(tint, at(.64f, .80f), at(.64f, .18f), stroke * 1.5f, StrokeCap.Round)
            drawLine(tint, at(.84f, .80f), at(.84f, .46f), stroke * 1.5f, StrokeCap.Round)
        }
        WorkspaceIcon.Project -> {
            drawRoundRect(tint, at(.15f, .16f), Size(unit * .70f, unit * .68f), CornerRadius(unit * .05f), style = style)
            drawLine(tint, at(.38f, .17f), at(.38f, .83f), stroke * .7f, StrokeCap.Round)
            drawLine(tint, at(.48f, .36f), at(.74f, .36f), stroke * .65f, StrokeCap.Round)
            drawLine(tint, at(.48f, .52f), at(.70f, .52f), stroke * .65f, StrokeCap.Round)
        }
        WorkspaceIcon.Words -> {
            val path = Path().apply {
                moveTo(at(.10f, .20f).x, at(.10f, .20f).y)
                lineTo(at(.29f, .80f).x, at(.29f, .80f).y)
                lineTo(at(.50f, .31f).x, at(.50f, .31f).y)
                lineTo(at(.71f, .80f).x, at(.71f, .80f).y)
                lineTo(at(.90f, .20f).x, at(.90f, .20f).y)
            }
            drawPath(path, tint, style = style)
        }
        WorkspaceIcon.Close -> {
            drawLine(tint, at(.22f, .22f), at(.78f, .78f), stroke, StrokeCap.Round)
            drawLine(tint, at(.78f, .22f), at(.22f, .78f), stroke, StrokeCap.Round)
        }
        WorkspaceIcon.Check -> {
            drawLine(tint, at(.18f, .52f), at(.41f, .75f), stroke, StrokeCap.Round)
            drawLine(tint, at(.41f, .75f), at(.84f, .25f), stroke, StrokeCap.Round)
        }
    }
}

package com.weave.notebook.command

import com.weave.notebook.model.StrokeRecord
import kotlin.math.hypot

/**
 * Bounded undo/redo. One command per stroke, erase, page op, or template change.
 * New edit clears redo.
 */
sealed class EditorCommand {
    data class AddStroke(val stroke: StrokeRecord) : EditorCommand()
    data class EraseStroke(val stroke: StrokeRecord) : EditorCommand()
    data class ReplaceStroke(val before: StrokeRecord, val after: StrokeRecord) : EditorCommand()
}

class CommandHistory(
    private val maxCommands: Int = 100,
    private val maxStrokeSamples: Int = 500_000
) {
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()
    private var sampleBudget = 0

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(command: EditorCommand) {
        undoStack.addLast(command)
        redoStack.clear()
        sampleBudget += sampleCount(command)
        trim()
    }

    fun undo(): EditorCommand? {
        val cmd = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(cmd)
        sampleBudget -= sampleCount(cmd)
        return cmd
    }

    fun redo(): EditorCommand? {
        val cmd = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(cmd)
        sampleBudget += sampleCount(cmd)
        return cmd
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        sampleBudget = 0
    }

    private fun trim() {
        while (undoStack.size > maxCommands || sampleBudget > maxStrokeSamples) {
            val removed = undoStack.removeFirst()
            sampleBudget -= sampleCount(removed)
        }
    }

    private fun sampleCount(cmd: EditorCommand): Int = when (cmd) {
        is EditorCommand.AddStroke -> cmd.stroke.samples.size
        is EditorCommand.EraseStroke -> cmd.stroke.samples.size
        is EditorCommand.ReplaceStroke -> cmd.before.samples.size + cmd.after.samples.size
    }
}

/**
 * Whole-stroke eraser using spatial hit testing in page millimeters.
 */
object StrokeEraser {
    fun hitTest(
        strokes: List<StrokeRecord>,
        xMm: Float,
        yMm: Float,
        radiusMm: Float
    ): StrokeRecord? {
        // Search newest first for intuitive erase of top content.
        for (i in strokes.indices.reversed()) {
            val s = strokes[i]
            if (s.brush == com.weave.notebook.model.BrushKind.ERASER) continue
            if (strokeHits(s, xMm, yMm, radiusMm)) return s
        }
        return null
    }

    private fun strokeHits(stroke: StrokeRecord, x: Float, y: Float, radius: Float): Boolean {
        val samples = stroke.samples
        if (samples.isEmpty()) return false
        val r = radius + stroke.widthMm * 0.5f
        if (samples.size == 1) {
            return hypot(samples[0].xMm - x, samples[0].yMm - y) <= r
        }
        for (i in 1 until samples.size) {
            if (distToSegment(x, y, samples[i - 1].xMm, samples[i - 1].yMm, samples[i].xMm, samples[i].yMm) <= r) {
                return true
            }
        }
        return false
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return hypot(px - x1, py - y1)
        val t = (((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
        return hypot(px - (x1 + t * dx), py - (y1 + t * dy))
    }
}

/** Page-space grid narrows whole-stroke erasing; interpolates between sparse input samples. */
object EraserPath {
    fun hitIds(strokes: List<StrokeRecord>, path: List<com.weave.notebook.model.StrokeSample>, radius: Float): Set<String> {
        if(path.isEmpty()) return emptySet()
        val cells=HashMap<Pair<Int,Int>,MutableList<StrokeRecord>>()
        val cellSize=16f
        strokes.forEach {s->
            if(s.samples.isNotEmpty()) {
                val margin=s.widthMm/2
                val left=kotlin.math.floor((s.samples.minOf {it.xMm}-margin)/cellSize).toInt()
                val right=kotlin.math.floor((s.samples.maxOf {it.xMm}+margin)/cellSize).toInt()
                val top=kotlin.math.floor((s.samples.minOf {it.yMm}-margin)/cellSize).toInt()
                val bottom=kotlin.math.floor((s.samples.maxOf {it.yMm}+margin)/cellSize).toInt()
                for(x in left..right)for(y in top..bottom)cells.getOrPut(x to y) {mutableListOf()}.add(s)
            }
        }
        val removed=HashSet<String>()
        fun hit(x:Float,y:Float) {
            val candidates=LinkedHashSet<StrokeRecord>()
            for(cx in kotlin.math.floor((x-radius)/cellSize).toInt()..kotlin.math.floor((x+radius)/cellSize).toInt())
                for(cy in kotlin.math.floor((y-radius)/cellSize).toInt()..kotlin.math.floor((y+radius)/cellSize).toInt())
                    cells[cx to cy]?.let {candidates.addAll(it)}
            val remaining=candidates.filter {it.strokeId !in removed}.toMutableList()
            while(true) {val s=StrokeEraser.hitTest(remaining,x,y,radius)?:break;removed.add(s.strokeId);remaining.remove(s)}
        }
        hit(path[0].xMm,path[0].yMm)
        path.zipWithNext().forEach { (a,b)->
            val n=kotlin.math.ceil(kotlin.math.hypot(b.xMm-a.xMm,b.yMm-a.yMm)/maxOf(0.25f,radius/2)).toInt().coerceAtLeast(1)
            for(i in 1..n) {val t=i.toFloat()/n;hit(a.xMm+(b.xMm-a.xMm)*t,a.yMm+(b.yMm-a.yMm)*t)}
        }
        return removed
    }
}

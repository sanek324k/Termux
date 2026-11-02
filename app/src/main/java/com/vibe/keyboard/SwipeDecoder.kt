package com.vibe.keyboard

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.ln

/** Простой распознаватель свайп-ввода: сравнивает путь пальца с "идеальным" путём слова по центрам клавиш. */
object SwipeDecoder {
    private const val N = 40

    private fun length(p: List<Offset>): Float {
        var s = 0f
        for (i in 1 until p.size) s += (p[i] - p[i - 1]).getDistance()
        return s
    }

    private fun resample(pts: List<Offset>, n: Int): List<Offset> {
        if (pts.isEmpty()) return List(n) { Offset.Zero }
        if (pts.size < 2) return List(n) { pts[0] }
        val lens = FloatArray(pts.size)
        var total = 0f
        for (i in 1 until pts.size) {
            total += (pts[i] - pts[i - 1]).getDistance()
            lens[i] = total
        }
        if (total == 0f) return List(n) { pts[0] }
        val out = ArrayList<Offset>(n)
        var seg = 1
        for (k in 0 until n) {
            val target = total * k / (n - 1)
            while (seg < pts.size - 1 && lens[seg] < target) seg++
            val l0 = lens[seg - 1]
            val l1 = lens[seg]
            val t = if (l1 - l0 > 0f) ((target - l0) / (l1 - l0)).coerceIn(0f, 1f) else 0f
            out.add(pts[seg - 1] + (pts[seg] - pts[seg - 1]) * t)
        }
        return out
    }

    fun decode(
        path: List<Offset>,
        centers: Map<Char, Offset>,
        keyW: Float,
        words: List<Pair<String, Int>>,
    ): List<String> {
        if (path.size < 2 || centers.isEmpty() || keyW <= 0f) return emptyList()
        val pathLen = length(path)
        if (pathLen < keyW * 1.2f) return emptyList()
        val user = resample(path, N)
        val first = path.first()
        val last = path.last()
        val maxEnd = keyW * 1.9f
        val scored = ArrayList<Pair<Float, String>>()

        for ((w, rank) in words) {
            val c0 = centers[w[0]] ?: continue
            if ((c0 - first).getDistance() > maxEnd) continue
            val cl = centers[w[w.length - 1]] ?: continue
            if ((cl - last).getDistance() > maxEnd) continue

            val pts = ArrayList<Offset>()
            var ok = true
            for (ch in w) {
                val c = centers[ch]
                if (c == null) { ok = false; break }
                if (pts.isEmpty() || (pts[pts.size - 1] - c).getDistance() > 1f) pts.add(c)
            }
            if (!ok || pts.size < 2) continue

            val ideal = resample(pts, N)
            var sum = 0f
            for (i in 0 until N) sum += (ideal[i] - user[i]).getDistance()
            val avg = sum / N / keyW
            if (avg > 1.8f) continue
            val ratio = length(pts) / pathLen
            val score = avg + 0.25f * abs(ln(ratio)) + minOf(rank, 625) * 0.0008f
            scored.add(score to w)
        }
        return scored.sortedBy { it.first }.map { it.second }.distinct().take(3)
    }
}

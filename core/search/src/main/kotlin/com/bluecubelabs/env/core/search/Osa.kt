package com.bluecubelabs.env.core.search

object Osa {
    fun distance(a: CharSequence, b: CharSequence): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        val d = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) d[i][0] = i
        for (j in 0..m) d[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, d[i - 2][j - 2] + 1)
                }
                d[i][j] = v
            }
        }
        return d[n][m]
    }

    fun prefixDistance(query: String, target: String): Int {
        val len = query.length
        var best = Int.MAX_VALUE
        for (l in (len - 1)..(len + 1)) {
            if (l < 1 || l > target.length) continue
            val dist = distance(query, target.substring(0, l))
            if (dist < best) best = dist
        }
        return best
    }
}

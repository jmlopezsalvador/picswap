package com.picswap.domain

/**
 * Recorte de fondo sin IA. Trabaja sobre píxeles ARGB no premultiplicados (como `ImageBitmap.readPixels`).
 *
 * Idea: el fondo de una foto de un objeto sobre una superficie lisa es casi uniforme. Se estima su color con
 * los bordes de la imagen y se "inunda" desde los bordes hacia dentro mientras los píxeles se parezcan al
 * fondo. Lo que no se alcanza es el objeto. El resultado conserva el color original del objeto y pone
 * transparencia (alfa) en el fondo, con un borde suavizado.
 *
 * Como el fondo queda transparente, la app pinta debajo el color plano elegido (cambiarlo es instantáneo).
 *
 * @param tolerance 0 = desactivado (devuelve la imagen tal cual). Valores típicos: 25-60 (distancia RGB).
 * @param refColor color de fondo a retirar elegido con el cuentagotas; si es null se estima con los bordes.
 * @param seedIndex posición (índice del píxel) tocada con el cuentagotas: se parte también desde ahí, aunque no
 *   esté en el borde de la imagen.
 */
fun cutoutBackground(
    argb: IntArray,
    width: Int,
    height: Int,
    tolerance: Int,
    refColor: Int? = null,
    seedIndex: Int? = null,
): IntArray {
    if (tolerance <= 0 || width < 4 || height < 4) return argb
    val n = width * height

    val bg = refColor ?: estimateBorderColor(argb, width, height)
    val maxSq = tolerance * tolerance
    val step = maxOf(6, (tolerance * 0.35f).toInt())
    val stepSq = step * step

    // --- 1. Inundación desde los bordes: bgMask[i] = es fondo ---
    val isBg = BooleanArray(n)
    val stack = IntArray(n)
    var sp = 0

    fun distSq(c1: Int, c2: Int): Int {
        val dr = ((c1 shr 16) and 0xFF) - ((c2 shr 16) and 0xFF)
        val dg = ((c1 shr 8) and 0xFF) - ((c2 shr 8) and 0xFF)
        val db = (c1 and 0xFF) - (c2 and 0xFF)
        return dr * dr + dg * dg + db * db
    }

    fun seed(i: Int) {
        if (!isBg[i] && distSq(argb[i], bg) <= maxSq) {
            isBg[i] = true
            stack[sp++] = i
        }
    }
    for (x in 0 until width) { seed(x); seed((height - 1) * width + x) }
    for (y in 0 until height) { seed(y * width); seed(y * width + width - 1) }
    if (seedIndex != null && seedIndex in 0 until n && !isBg[seedIndex]) {  // el punto tocado siempre cuenta como fondo
        isBg[seedIndex] = true
        stack[sp++] = seedIndex
    }

    while (sp > 0) {
        val i = stack[--sp]
        val x = i % width
        val y = i / width
        val c = argb[i]
        fun visit(j: Int) {
            if (!isBg[j] && distSq(argb[j], bg) <= maxSq && distSq(argb[j], c) <= stepSq) {
                isBg[j] = true
                stack[sp++] = j
            }
        }
        if (x > 0) visit(i - 1)
        if (x < width - 1) visit(i + 1)
        if (y > 0) visit(i - width)
        if (y < height - 1) visit(i + width)
    }

    // --- 2. Quitar motas: componentes de objeto muy pequeños (polvo, ruido) pasan a ser fondo ---
    val minArea = maxOf(16, n / 2000)
    val seen = BooleanArray(n)
    val comp = IntArray(n)
    for (start in 0 until n) {
        if (isBg[start] || seen[start]) continue
        var count = 0
        sp = 0
        stack[sp++] = start
        seen[start] = true
        while (sp > 0) {
            val i = stack[--sp]
            comp[count++] = i
            val x = i % width
            val y = i / width
            fun visit(j: Int) {
                if (!isBg[j] && !seen[j]) { seen[j] = true; stack[sp++] = j }
            }
            if (x > 0) visit(i - 1)
            if (x < width - 1) visit(i + 1)
            if (y > 0) visit(i - width)
            if (y < height - 1) visit(i + width)
        }
        if (count < minArea) for (k in 0 until count) isBg[comp[k]] = true
    }

    // --- 3. Afinar 1 px el objeto (evita el halo del fondo original en el borde) ---
    val hard = IntArray(n)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = y * width + x
            if (isBg[i]) continue
            val inner = (x == 0 || !isBg[i - 1]) && (x == width - 1 || !isBg[i + 1]) &&
                (y == 0 || !isBg[i - width]) && (y == height - 1 || !isBg[i + width])
            if (inner) hard[i] = 255
        }
    }

    // --- 4. Suavizar el borde (desenfoque 3x3 de la máscara) ---
    val out = IntArray(n)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val i = y * width + x
            var sum = 0
            var cnt = 0
            for (dy in -1..1) {
                val yy = y + dy
                if (yy < 0 || yy >= height) continue
                for (dx in -1..1) {
                    val xx = x + dx
                    if (xx < 0 || xx >= width) continue
                    sum += hard[yy * width + xx]
                    cnt++
                }
            }
            val a = sum / cnt
            out[i] = if (a == 0) 0 else (a shl 24) or (argb[i] and 0x00FFFFFF)
        }
    }
    return out
}

/** Color típico del fondo: mediana por canal de un marco fino alrededor de la imagen. */
internal fun estimateBorderColor(argb: IntArray, width: Int, height: Int): Int {
    val ring = maxOf(2, minOf(width, height) / 100)
    val hr = IntArray(256)
    val hg = IntArray(256)
    val hb = IntArray(256)
    var count = 0
    fun add(c: Int) {
        hr[(c shr 16) and 0xFF]++
        hg[(c shr 8) and 0xFF]++
        hb[c and 0xFF]++
        count++
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x < ring || y < ring || x >= width - ring || y >= height - ring) add(argb[y * width + x])
        }
    }
    fun median(h: IntArray): Int {
        var acc = 0
        for (v in 0..255) {
            acc += h[v]
            if (acc * 2 >= count) return v
        }
        return 0
    }
    return (0xFF shl 24) or (median(hr) shl 16) or (median(hg) shl 8) or median(hb)
}

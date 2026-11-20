package com.picswap.domain

import kotlin.math.roundToInt

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
 * @param feather desenfoque del borde (0-100): 0 = borde duro; más = transición más ancha y suave entre objeto y fondo.
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
    feather: Int = 10,
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

    // --- 4. Desenfocar el borde: el radio es proporcional al tamaño de la imagen (así la vista previa y el
    //        resultado final se ven igual) y `feather` 0 deja el borde duro. ---
    val radius = if (feather <= 0) 0 else maxOf(1, (feather / 100f * 0.012f * maxOf(width, height)).roundToInt())
    val out = IntArray(n)
    if (radius == 0) {
        for (i in 0 until n) if (hard[i] != 0) out[i] = (0xFF shl 24) or (argb[i] and 0x00FFFFFF)
        return out
    }

    fun blur(src: IntArray): IntArray {
        var r = boxBlur(src, width, height, radius)
        r = boxBlur(r, width, height, radius)
        return r
    }

    val alpha = blur(hard)
    // Color de los píxeles de la transición: media de los píxeles de OBJETO cercanos. Si se dejara el color original,
    // la transición se teñiría con el color del fondo viejo y aparecería un halo oscuro (o claro) alrededor del objeto.
    fun channel(shift: Int): IntArray {
        val c = IntArray(n) { if (hard[it] != 0) (argb[it] shr shift) and 0xFF else 0 }
        return blur(c)
    }
    val rB = channel(16)
    val gB = channel(8)
    val bB = channel(0)
    for (i in 0 until n) {
        val a = alpha[i]
        if (a == 0) continue
        val r = minOf(255, rB[i] * 255 / a)
        val g = minOf(255, gB[i] * 255 / a)
        val bl = minOf(255, bB[i] * 255 / a)
        out[i] = (a shl 24) or (r shl 16) or (g shl 8) or bl
    }
    return out
}

/** Desenfoque de caja separable (media de una ventana de 2r+1 píxeles) sobre una máscara de 0 a 255. */
private fun boxBlur(src: IntArray, w: Int, h: Int, r: Int): IntArray {
    val tmp = IntArray(src.size)
    val out = IntArray(src.size)
    val win = 2 * r + 1
    for (y in 0 until h) {
        val row = y * w
        var sum = 0
        for (k in -r..r) sum += src[row + k.coerceIn(0, w - 1)]
        for (x in 0 until w) {
            tmp[row + x] = sum / win
            sum += src[row + minOf(x + r + 1, w - 1)] - src[row + maxOf(x - r, 0)]
        }
    }
    for (x in 0 until w) {
        var sum = 0
        for (k in -r..r) sum += tmp[k.coerceIn(0, h - 1) * w + x]
        for (y in 0 until h) {
            out[y * w + x] = sum / win
            sum += tmp[minOf(y + r + 1, h - 1) * w + x] - tmp[maxOf(y - r, 0) * w + x]
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

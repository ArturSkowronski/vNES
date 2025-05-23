/*
 *
 *  * Copyright (C) 2025 Artur Skowroński
 *  * This file is part of kNES, a fork of vNES (GPLv3) rewritten in Kotlin.
 *  *
 *  * vNES was originally developed by Brian F. R. (bfirsh) and released under the GPL-3.0 license.
 *  * This project is a reimplementation and extension of that work.
 *  *
 *  * kNES is licensed under the GNU General Public License v3.0.
 *  * See the LICENSE file for more details.
 *
 */

package knes.emulator.utils

object Misc {
    @JvmField
    var debug: Boolean = true // Hardcoded for simplicity
    private val rnd = FloatArray(100000) { Math.random().toFloat() }
    private var nextRnd = 0

    init {
        for (i in rnd.indices) {
            rnd[i] = Math.random().toFloat()
        }
    }

    @JvmStatic
    fun hex8(i: Int): String {
        var s = Integer.toHexString(i)
        while (s.length < 2) s = "0$s"
        return s.uppercase()
    }

    @JvmStatic
    fun hex16(i: Int): String {
        var s = Integer.toHexString(i)
        while (s.length < 4) s = "0$s"
        return s.uppercase()
    }

    @JvmStatic
    fun binN(num: Int, N: Int): String {
        return CharArray(N) { i ->
            if ((num shr (N - i - 1)) and 0x1 == 1) '1' else '0'
        }.concatToString()
    }

    @JvmStatic
    fun bin8(num: Int) = binN(num, 8)

    @JvmStatic
    fun bin16(num: Int) = binN(num, 16)

    @JvmStatic
    fun binStr(value: Long, bitcount: Int): String {
        return (bitcount - 1 downTo 0).joinToString("") { i ->
            if ((value and (1L shl i)) != 0L) "1" else "0"
        }
    }

    @JvmStatic
    fun resizeArray(array: IntArray, newSize: Int): IntArray {
        return IntArray(newSize).apply {
            System.arraycopy(array, 0, this, 0, minOf(newSize, array.size))
        }
    }

    @JvmStatic
    fun pad(str: String, padStr: String, length: Int): String {
        val sb = StringBuilder(str)
        while (sb.length < length) {
            sb.append(padStr)
        }
        return sb.toString()
    }

    @JvmStatic
    fun random(): Float {
        val ret = rnd[nextRnd]
        nextRnd++
        if (nextRnd >= rnd.size) {
            nextRnd = (Math.random() * (rnd.size - 1)).toInt()
        }
        return ret
    }
}

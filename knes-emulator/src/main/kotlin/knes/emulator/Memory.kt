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

package knes.emulator

import java.io.File
import java.io.FileWriter
import java.io.IOException

class Memory(var memSize: Int) {
	var mem: ShortArray

    init {
        mem = ShortArray(memSize)
    }

    fun stateLoad(buf: ByteBuffer) {
        if (false) mem = ShortArray(this.memSize)
        buf.readByteArray(mem)
    }

    fun stateSave(buf: ByteBuffer) {
        buf.putByteArray(mem)
    }

    fun reset() {
        for (i in mem.indices) mem[i] = 0
    }

    fun write(address: Int, value: Short) {
        mem[address] = value
    }

    fun load(address: Int): Short {
        return mem[address]
    }

    @JvmOverloads
    fun dump(file: String, offset: Int = 0, length: Int = mem.size) {
        val ch = CharArray(length)
        for (i in 0 until length) {
            ch[i] = Char(mem[offset + i].toUShort())
        }

        try {
            val f = File(file)
            val writer = FileWriter(f)
            writer.write(ch)
            writer.close()

            //System.out.println("Memory dumped to file "+file+".");
        } catch (ioe: IOException) {
            //System.out.println("Memory dump to file: IO Error!");
        }
    }

    fun write(address: Int, array: ShortArray, length: Int) {
        if (address + length > mem.size) return
        System.arraycopy(array, 0, mem, address, length)
    }

    fun write(address: Int, array: ShortArray, arrayoffset: Int, length: Int) {
        if (address + length > mem.size) return
        System.arraycopy(array, arrayoffset, mem, address, length)
    }
}
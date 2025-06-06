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

package knes.controllers

interface ControllerProvider {
    fun getButtonState(button: NESButton): Short
    fun mapButton(button: NESButton, code: Int)
    fun update()
    fun configure(config: Map<String, Any> = emptyMap()) {
        // Default empty implementation
    }
    
    enum class NESButton {
        A, B, SELECT, START, UP, DOWN, LEFT, RIGHT
    }
}

package vnes.emulator.ui

import vnes.emulator.input.InputHandler

/**
 * Factory interface for creating UI components for the NES emulator.
 * This interface allows different UI implementations to be plugged into the emulator core.
 */
interface NESUIFactory {
    /**
     * Creates a UI controller that handles input and lifecycle management
     *
     * @param nes The NES instance to associate with the input handler
     * @return An InputHandler implementation
     */
    fun createInputHandler(): InputHandler?

    /**
     * Creates a rendering surface that implements ScreenView interface
     *
     * @param scale The initial scale factor for the screen view
     * @return A ScreenView implementation
     */
    fun createScreenView(scale: Int): ScreenView?

    /**
     * Optional: Configuration for UI-specific settings
     *
     * @param enableAudio Whether audio should be enabled
     * @param fpsLimit The maximum FPS to target, or 0 for unlimited
     * @param enablePpuLogging Whether PPU logging should be enabled
     */
    fun configureUISettings(enableAudio: Boolean, fpsLimit: Int, enablePpuLogging: Boolean) {}
}
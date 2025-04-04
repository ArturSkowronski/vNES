package vnes.emulator;
/*
vNES
Copyright © 2006-2013 Open Emulation Project

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import vnes.emulator.cpu.CPU;
import vnes.emulator.input.InputHandler;
import vnes.emulator.mappers.MemoryMapper;
import vnes.emulator.memory.MemoryAccess;
import vnes.emulator.ppu.PPU;
import vnes.emulator.producers.ChannelRegistryProducer;
import vnes.emulator.producers.MapperProducer;
import vnes.emulator.ui.*;
import vnes.emulator.utils.Globals;
import vnes.emulator.utils.PaletteTable;

public class NES {

    private GUI gui;
    private CPU cpu;
    private PPU ppu;
    private PAPU papu;

    private Memory cpuMem;
    private Memory ppuMem;
    private Memory sprMem;

    private MemoryMapper memMapper;

    private PaletteTable palTable;

    private ROM rom;
    private String romFile;

    private boolean isRunning = false;

    /**
     * Constructor that takes a GUI directly.
     * 
     * @param gui The GUI implementation to use
     */
    public NES(GUI gui) {
        this.gui = gui;
        initializeConstructor();
    }

    /**
     * Constructor that creates a GUI using a factory and screen view.
     * 
     * @param uiFactory The factory to create UI components
     * @param screenView The screen view to use
     */
    public NES(NESUIFactory uiFactory, ScreenView screenView) {
        InputHandler inputHandler = uiFactory.createInputHandler();
        this.gui = new GUIAdapter(inputHandler, screenView);
        initializeConstructor();
    }

    /**
     * Initialize common components used by all constructors.
     */
    private void initializeConstructor() {
        cpuMem = new Memory(0x10000); // Main memory (internal to CPU)
        ppuMem = new Memory(0x8000);  // VRAM memory (internal to PPU)
        sprMem = new Memory(0x100);   // Sprite RAM  (internal to PPU)

        ppu = new PPU();
        papu = new PAPU(this);
        palTable = new PaletteTable();
        cpu = new CPU(papu, ppu);

        cpu.init(getMemoryAccess(), getCpuMemory());
        ppu.init(
                getGui(),
                getPpuMemory(),
                getSprMemory(),
                getCpuMemory(),
                getCpu(),
                getMemoryMapper(),
                getPapu().getLine(),
                getPalTable()
        );

        papu.init(new ChannelRegistryProducer());
        palTable.init();

        enableSound(true);

        clearCPUMemory();
    }

    public ScreenView getScreenView() {
        return gui.getScreenView();
    }

    public boolean stateLoad(ByteBuffer buf) {

        boolean continueEmulation = false;
        boolean success;

        if (cpu.isRunning()) {
            continueEmulation = true;
            stopEmulation();
        }

        if (buf.readByte() == 1) {
            cpuMem.stateLoad(buf);
            ppuMem.stateLoad(buf);
            sprMem.stateLoad(buf);
            cpu.stateLoad(buf);
            memMapper.stateLoad(buf);
            ppu.stateLoad(buf);
            success = true;
        } else {
            success = false;
        }

        if (continueEmulation) {
            startEmulation();
        }

        return success;

    }

    public void stateSave(ByteBuffer buf) {

        boolean continueEmulation = isRunning();
        stopEmulation();

        // Version:
        buf.putByte((short) 1);

        // Let units save their state:
        cpuMem.stateSave(buf);
        ppuMem.stateSave(buf);
        sprMem.stateSave(buf);
        cpu.stateSave(buf);
        memMapper.stateSave(buf);
        ppu.stateSave(buf);

        // Continue emulation:
        if (continueEmulation) {
            startEmulation();
        }

    }

    public boolean isRunning() {
        return isRunning;
    }

    public void startEmulation() {

        if (Globals.enableSound && !papu.isRunning()) {
            papu.start();
        }

        if (rom != null && rom.isValid() && !cpu.isRunning()) {
            cpu.beginExecution();
            isRunning = true;
        }
    }

    public void stopEmulation() {
        if (cpu.isRunning()) {
            cpu.endExecution();
            isRunning = false;
        }

        if (Globals.enableSound && papu.isRunning()) {
            papu.stop();
        }
    }

    public void reloadRom() {

        if (romFile != null) {
            loadRom(romFile);
        }

    }

    public void clearCPUMemory() {
        // Initialize RAM with a mix of values (0x00, 0xFF, and random bytes)
        // This is more accurate to real NES behavior and fixes issues with games like SMB
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 0x2000; i++) {
            // Use a mix of values: 0x00, 0xFF, and random bytes
            int r = random.nextInt(100);
            if (r < 33) {
                cpuMem.mem[i] = 0x00;
            } else if (r < 66) {
                cpuMem.mem[i] = (short)0xFF;
            } else {
                cpuMem.mem[i] = (short)(random.nextInt(256));
            }
        }

        // Set specific values that are important for proper operation
        for (int p = 0; p < 4; p++) {
            int i = p * 0x800;
            cpuMem.mem[i + 0x008] = 0xF7;
            cpuMem.mem[i + 0x009] = 0xEF;
            cpuMem.mem[i + 0x00A] = 0xDF;
            cpuMem.mem[i + 0x00F] = 0xBF;
        }
    }

    public CPU getCpu() {
        return cpu;
    }

   	public PPU getPpu() {
		return ppu;
	}

    public PAPU getPapu() {
        return papu;
    }

    public Memory getCpuMemory() {
        return cpuMem;
    }

    public Memory getPpuMemory() {
        return ppuMem;
    }

    public Memory getSprMemory() {
        return sprMem;
    }

    public ROM getRom() {
        return rom;
    }

    public GUI getGui() {
        return gui;
    }

    public MemoryMapper getMemoryMapper() {
        return memMapper;
    }

    public MemoryAccess getMemoryAccess() {
        return memMapper;
    }

    public PaletteTable getPalTable() {
        return palTable;
    }

    public boolean loadRom(String file) {

        if (isRunning) {
            stopEmulation();
        }

        rom = new ROM(gui::showLoadProgress, gui::showErrorMsg);
        rom.load(file);
        if (rom.isValid()) {

            // The CPU will load
            // the ROM into the CPU
            // and PPU memory.

            reset();

            MapperProducer mapperProducer = new MapperProducer(gui::showErrorMsg);
            memMapper = mapperProducer.produce(this, rom);

            cpu.setMapper(memMapper);
            ppu.setMapper(memMapper);
            memMapper.loadROM(rom);
            ppu.setMirroring(rom.getMirroringType());

            this.romFile = file;

        }
        return rom.isValid();
    }

    public void reset() {

        if (memMapper != null) {
            memMapper.reset();
        }

        cpuMem.reset();
        ppuMem.reset();
        sprMem.reset();

        clearCPUMemory();

        cpu.reset();
        cpu.init(
            getMemoryAccess(),
            getCpuMemory()
        );
        ppu.reset();
        palTable.reset();
        papu.reset(this);

        InputHandler joy1 = gui.getJoy1();
        if (joy1 != null) {
            joy1.reset();
        }

    }

    public void beginExecution() {
        cpu.beginExecution();
    }

    public void enableSound(boolean enable) {

        boolean wasRunning = isRunning();
        if (wasRunning) {
            stopEmulation();
        }

        if (enable) {
            papu.start();
        } else {
            papu.stop();
        }

        Globals.enableSound = enable;

        if (wasRunning) {
            startEmulation();
        }

    }

    public void menuListener() {
        if (isRunning()) {
            stopEmulation();
            reset();
            reloadRom();
            startEmulation();
        }
    }

    public void destroy() {

        if (cpu != null) {
            cpu.destroy();
        }
        if (ppu != null) {
            ppu.destroy();
        }
        if (papu != null) {
            papu.destroy();
        }
        if (cpuMem != null) {
            cpuMem.destroy();
        }
        if (ppuMem != null) {
            ppuMem.destroy();
        }
        if (sprMem != null) {
            sprMem.destroy();
        }
        if (memMapper != null) {
            memMapper.destroy();
        }
        if (rom != null) {
            rom.destroy();
        }
        if (gui != null) {
            gui.destroy();
        }

        if (getCpu().isRunning()) {
            stopEmulation();
        }

        gui = null;
        cpu = null;
        ppu = null;
        papu = null;
        cpuMem = null;
        ppuMem = null;
        sprMem = null;
        memMapper = null;
        rom = null;
        palTable = null;
    }
}

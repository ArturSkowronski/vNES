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

package knes.applet;

import knes.emulator.ui.GUI;
import knes.emulator.ui.ScreenView;
import knes.emulator.utils.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;

public class AppletScreenView extends JPanel implements ScreenView {

    // knes.emulator.Scale modes:
    public static final int SCALE_NONE = 0;
    public static final int SCALE_HW2X = 1;
    public static final int SCALE_HW3X = 2;
    public static final int SCALE_NORMAL = 3;
    public static final int SCALE_SCANLINE = 4;
    public static final int SCALE_RASTER = 5;
    private final GUI gui;
    private BufferedImage img;
    private VolatileImage vimg;
    private boolean usingMenu = false;
    private Graphics gfx;
    private final int width;
    private final int height;
    private int[] pix;
    private int[] pix_scaled;
    private int scaleMode;
    // FPS counter variables:
    private boolean showFPS = true;
    private long prevFrameTime;
    private String fps;
    private int fpsCounter;
    private final Font fpsFont = new Font("Verdana", Font.BOLD, 10);
    private int bgColor = Color.white.darker().getRGB();
    private boolean notifyImageReady;

    // Constructor
    public AppletScreenView(GUI gui, int width, int height) {

        super(false);
        this.gui = gui;
//        this.nes = nes;
        this.width = width;
        this.height = height;
        this.scaleMode = -1;

    }

    public void setNotifyImageReady(boolean value) {
        this.notifyImageReady = value;
    }

    public void setBgColor(int color) {
        bgColor = color;
    }

    public void setScaleMode(int newMode) {

        if (newMode != scaleMode) {

            // Check differences:
            boolean diffHW = useHWScaling(newMode) != useHWScaling(scaleMode);
            boolean diffSz = getScaleModeScale(newMode) != getScaleModeScale(scaleMode);

            // Change scale mode:
            this.scaleMode = newMode;

            if (diffHW || diffSz) {

                // Create new view:
                createView();

            }

        }

    }

    public void init() {

        setScaleMode(SCALE_NONE);

    }

    private void createView() {

        int scale = getScaleModeScale(scaleMode);

        if (!useHWScaling(scaleMode)) {

            // Create new BufferedImage with scaled width & height:
            img = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);

        } else {

            // Create new BufferedImage with normal width & height:
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // Create graphics object to use for FPS display:
            gfx = img.createGraphics();
            gfx.setFont(fpsFont);


            // Set rendering hints:
            Graphics2D g2d = (Graphics2D) gfx;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            try {

                // Create hardware accellerated image:
                vimg = createVolatileImage(width, height, new ImageCapabilities(true));

            } catch (Exception e) {

                // Unable to create image. Fall back to software scaling:
                //System.out.println("Unable to create HW accellerated image.");
                scaleMode = SCALE_NORMAL;
                img = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);

            }

        }


        // Create graphics object to use for FPS display:
        gfx = img.createGraphics();
        gfx.setFont(fpsFont);


        // Set rendering hints:
        Graphics2D g2d = (Graphics2D) gfx;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);


        // Retrieve raster from image:
        DataBufferInt dbi = (DataBufferInt) img.getRaster().getDataBuffer();
        int[] raster = dbi.getData();


        // Replace current rasters with the one used by the image:
        if (scaleMode == SCALE_NONE || scaleMode == SCALE_HW2X || scaleMode == SCALE_HW3X) {

            pix = raster;
//            nes.getPpu().setBuffer(raster);

        } else {

            pix_scaled = raster;

        }


        // Set background color:
        for (int i = 0; i < raster.length; i++) {
            raster[i] = bgColor;
        }


        // Set component size & bounds:
        setSize(width * scale, height * scale);
        setBounds(getX(), getY(), width * scale, height * scale);


        // Repaint component:
        this.invalidate();
        repaint();


    }

    public void imageReady(boolean skipFrame) {

        if (!Globals.focused) {
            setFocusable(true);
            requestFocus();
            Globals.focused = true;
        }

        // Skip image drawing if minimized or frameskipping:
        if (!skipFrame) {

//            nes.getPpu().setRequestRenderAll(false);
            paint(getGraphics());

        }

        // Notify GUI, so it can write the sound buffer:
        if (notifyImageReady) {
            gui.imageReady(skipFrame);
        }

    }

    public Image getImage() {
        return img;
    }

    public int[] getBuffer() {
        return pix;
    }

    public void update(Graphics g) {
    }

    public boolean scalingEnabled() {
        return scaleMode != SCALE_NONE;
    }

    public int getScaleMode() {
        return scaleMode;
    }

    public boolean useNormalScaling() {
        return (scaleMode == SCALE_NORMAL);
    }

    public void paint(Graphics g) {

        // Skip if not needed:
        if (usingMenu) {
            return;
        }

        if (scaleMode != SCALE_NONE) {

            // Scaled drawing:
            paintFPS(0, 14, g);
            paint_scaled(g);

        } else if (img != null && g != null) {

            // Normal draw:
            paintFPS(0, 14, g);
            g.drawImage(img, 0, 0, null);

        }

    }

    public void paint_scaled(Graphics g) {

        // Skip if not needed:
        if (usingMenu) {
            return;
        }

        if (scaleMode == SCALE_HW2X) {

            // 2X Hardware accellerated scaling.
            if (g != null && img != null && vimg != null) {

                // Draw BufferedImage into accellerated image:
                vimg.getGraphics().drawImage(img, 0, 0, null);

                // Draw accellerated image scaled:
                g.drawImage(vimg, 0, 0, width * 2, height * 2, null);

            }

        } else if (scaleMode == SCALE_HW3X) {

            // 3X Hardware accellerated scaling.
            if (g != null && img != null && vimg != null) {

                // Draw BufferedImage into accellerated image:
                vimg.getGraphics().drawImage(img, 0, 0, null);

                // Draw accellerated image scaled:
                g.drawImage(vimg, 0, 0, width * 3, height * 3, null);

            }

        } else {

            // 2X Software scaling.
            if (g != null && img != null) {

                // Draw big BufferedImage directly:
                g.drawImage(img, 0, 0, width * 2, height * 2, null);

            }

        }

    }

    public void setFPSEnabled(boolean val) {

        // Whether to show FPS count.
        showFPS = val;

    }

    public void paintFPS(int x, int y, Graphics g) {

        // Skip if not needed:
        if (usingMenu) {
            return;
        }

        if (showFPS) {

            // Update FPS count:
            if (--fpsCounter <= 0) {

                long ct = gui.getTimer().currentMicros();
                long frameT = (ct - prevFrameTime) / 45;
                if (frameT == 0) {
                    fps = "FPS: -";
                } else {
                    fps = "FPS: " + (1000000 / frameT);
                }
                fpsCounter = 45;
                prevFrameTime = ct;

            }

            // Draw FPS.
            gfx.setColor(Color.black);
            gfx.fillRect(x, y - gfx.getFontMetrics().getAscent(), gfx.getFontMetrics().stringWidth(fps) + 3, gfx.getFontMetrics().getHeight());
            gfx.setColor(Color.cyan);
            gfx.drawString(fps, x, y);

        }

    }

    public int getBufferWidth() {
        return width;
    }

    public int getBufferHeight() {
        return height;
    }

    public void setUsingMenu(boolean val) {
        usingMenu = val;
    }

    public boolean useHWScaling() {
        return useHWScaling(scaleMode);
    }

    public boolean useHWScaling(int mode) {
        return mode == SCALE_HW2X || mode == SCALE_HW3X;
    }

    public int getScaleModeScale(int mode) {
        if (mode == -1) {
            return -1;
        } else if (mode == SCALE_NONE) {
            return 1;
        } else if (mode == SCALE_HW3X) {
            return 3;
        } else {
            return 2;
        }
    }

    public void destroy() {

//        nes = null;
        img = null;

    }
}

package com.trifectumsoftware.mekapda.client.ui;

import com.wf.gemrender.ui.Canvas;
import com.wf.gemrender.ui.CanvasTarget;
import com.trifectumsoftware.mekapda.client.MekaPdaClientState;

public final class PdaSurface {

    public static final float WIDTH = 400;
    public static final float HEIGHT = 260;

    private static final int MAX_PIXEL_SCALE = 4;
    private static final int MIN_PIXEL_SCALE = 1;
    private static final int CLEAR = 0xFF000000;

    private int pixelScale = 2;
    private int requestedScale = 2;
    private final CanvasTarget target = new CanvasTarget((int) WIDTH * 2, (int) HEIGHT * 2);
    private final PdaView view = new PdaView();
    private boolean lastFocused;
    private boolean bootstrapped;
    private long lastFrame;

    public PdaSurface() {
        MekaPdaClientState.attach(view);
    }

    public PdaView view() {
        return view;
    }

    public int texture() {
        return target.texture();
    }

    public void pixelScale(int value) {
        requestedScale = Math.max(MIN_PIXEL_SCALE, Math.min(MAX_PIXEL_SCALE, value));
    }

    public void update(float mouseX, float mouseY, boolean focused, boolean pressed, double wheel) {
        if (requestedScale != pixelScale) {
            pixelScale = requestedScale;
            target.resize((int) WIDTH * pixelScale, (int) HEIGHT * pixelScale);
        }
        if (!focused) {
            pressed = false;
            wheel = 0;
        }
        if (!bootstrapped) {
            bootstrapped = true;
            view.open();
        } else if (focused && !lastFocused) {
            view.open();
        }
        lastFocused = focused;
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.0f : Math.min(0.1f, (now - lastFrame) / 1000.0f);
        lastFrame = now;
        view.input(mouseX, mouseY, pressed, wheel);

        Canvas canvas = Canvas.shared().begin(target, WIDTH, HEIGHT, CLEAR);
        try {
            view.render(canvas, (int) mouseX, (int) mouseY, focused, dt);
        } finally {
            canvas.end();
        }
    }

    public void close() {
        MekaPdaClientState.detach(view);
        target.close();
    }
}

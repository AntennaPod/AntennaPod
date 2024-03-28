package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.NonNull;

public class WaveformBackground extends BaseBackground {
    protected static final int NUM_PARTICLES = 40;

    public WaveformBackground(Context context) {
        super(context);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(1.1f + 1.1f * i / NUM_PARTICLES - 0.05f, 0, 0, 0));
        }
    }

    @Override
    protected void drawParticle(@NonNull Canvas canvas, Particle p, float width, float height,
                                float innerBoxX, float innerBoxY, float innerBoxSize) {
        float x = (float) (width * p.positionX);
        canvas.drawRect(x, height, x + (1.1f * width) / NUM_PARTICLES,
                (float) (0.95f * height - 0.3f * p.positionY * height), paintParticles);
    }

    @Override
    protected void particleTick(Particle p, long timeSinceLastFrame) {
        p.positionX += 0.0001 * timeSinceLastFrame;
        if (p.positionY <= 0.2 || p.positionY >= 1) {
            p.speed = -p.speed;
            p.positionY -= p.speed * timeSinceLastFrame;
        }
        p.positionY -= p.speed * timeSinceLastFrame;
        if (p.positionX > 1.05f) {
            p.positionX -= 1.1;
            p.positionY = 0.2 + 0.8 * Math.random();
            p.speed = 0.0008 * Math.random() - 0.0004;
        }
    }
}

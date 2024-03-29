package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;

public class WavesBackground extends BaseBackground {
    protected static final int NUM_PARTICLES = 10;

    public WavesBackground(Context context) {
        super(context);
        paintParticles.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(0, 0, 1.0f * i / NUM_PARTICLES, 0));
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        paintParticles.setStrokeWidth(0.05f * getBounds().height());
        super.draw(canvas);
    }

    @Override
    protected void drawParticle(@NonNull Canvas canvas, Particle p, float width, float height,
                                float innerBoxX, float innerBoxY, float innerBoxSize) {
        canvas.drawCircle(width / 2, 1.1f * height, (float) (p.positionZ * 1.2f * height), paintParticles);
    }

    @Override
    protected void particleTick(Particle p, long timeSinceLastFrame) {
        p.positionZ += 0.00005 * timeSinceLastFrame;
        if (p.positionZ > 1f) {
            p.positionZ -= 1f;
        }
    }
}

package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.NonNull;

public class RotatingSquaresBackground extends BaseBackground {
    public RotatingSquaresBackground(Context context) {
        super(context);
        for (int i = 0; i < 16; i++) {
            particles.add(new Particle(
                    0.3 * (float) (i % 4) + 0.05 + 0.1 * Math.random() - 0.05,
                    0.2 * (float) (i / 4) + 0.20 + 0.1 * Math.random() - 0.05,
                    Math.random(), 0.00001 * (2 * Math.random() + 2)));
        }
    }

    @Override
    protected void drawParticle(@NonNull Canvas canvas, Particle p, float width, float height,
                                float innerBoxX, float innerBoxY, float innerBoxSize) {
        float x = (float) (p.positionX * width);
        float y = (float) (p.positionY * height);
        float size = innerBoxSize / 6;
        canvas.save();
        canvas.rotate((float) (360 * p.positionZ), x, y);
        canvas.drawRect(x - size, y - size, x + size, y + size, paintParticles);
        canvas.restore();
    }

    @Override
    protected void particleTick(Particle p, long timeSinceLastFrame) {
        p.positionZ += p.speed * timeSinceLastFrame;
        if (p.positionZ > 1) {
            p.positionZ -= 1;
        }
    }
}
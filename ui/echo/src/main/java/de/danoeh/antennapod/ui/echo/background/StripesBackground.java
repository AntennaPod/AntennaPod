package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.NonNull;

public class StripesBackground extends BaseBackground {
    protected static final int NUM_PARTICLES = 15;

    public StripesBackground(Context context) {
        super(context);
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(2f * i / NUM_PARTICLES - 1f, 0, 0, 0));
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        paintParticles.setStrokeWidth(0.05f * getBounds().width());
        super.draw(canvas);
    }

    @Override
    protected void drawParticle(@NonNull Canvas canvas, Particle p, float width, float height,
                                float innerBoxX, float innerBoxY, float innerBoxSize) {
        float strokeWidth = 0.05f * width;
        float x = (float) (width * p.positionX);
        canvas.drawLine(x, -strokeWidth, x + width, height + strokeWidth, paintParticles);
    }

    @Override
    protected void particleTick(Particle p, long timeSinceLastFrame) {
        p.positionX += 0.00005 * timeSinceLastFrame;
        if (p.positionX > 1f) {
            p.positionX -= 2f;
        }
    }
}

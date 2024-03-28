package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import de.danoeh.antennapod.ui.echo.R;

public abstract class BaseBackground extends Drawable {
    private final Paint paintBackground;
    protected final Paint paintParticles;
    protected final ArrayList<Particle> particles = new ArrayList<>();
    private final int colorBackgroundFrom;
    private final int colorBackgroundTo;
    private long lastFrame = 0;

    public BaseBackground(Context context) {
        colorBackgroundFrom = ContextCompat.getColor(context, R.color.gradient_000);
        colorBackgroundTo = ContextCompat.getColor(context, R.color.gradient_100);
        paintBackground = new Paint();
        paintParticles = new Paint();
        paintParticles.setColor(0xffffffff);
        paintParticles.setFlags(Paint.ANTI_ALIAS_FLAG);
        paintParticles.setStyle(Paint.Style.FILL);
        paintParticles.setAlpha(25);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float width = getBounds().width();
        float height = getBounds().height();
        paintBackground.setShader(new LinearGradient(0, 0, 0, height,
                colorBackgroundFrom, colorBackgroundTo, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paintBackground);

        long timeSinceLastFrame = System.currentTimeMillis() - lastFrame;
        lastFrame = System.currentTimeMillis();
        if (timeSinceLastFrame > 500) {
            timeSinceLastFrame = 0;
        }
        final float innerBoxSize = (Math.abs(width - height) < 0.001f) // Square share version
                ? (0.9f * width) : (0.9f * Math.min(width, 0.7f * height));
        final float innerBoxX = (width - innerBoxSize) / 2;
        final float innerBoxY = (height - innerBoxSize) / 2;

        for (Particle p : particles) {
            drawParticle(canvas, p, width, height, innerBoxX, innerBoxY, innerBoxSize);
            particleTick(p, timeSinceLastFrame);
        }

        drawInner(canvas, innerBoxX, innerBoxY, innerBoxSize);
    }

    protected void drawInner(Canvas canvas, float innerBoxX, float innerBoxY, float innerBoxSize) {
    }

    protected abstract void particleTick(Particle p, long timeSinceLastFrame);

    protected abstract void drawParticle(@NonNull Canvas canvas, Particle p, float width, float height,
                                         float innerBoxX, float innerBoxY, float innerBoxSize);

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    protected static class Particle {
        double positionX;
        double positionY;
        double positionZ;
        double speed;

        public Particle(double positionX, double positionY, double positionZ, double speed) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.positionZ = positionZ;
            this.speed = speed;
        }
    }
}

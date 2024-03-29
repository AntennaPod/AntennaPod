package de.danoeh.antennapod.ui.echo.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import java.util.ArrayList;

public class FinalShareBackground extends BubbleBackground {
    private static final float[][] COVER_POSITIONS = new float[][]{ new float[] {0.0f, 0.0f},
            new float[] {0.4f, 0.0f}, new float[] {0.4f, 0.2f}, new float[] {0.6f, 0.2f}, new float[] {0.8f, 0.2f}};
    private final Paint paintTextMain;
    private final Paint paintCoverBorder;
    private final String heading;
    private final String year;
    private final Drawable logo;
    private final ArrayList<String> favoritePodNames;
    private final ArrayList<Drawable> favoritePodImages;
    private final Typeface typefaceNormal;
    private final Typeface typefaceBold;

    public FinalShareBackground(Context context,
                                ArrayList<String> favoritePodNames, ArrayList<Drawable> favoritePodImages) {
        super(context);
        this.heading = context.getString(R.string.echo_share_heading);
        this.logo = AppCompatResources.getDrawable(context, R.drawable.echo);
        this.favoritePodNames = favoritePodNames;
        this.favoritePodImages = favoritePodImages;
        this.year = String.valueOf(EchoConfig.RELEASE_YEAR);
        typefaceNormal = ResourcesCompat.getFont(context, R.font.sarabun_regular);
        typefaceBold = ResourcesCompat.getFont(context, R.font.sarabun_semi_bold);
        paintTextMain = new Paint();
        paintTextMain.setColor(0xffffffff);
        paintTextMain.setFlags(Paint.ANTI_ALIAS_FLAG);
        paintTextMain.setStyle(Paint.Style.FILL);
        paintCoverBorder = new Paint();
        paintCoverBorder.setColor(0xffffffff);
        paintCoverBorder.setFlags(Paint.ANTI_ALIAS_FLAG);
        paintCoverBorder.setStyle(Paint.Style.FILL);
        paintCoverBorder.setAlpha(70);
    }

    protected void drawInner(Canvas canvas, float innerBoxX, float innerBoxY, float innerBoxSize) {
        paintTextMain.setTextAlign(Paint.Align.CENTER);
        paintTextMain.setTypeface(typefaceBold);
        float headingSize = innerBoxSize / 14;
        paintTextMain.setTextSize(headingSize);
        canvas.drawText(heading, innerBoxX + 0.5f * innerBoxSize, innerBoxY + headingSize, paintTextMain);
        paintTextMain.setTextSize(0.12f * innerBoxSize);
        canvas.drawText(year, innerBoxX + 0.8f * innerBoxSize, innerBoxY + 0.25f * innerBoxSize, paintTextMain);

        float fontSizePods = innerBoxSize / 18; // First one only
        float textY = innerBoxY + 0.62f * innerBoxSize;
        for (int i = 0; i < favoritePodNames.size(); i++) {
            float coverSize = (i == 0) ? (0.4f * innerBoxSize) : (0.2f * innerBoxSize);
            float coverX = COVER_POSITIONS[i][0];
            float coverY = COVER_POSITIONS[i][1];
            RectF logo1Pos = new RectF(innerBoxX + coverX * innerBoxSize,
                    innerBoxY + (coverY + 0.12f) * innerBoxSize,
                    innerBoxX + coverX * innerBoxSize + coverSize,
                    innerBoxY + (coverY + 0.12f) * innerBoxSize + coverSize);
            logo1Pos.inset((int) (0.01f * innerBoxSize), (int) (0.01f * innerBoxSize));
            float radius = (i == 0) ? (coverSize / 16) : (coverSize / 8);
            canvas.drawRoundRect(logo1Pos, radius, radius, paintCoverBorder);
            logo1Pos.inset((int) (0.003f * innerBoxSize), (int) (0.003f * innerBoxSize));
            Rect pos = new Rect();
            logo1Pos.round(pos);
            if (favoritePodImages.size() > i) {
                favoritePodImages.get(i).setBounds(pos);
                favoritePodImages.get(i).draw(canvas);
            } else {
                canvas.drawText(" ...", pos.left, pos.centerY(), paintTextMain);
            }

            paintTextMain.setTextAlign(Paint.Align.CENTER);
            paintTextMain.setTextSize(fontSizePods);
            final float numberWidth = 0.06f * innerBoxSize;
            canvas.drawText((i + 1) + ".", innerBoxX + numberWidth / 2, textY, paintTextMain);
            paintTextMain.setTextAlign(Paint.Align.LEFT);
            String ellipsizedTitle = ellipsize(favoritePodNames.get(i), paintTextMain, innerBoxSize - numberWidth);
            canvas.drawText(ellipsizedTitle, innerBoxX + numberWidth, textY, paintTextMain);
            fontSizePods = innerBoxSize / 24; // Starting with second text is smaller
            textY += 1.3f * fontSizePods;
            paintTextMain.setTypeface(typefaceNormal);
        }

        double ratio = (1.0 * logo.getIntrinsicHeight()) / logo.getIntrinsicWidth();
        logo.setBounds((int) (innerBoxX + 0.1 * innerBoxSize),
                (int) (innerBoxY + innerBoxSize - 0.8 * innerBoxSize * ratio),
                (int) (innerBoxX + 0.9 * innerBoxSize),
                (int) (innerBoxY + innerBoxSize));
        logo.draw(canvas);
    }

    String ellipsize(String string, Paint paint, float maxWidth) {
        if (paint.measureText(string) <= maxWidth) {
            return string;
        }
        while (paint.measureText(string + "…") > maxWidth || string.endsWith(" ")) {
            string = string.substring(0, string.length() - 1);
        }
        return string + "…";
    }
}

package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import me.toptas.fancyshowcase.FancyShowCaseView;
import me.toptas.fancyshowcase.FocusShape;

public class ShowCaseHelper {
    private ShowCaseHelper() {

    }

    public static FancyShowCaseView.Builder brandedShowcase(Activity activity, int message) {
        return new FancyShowCaseView.Builder(activity)
                .focusShape(FocusShape.ROUNDED_RECTANGLE)
                .backgroundColor((activity.getResources().getColor(R.color.antennapod_blue) & 0xffffff) + 0xee000000)
                .roundRectRadius(8)
                .customView(R.layout.welcome, view -> ((TextView) view.findViewById(R.id.message)).setText(message));
    }

    public abstract static class DismissListener implements me.toptas.fancyshowcase.DismissListener {
        @Override
        public void onSkipped(String id) {

        }
    }
}

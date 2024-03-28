package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.BubbleBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;

public class IntroScreen extends EchoScreen {
    private final SimpleEchoScreenBinding viewBinding;

    public IntroScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.echoLogo.setVisibility(View.VISIBLE);
        viewBinding.aboveLabel.setText(R.string.echo_intro_your_year);
        viewBinding.largeLabel.setText(String.format(getEchoLanguage(), "%d", EchoConfig.RELEASE_YEAR));
        viewBinding.belowLabel.setText(R.string.echo_intro_in_podcasts);
        viewBinding.smallLabel.setText(R.string.echo_intro_locally);
        viewBinding.backgroundImage.setImageDrawable(new BubbleBackground(context));
    }

    @Override
    public View getView() {
        return viewBinding.getRoot();
    }

    @Override
    public void postInvalidate() {
        viewBinding.backgroundImage.postInvalidate();
    }
}

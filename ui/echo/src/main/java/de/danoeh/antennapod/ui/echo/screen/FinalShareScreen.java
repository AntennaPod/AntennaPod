package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.FinalShareBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class FinalShareScreen extends EchoScreen {
    private static final int SHARE_SIZE = 1000;
    private static final String TAG = "FinalShareScreen";
    private final SimpleEchoScreenBinding viewBinding;
    private final ArrayList<String> favoritePodNames = new ArrayList<>();
    private final ArrayList<Drawable> favoritePodImages = new ArrayList<>();
    private final FinalShareBackground background;
    private Disposable disposable;

    public FinalShareScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.actionButton.setOnClickListener(v -> share());
        viewBinding.actionButton.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(
                context.getResources(), R.drawable.ic_share, context.getTheme()), null, null, null);
        viewBinding.actionButton.setVisibility(View.VISIBLE);
        viewBinding.actionButton.setText(R.string.share_label);
        background = new FinalShareBackground(context, favoritePodNames, favoritePodImages);
        viewBinding.backgroundImage.setImageDrawable(background);
    }

    @Override
    public View getView() {
        return viewBinding.getRoot();
    }

    @Override
    public void postInvalidate() {
        viewBinding.backgroundImage.postInvalidate();
    }

    private void share() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(SHARE_SIZE, SHARE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            background.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            background.draw(canvas);
            viewBinding.backgroundImage.setImageDrawable(null);
            viewBinding.backgroundImage.setImageDrawable(background);
            File file = new File(UserPreferences.getDataFolder(null), "AntennaPodEcho.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.close();

            Uri fileUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority), file);
            new ShareCompat.IntentBuilder(context)
                    .setType("image/png")
                    .addStream(fileUri)
                    .setText(context.getString(R.string.echo_share, EchoConfig.RELEASE_YEAR))
                    .setChooserTitle(R.string.share_file_label)
                    .startChooser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startLoading(DBReader.StatisticsResult statisticsData) {
        favoritePodNames.clear();
        for (int i = 0; i < 5 && i < statisticsData.feedTime.size(); i++) {
            favoritePodNames.add(statisticsData.feedTime.get(i).feed.getTitle());
        }
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
                () -> {
                    favoritePodImages.clear();
                    for (int i = 0; i < 5 && i < statisticsData.feedTime.size(); i++) {
                        BitmapDrawable cover = new BitmapDrawable(context.getResources(), (Bitmap) null);
                        try {
                            final int size = SHARE_SIZE / 3;
                            final int radius = (i == 0) ? (size / 16) : (size / 8);
                            cover = new BitmapDrawable(context.getResources(), Glide.with(context)
                                    .asBitmap()
                                    .load(statisticsData.feedTime.get(i).feed.getImageUrl())
                                    .apply(new RequestOptions()
                                            .fitCenter()
                                            .transform(new RoundedCorners(radius)))
                                    .submit(size, size)
                                    .get(5, TimeUnit.SECONDS));
                        } catch (Exception e) {
                            Log.d(TAG, "Loading cover: " + e.getMessage());
                        }
                        favoritePodImages.add(cover);
                    }
                    return statisticsData;
                })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(result -> { },
                error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}

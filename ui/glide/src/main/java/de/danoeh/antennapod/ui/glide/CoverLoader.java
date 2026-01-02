package de.danoeh.antennapod.ui.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import de.danoeh.antennapod.ui.common.FallbackImageData;
import de.danoeh.antennapod.ui.common.GenerativeUrlBuilder;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;


public class CoverLoader {

    // imageView Builders

    public static RequestBuilder<Bitmap> with(
            ImageView imageView,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(imageView, Bitmap.class, null, null, urlBuilder);
    }

    public static RequestBuilder<Bitmap> with(
            ImageView imageView,
            RequestOptions options,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(imageView, Bitmap.class, null, options, urlBuilder);
    }

    public static RequestBuilder<Bitmap> with(
            ImageView imageView,
            Float radiusDp,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(imageView, Bitmap.class, radiusDp, null, urlBuilder);
    }

    public static <RT> RequestBuilder<RT> with(
            ImageView imageView,
            @NonNull Class<RT> resourceClass,
            Float radiusDp,
            RequestOptions options,
            GenerativeUrlBuilder urlBuilder
    ) {
        urlBuilder.hasFallbackUrl();
        // if there is a fallback url, the Primary url have to be fetched first
        RequestBuilder<RT> errorBuilder;
        if (urlBuilder.hasFallbackUrl()) {
            // todo, load the primary image

            errorBuilder = with(
                    imageView,
                    resourceClass,
                    radiusDp,
                    options,
                    new GenerativeUrlBuilder(
                            urlBuilder.getFallbackUrl(),
                            urlBuilder.getFallbackText(),
                            urlBuilder.getFeedDownloadUrl(),
                            urlBuilder.isInitialized()
                    )
            );

        } else {
            errorBuilder = Glide.with(imageView)
                    .as(resourceClass)
                    .load(new FallbackImageData(
                            urlBuilder.getFeedDownloadUrl(),
                            urlBuilder.getFallbackText(),
                            urlBuilder.isShowImageWithoutFallbackText()));

            errorBuilder = applyDefaultOptions(errorBuilder, imageView.getContext(), radiusDp);
            if (options != null) {
                errorBuilder = errorBuilder.apply(options);
            }
        }
        String url = urlBuilder.buildUrl();
        RequestBuilder<RT> builder = Glide.with(imageView)
                .as(resourceClass)
                .load(url)
                .dontAnimate()
                .error(errorBuilder);

        builder = applyDefaultOptions(builder, imageView.getContext(), radiusDp);
        if (options != null) {
            builder = builder.apply(options);
        }

        return builder;
    }

    // Context methods

    public static RequestBuilder<Bitmap> with(
            Context context,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(context, Bitmap.class, null, null, urlBuilder);
    }

    public static RequestBuilder<Bitmap> with(
            Context context,
            RequestOptions options,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(context, Bitmap.class, null, options, urlBuilder);
    }

    public static RequestBuilder<Bitmap> with(
            Context context,
            Float radiusDp,
            RequestOptions options,
            GenerativeUrlBuilder urlBuilder
    ) {
        return with(context, Bitmap.class, radiusDp, options, urlBuilder);
    }

    public static <RT> RequestBuilder<RT> with(
            Context context,
            @NonNull Class<RT> resourceClass,
            Float radiusDp,
            RequestOptions options,
            GenerativeUrlBuilder urlBuilder
    ) {
        String url = urlBuilder.buildUrl();

        RequestBuilder<RT> errorBuilder;
        if (urlBuilder.hasFallbackUrl()) {
            // todo, load the primary image
            errorBuilder = with(
                    context,
                    resourceClass,
                    radiusDp,
                    options,
                    new GenerativeUrlBuilder(
                            urlBuilder.getPrimaryUrl(),
                            urlBuilder.getFallbackText(),
                            urlBuilder.getFeedDownloadUrl(),
                            urlBuilder.isInitialized()
                    )
            );
        } else {
            errorBuilder = Glide.with(context)
                    .as(resourceClass)
                    .load(new FallbackImageData(
                            urlBuilder.getFeedDownloadUrl(),
                            urlBuilder.getFallbackText(),
                            urlBuilder.isShowImageWithoutFallbackText()));

            errorBuilder = applyDefaultOptions(errorBuilder, context, radiusDp);
            if (options != null) {
                errorBuilder = errorBuilder.apply(options);
            }
        }
        RequestBuilder<RT> builder = Glide.with(context)
                .as(resourceClass)
                .load(url)
                .error(errorBuilder);

        builder = applyDefaultOptions(builder, context, radiusDp);
        if (options != null) {
            builder = builder.apply(options);
        }

        return builder;
    }

    private static <RT> RequestBuilder<RT> applyDefaultOptions(
            RequestBuilder<RT> builder,
            Context context,
            Float radiusDp) {
        if (radiusDp != null) {
            int radiusPx = dpToPx(context, radiusDp);
            return builder.transform(new FitCenter(), new RoundedCorners(radiusPx))
                    .placeholder(ImagePlaceholder.getDrawable(context, (float) radiusPx));
        }
        return builder.transform(new FitCenter());
    }

    private static int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}

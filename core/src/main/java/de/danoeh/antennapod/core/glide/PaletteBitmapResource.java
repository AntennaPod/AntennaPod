/*
 * Source: https://github.com/bumptech/glide/wiki/Custom-targets#palette-example
 */

package de.danoeh.antennapod.core.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Util;

public class PaletteBitmapResource implements Resource<PaletteBitmap> {
    private final PaletteBitmap paletteBitmap;

    public PaletteBitmapResource(@NonNull PaletteBitmap paletteBitmap) {
        this.paletteBitmap = paletteBitmap;
    }

    @NonNull
    @Override
    public Class<PaletteBitmap> getResourceClass() {
        return PaletteBitmap.class;
    }

    @NonNull
    @Override
    public PaletteBitmap get() {
        return paletteBitmap;
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(paletteBitmap.bitmap);
    }

    @Override
    public void recycle() {
        paletteBitmap.bitmap.recycle();
    }
}
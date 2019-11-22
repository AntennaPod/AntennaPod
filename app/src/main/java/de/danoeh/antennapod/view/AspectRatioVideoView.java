package de.danoeh.antennapod.view;

/*
 * Copyright (C) Google Inc.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class AspectRatioVideoView extends VideoView {


    private int mVideoWidth;
    private int mVideoHeight;
    private float mAvailableWidth = -1;
    private float mAvailableHeight = -1;

    public AspectRatioVideoView(Context context) {
        this(context, null);
    }

    public AspectRatioVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AspectRatioVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mVideoWidth <= 0 || mVideoHeight <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        if (mAvailableWidth < 0 || mAvailableHeight < 0) {
            mAvailableWidth = getWidth();
            mAvailableHeight = getHeight();
        }

        float heightRatio = (float) mVideoHeight / mAvailableHeight;
        float widthRatio = (float) mVideoWidth / mAvailableWidth;

        int scaledHeight;
        int scaledWidth;

        if (heightRatio > widthRatio) {
            scaledHeight = (int) Math.ceil((float) mVideoHeight
                    / heightRatio);
            scaledWidth = (int) Math.ceil((float) mVideoWidth
                    / heightRatio);
        } else {
            scaledHeight = (int) Math.ceil((float) mVideoHeight
                    / widthRatio);
            scaledWidth = (int) Math.ceil((float) mVideoWidth
                    / widthRatio);
        }

        setMeasuredDimension(scaledWidth, scaledHeight);
    }

    /**
     * Source code originally from:
     * http://clseto.mysinablog.com/index.php?op=ViewArticle&articleId=2992625
     *
     * @param videoWidth
     * @param videoHeight
     */
    public void setVideoSize(int videoWidth, int videoHeight) {
        // Set the new video size
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;

        /*
         * If this isn't set the video is stretched across the
         * SurfaceHolders display surface (i.e. the SurfaceHolder
         * as the same size and the video is drawn to fit this
         * display area). We want the size to be the video size
         * and allow the aspectratio to handle how the surface is shown
         */
        getHolder().setFixedSize(videoWidth, videoHeight);

        requestLayout();
        invalidate();
    }

    /**
     * Sets the maximum size that the view might expand to
     * @param width
     * @param height
     */
    public void setAvailableSize(float width, float height) {
        mAvailableWidth = width;
        mAvailableHeight = height;
        requestLayout();
    }

}

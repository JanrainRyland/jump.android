/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Copyright (c) 2011, Janrain, Inc.

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
   other materials provided with the distribution.
 * Neither the name of the Janrain, Inc. nor the names of its
   contributors may be used to endorse or promote products derived from this
   software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 File:   Story.java
 Author: Lilli Szafranski - lilli@janrain.com
 Date:   April 22, 2011
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package com.janrain.android.quickshare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Config;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Story implements Serializable {
    private static final String TAG = Story.class.getSimpleName();

    private String mTitle;
    private String mDate;
    private String mDescription;
    private String mPlainText;
    private String mLink;
    private ArrayList<String> mImageUrls;
    private transient Bitmap mImage;

    private boolean mDescriptionImagesAlreadyScaled;
    private boolean mCurrentlyDownloading;

    public static Story dummyStory() {
        return new Story();
    }

    public Story(String title, String date, String description,
                 String plainText, String link, ArrayList<String> imageUrls) {
        this.mTitle = title;
        this.mDate = date;
        this.mDescription = description;
        this.mPlainText = plainText;
        this.mLink = link;
        this.mImageUrls = imageUrls;

        mDescriptionImagesAlreadyScaled = false;

        if (mImageUrls != null)
            if (!mImageUrls.isEmpty())
                startDownloadImage(mImageUrls.get(0));
    }

    private Story() {
    }

    public void downloadImage() {
        if (mImageUrls != null)
            if (!mImageUrls.isEmpty())
                startDownloadImage(mImageUrls.get(0));
    }

    private void startDownloadImage(final String imageUrl) {
        if (Config.LOGD)
            Log.d(TAG, "[startDownloadImage] " + imageUrl);

        synchronized (this) {
            if (mCurrentlyDownloading) return;
            else mCurrentlyDownloading = true;
        }

        new AsyncTask<Void, Void, Void>(){
            public Void doInBackground(Void... s) {
                if (Config.LOGD)
                    Log.d(TAG, "[doInBackground] downloading image");
                
                try {
                    URL url = new URL(imageUrl);
                    InputStream is = url.openStream();

                    Bitmap bd = BitmapFactory.decodeStream(is);

                    int width = bd.getWidth();
                    int height = bd.getHeight();

                    if (Config.LOGD)
                        Log.d(TAG, "[getView] original image size: " +
                                ((Integer)width).toString() + ", " + ((Integer)height).toString());

                    int x_ratio = width/120, y_ratio = height/120, scale_ratio;

                    if (x_ratio <= 1 || y_ratio <= 1)
                        scale_ratio = 1;
                    else if (x_ratio < y_ratio)
                        scale_ratio = x_ratio;
                    else
                        scale_ratio = y_ratio;

                    mImage = Bitmap.createScaledBitmap(bd, width/scale_ratio, height/scale_ratio, true);

                    if (Config.LOGD)
                        Log.d(TAG, "[getView] scaled image size: " +
                                ((Integer)mImage.getWidth()).toString() + ", " +
                                ((Integer)mImage.getHeight()).toString());

                } catch (MalformedURLException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (IOException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (RuntimeException e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                } catch (OutOfMemoryError e) {
                    if (Config.LOGD) Log.d(TAG, e.toString());
                }

                mCurrentlyDownloading = false;
                return null;
            }

            protected void onPostExecute(Boolean loadSuccess) {
                Log.d(TAG, "[onPostExecute] image download onPostExecute, result: " +
                        (loadSuccess ? "succeeded" : "failed"));

////                if (loadSuccess)
////                    mListener.AsyncFeedReadSucceeded();
////                else
////                    mListener.AsyncFeedReadFailed();
            }
        }.execute();
    }

    public String getTitle() {
            return mTitle;
    }

    public String getDate() {
        return mDate;
    }

    public String getDescription() {
        if (!mDescriptionImagesAlreadyScaled)
            scaleDescriptionImages();

            return mDescription;
    }

    private void scaleDescriptionImages() {
        String[] splitDescription = mDescription.split("<img ", -1);

        int length = splitDescription.length;

        String newDescription = splitDescription[0];

        for (int i=1; i<length; i++) {
            Log.d(TAG, "[scaleDescriptionImages] " + ((Integer)i).toString() + ": " + splitDescription[i]);

            try {
                Pattern pattern = Pattern.compile("(.+?)style=\"(.+?)\"(.+?)/>(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(splitDescription[i]);

                Log.d(TAG, "[scaleDescriptionImages] matcher matches?: " + (matcher.matches() ? "yes" : "no"));

                for (int j=1; j<=matcher.groupCount(); j++)
                    Log.d(TAG, "[scaleDescriptionImages] matched group " + ((Integer)j).toString() + ": " + matcher.group(j));

                newDescription += "<img " + matcher.group(1) +
                        "style=\"" + newWidthAndHeight(matcher.group(2)) + "\"" +
                        matcher.group(3) + "/>" + matcher.group(4);
            } catch (IllegalStateException e) {
                Log.d(TAG, "[scaleDescriptionImages] exception: " + e.getLocalizedMessage());
                newDescription += "<img " + splitDescription[i];
            }
        }

        Log.d(TAG, "[scaleDescriptionImages] newDescription: " + newDescription);

        mDescription = newDescription;
    }

    private String newWidthAndHeight(String style) {
        Pattern patternWidth = Pattern.compile("(.*?)width:(.+?)px(.*)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        Pattern patternHeight = Pattern.compile("(.*?)height:(.+?)px(.*)", Pattern.CASE_INSENSITIVE);

        Matcher matcherWidth = patternWidth.matcher(style);
        Matcher matcherHeight = patternHeight.matcher(style);

        Log.d(TAG, "[newWidthAndHeight] matchers match style (" + style + ")?: " +
                (matcherWidth.matches() ? "width=yes and " : "width=no and ") +
                (matcherHeight.matches() ? "height=yes" : "height=no"));

        if (!matcherWidth.matches() || !matcherHeight.matches())
            return style;

        Integer width;
        Integer height;

        try {
            width = new Integer(matcherWidth.group(2).trim());
            height = new Integer(matcherHeight.group(2).trim());
        } catch (NumberFormatException e) { return style; }

        if (width <= 280)
            return style;

        Double ratio = width / 280.0;
        Integer newHeight = (new Double(height / ratio)).intValue();

        Log.d(TAG, "[newWidthAndHeight] style before: " + style);
        style = style.replace("width:" + matcherWidth.group(2) + "px", "width: 280px");
        style = style.replace("height:" + matcherHeight.group(2) + "px", "height: " + newHeight.toString() + "px");
        Log.d(TAG, "[newWidthAndHeight] style after: " + style);

        return style;
    }

    public String getPlainText() {
        return mPlainText;
    }

    public String getLink() {
        return mLink;
    }

    public ArrayList<String> getImageUrls() {
        return mImageUrls;
    }

    public Bitmap getImage() {
        return mImage;
    }

}
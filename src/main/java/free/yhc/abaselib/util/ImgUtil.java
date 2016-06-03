/******************************************************************************
 * Copyright (C) 2016
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of free.yhc.baselib
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.abaselib.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;

import free.yhc.baselib.Logger;

public class ImgUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(ImgUtil.class, Logger.LOGLV_DEFAULT);

    /**
     * Decode image from file path(String) or raw data (byte[]).
     * @param image Two types are supported.
     *              String for file path / byte[] for raw image data.
     */
    @NonNull
    private static Bitmap
    decodeBitmap(@NonNull Object image, BitmapFactory.Options opt) {
        if (image instanceof String) {
            return BitmapFactory.decodeFile((String) image, opt);
        } else if (image instanceof byte[]) {
            byte[] data = (byte[]) image;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opt);
        }
        P.bug(false);
        //noinspection ConstantConditions
        return null;
    }

    /**
     * Get size(width, height) of Bitmap file.
     * @param bitmap 'image file path(String)' or 'bitmap data(data[])'
     * @param out out[0] : width of image / out[1] : height of image
     * @return false if image cannot be decode. true if success
     */
    private static boolean
    getBitmapSize(@NonNull Object bitmap, @NonNull int[] out) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        //noinspection ConstantConditions
        decodeBitmap(bitmap, opt);
        if (opt.outWidth <= 0 || opt.outHeight <= 0 || null == opt.outMimeType)
            return false;
        out[0] = opt.outWidth;
        out[1] = opt.outHeight;
        return true;
    }

    /**
     * Calculate rectangle(out[]). This is got by fitting  rectangle(width,height) to
     *   bound rectangle(boundW, boundH) with fixed ratio - preserving width-height-ratio.
     * Shrinking is mandatory. But scaling is optional(argument).
     *
     * @param out calculated value [ out[0](width) out[1](height) ]
     * @param scale 'false' to use smaller rectangle as it is.
     * @param boundW width of bound rect
     * @param boundH height of bound rect
     * @param width width of rect to be shrunk
     * @param height height of rect to be shrunk
     * @return true(adjusted) / false(out[] is same with original)
     */
    public static boolean
    adjustFixedRatio(@NonNull int[] out, boolean scale,
                     int boundW, int boundH, int width, int height) {
        P.bug(width > 0 && height > 0);
        // Check size of picture..
        float rw = (float) boundW / (float) width; // width ratio
        float rh = (float) boundH / (float) height; // height ratio

        float ratio = (rw > rh) ? rh : rw; // choose minimum
        if (!scale && ratio >= 1.0f) {
            out[0] = width;
            out[1] = height;
            return false;
        } else {
            // Scaling or shrinking
            // integer-type-casting(rounding down) guarantees that value cannot
            // be greater than bound!!
            out[0] = (int)(ratio * width);
            out[1] = (int)(ratio * height);
            return true;
        }
    }

    /**
     * Make fixed-ration-bounded-bitmap with file.
     * If (0 >= boundW || 0 >= boundH), original-size-bitmap is trying to be created.
     * @param bitmap bitmap file path (absolute path) or raw data (byte[])
     * @param scale 'false' to use smaller bitmap as it is(without scaling).
     * @param boundW bound width
     * @param boundH bound height
     * @return null if fails
     */
    public static Bitmap
    decodeBitmap(@NonNull Object bitmap, boolean scale, int boundW, int boundH) {
        BitmapFactory.Options opt = null;
        if (0 < boundW && 0 < boundH) {
            int[] bitmapsz = new int[2]; // image size : [0]=width / [1] = height
            if (!getBitmapSize(bitmap, bitmapsz))
                // This is not proper image data
                return null;

            int[] bsz = new int[2]; // adjusted bitmap size
            boolean bShrink = adjustFixedRatio(bsz, scale, boundW, boundH, bitmapsz[0], bitmapsz[1]);

            opt = new BitmapFactory.Options();
            opt.inDither = false;
            if (bShrink) {
                // To save memory we need to control sampling rate. (based on
                // width!)
                // for performance reason, we use power of 2.
                if (0 >= bsz[0])
                    return null;

                int sampleSize = 1;
                while (1 < bitmapsz[0] / (bsz[0] * sampleSize))
                    sampleSize *= 2;

                // shrinking based on width ratio!!
                // NOTE : width-based-shrinking may make 1-pixel error in height
                // side!
                // (This is not Math!! And we are using integer!!! we cannot
                // make it exactly!!!)
                opt.inScaled = true;
                opt.inSampleSize = sampleSize;
                opt.inDensity = bitmapsz[0] / sampleSize;
                opt.inTargetDensity = bsz[0];
            }
        }
        return decodeBitmap(bitmap, opt);
    }

    public static Bitmap
    decodeBitmap(Object bitmap, int boundW, int boundH) {
        return decodeBitmap(bitmap, false, boundW, boundH);
    }

    /**
     * Compress give bitmap to JPEG formatted image data.
     */
    @NonNull
    public static byte[]
    compressToJpeg(Bitmap bm, int quality, int initOutStreamSize) {
        long time = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(initOutStreamSize);
        bm.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        if (DBG) P.v("TIME: Compress Image : " + (System.currentTimeMillis() - time));
        return baos.toByteArray();
    }

    @NonNull
    public static byte[]
    compressToJpeg(Bitmap bm) {
        return compressToJpeg(bm, 100, 1024);
    }
}

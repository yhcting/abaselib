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

import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;

/*
 * General utilities that heavily depends on Android Framework.
 */
public class AUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(AUtil.class, Logger.LOGLV_DEFAULT);

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    public static boolean
    isHandlerContext(@NonNull Handler h, @NonNull Thread thread) {
        return h.getLooper().getThread() == thread;
    }

    public static boolean
    isHandlerContext(@NonNull Handler h) {
        return isHandlerContext(h, Thread.currentThread());
    }

    public static boolean
    isUiThread(@NonNull Thread thread) {
        return isHandlerContext(AppEnv.getUiHandler(), thread);
    }

    public static boolean
    isUiThread() {
        return isUiThread(Thread.currentThread());
    }

    /**
     * @return true if same, otherwise false;
     */
    public static boolean
    compareHandlerContext(Handler h0, Handler h1) {
        return h0.getLooper().getThread() == h1.getLooper().getThread();
    }

    public static void
    runOnHandlerContext(@NonNull Handler h,
                        @NonNull Runnable r) {
        if (h.getLooper().getThread() == Thread.currentThread())
            r.run();
        else
            h.post(r);
    }

    @NonNull
    public static View
    inflateLayout(int layout, ViewGroup root) {
        LayoutInflater inflater = (LayoutInflater)AppEnv.getAppContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(layout, root);
    }

    @NonNull
    public static View
    inflateLayout(int layout) {
        return inflateLayout(layout, null);
    }

    @NonNull
    public static Resources
    getResources() {
        return AppEnv.getAppContext().getResources();
    }

    @NonNull
    public static String
    getResString(int id) {
        return getResources().getString(id);
    }

    @NonNull
    public static CharSequence
    getResText(int id) {
        return getResources().getText(id);
    }

    public static int
    dpToPx(int dp) {
        return (int) (dp * AppEnv.getAppContext().getResources().getDisplayMetrics().density);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static File
    createTempFile() throws IOException {
        if (null == AppEnv.getTmpDir())
            throw new IOException("Temp directory is NOT set");
        return File.createTempFile(
                AppEnv.getAppContext().getPackageName() + android.os.Process.myPid(),
                null,
                AppEnv.getTmpDir());
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
}

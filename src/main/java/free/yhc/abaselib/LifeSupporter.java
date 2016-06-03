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

package free.yhc.abaselib;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import org.jetbrains.annotations.NotNull;

import free.yhc.baselib.Logger;
import free.yhc.abaselib.util.AUtil;
import free.yhc.baselib.async.TaskManager;
import free.yhc.baselib.async.TaskManagerBase;
import free.yhc.baselib.async.TmTask;
import free.yhc.baselib.util.Util;

public class LifeSupporter {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(LifeSupporter.class, Logger.LOGLV_DEFAULT);

    private static final String WLTAG = "free.yhc.abaselib.LifeSupporter";

    private PowerManager.WakeLock mWl = null;
    private WifiManager.WifiLock mWfl = null;
    private int mBalanceCnt = 0;
    private final int mWlOpt;
    private final Class<?> mServiceCls;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    public static final int LOCK_WAKE = 0x01;
    public static final int LOCK_WIFI = 0x02;


    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    private void
    getWakeLock() {
        // getWakeLock() and putWakeLock() are used only at TaskManager's owner thread
        // So, we don't need to synchronize it!
        if (DBG) P.i("Get Wakelock");
        P.bug(null == mWl && null == mWfl);
        // Initialize wakelock
        if (Util.bitIsSet(mWlOpt, LOCK_WAKE)) {
            mWl = ((PowerManager) AppEnv.getAppContext().getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WLTAG);
            mWl.acquire();
        }

        if (Util.bitIsSet(mWlOpt, LOCK_WIFI)) {
            mWfl = ((WifiManager) AppEnv.getAppContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, WLTAG);
            mWfl.acquire();
        }
    }

    private void
    putWakeLock() {
        if (DBG) P.i("Put Wakelock");

        // !! NOTE : Important.
        // if below line "mWl = null" is removed, then RuntimeException is raised
        //   when 'getWakeLock' -> 'putWakeLock' -> 'getWakeLock' -> 'putWakeLock(*)'
        //   (at them moment of location on * is marked - last 'putWakeLock').
        // That is, once WakeLock is released, reusing is dangerous in current Android Framework.
        // I'm not sure that this is Android FW's bug... or I missed something else...
        // Anyway, let's set 'mWl' as 'null' here to re-create new WakeLock at next time.
        if (Util.bitIsSet(mWlOpt, LOCK_WAKE)) {
            P.bug(null != mWl);
            mWl.release();
            mWl = null;
        }

        if (Util.bitIsSet(mWlOpt, LOCK_WIFI)) {
            P.bug(null != mWfl);
            mWfl.release();
            mWfl = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    protected LifeSupporter(@NotNull Class<?> serviceCls,
                            int wlopt) {
        mServiceCls = serviceCls;
        mWlOpt = wlopt;
    }
    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NotNull
    public static LifeSupporter
    create(@NotNull Class<?> serviceCls,
           int wlopt) {
        return new LifeSupporter(serviceCls, wlopt);
    }

    public void
    start() {
        if (DBG) P.v("Enter: balanceCnt: " + mBalanceCnt);
        P.bug(mBalanceCnt >= 0 && AUtil.isUiThread());
        if (0 < mBalanceCnt++)
            return;
        Intent i = new Intent(AppEnv.getAppContext(), mServiceCls);
        AppEnv.getAppContext().startService(i);
        getWakeLock();
    }

    public void
    stop() {
        if (DBG) P.v("Enter: balanceCnt: " + mBalanceCnt);
        // start() and stop() SHOULD be always pair!
        P.bug(mBalanceCnt > 0 && AUtil.isUiThread());
        if (0 < --mBalanceCnt)
            return;
        Intent i = new Intent(AppEnv.getAppContext(), mServiceCls);
        AppEnv.getAppContext().stopService(i);
        putWakeLock();
    }

    public static void
    addGuestTaskManager(@NotNull final LifeSupporter lifesup,
                        @NotNull final TaskManager tm) {
        TaskManagerBase.TaskQEventListener listener
            = new TaskManagerBase.TaskQEventListener() {
            @Override
            public void
            onEvent(
                    @NotNull TaskManagerBase tm,
                    @NotNull TaskManagerBase.TaskQEvent ev,
                    int szReady, int szRun,
                    @NotNull TmTask task) {
                switch (ev) {
                case ADDED_TO_READY:
                    if (1 == (szReady + szRun))
                        lifesup.start();
                    break;
                case REMOVED_FROM_READY:
                    if (0 == (szReady + szRun))
                        lifesup.stop();
                    break;
                case MOVED_TO_RUN: // nothing to do.
                    break;
                case REMOVED_FROM_RUN:
                    if (0 == (szReady + szRun))
                        lifesup.stop();
                    break;
                default:
                    P.bug(false);
                }
            }
        };
        tm.setTag(lifesup, listener);
        tm.addTaskQEventListener(AppEnv.getUiHandlerAdapter(), listener);
    }

    public static void
    removeGuestTaskManager(@NotNull LifeSupporter lifesup,
                           @NotNull TaskManager tm) {
        // Owner of tm should be UI thread to use life supporter.
        P.bug(AUtil.isUiThread() && tm.isOwnerThread());
        //noinspection unchecked
        TaskManagerBase.TaskQEventListener listener
                = (TaskManagerBase.TaskQEventListener)tm.removeTag(lifesup);
        P.bug(null != listener);
        assert listener != null;
        //noinspection unchecked
        tm.removeTaskQEventListener(listener);
    }
}

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

package free.yhc.abaselib.ux;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.View;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;
import free.yhc.baselib.adapter.android.AHandlerAdapter;
import free.yhc.baselib.async.Task;
import free.yhc.baselib.async.ThreadEx;
import free.yhc.abaselib.util.AUtil;

/* DialogTask is used not to run time-consuming task at UI thread.
 * That is, this is only for user-responsibility.
 * System's point of view, this decorator is nothing but just overhead.
 * And, this decorator is tightly coupled with user experience.
 * That's why this class inherits not TmTask but Task!
 * (Task should be run as soon as possible. Adding task to TaskManager doesn't meet this requirements.)
 * And, Job assigned to this decorator doesn't added to TaskManager, because even if there is lots of
 * Tasks in TaskManager, this job must NOT wait them.
 * Note that decorated job SHOULD be treated as foreground job!
 */
public class DialogTask extends Task<Object> implements
DialogInterface.OnDismissListener,
View.OnClickListener {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(DialogTask.class, Logger.LOGLV_DEFAULT);

    private static final int MAX_PROGRESS_RANGE = 10000; // See Android API reference document.

    private final Task mTask;
    private final Style mStyle;
    private final ProgressDialog mDialog;
    private final CharSequence mTitle;
    private final CharSequence mMessage;
    private final CharSequence mCancelButtonText ;
    private final CharSequence mWaitingCancelMessage;
    private final DialogInterface.OnDismissListener mOnDismissListener;

    private double mProgressScaleRatio = 1.0;

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    public enum Style {
        SPIN (ProgressDialog.STYLE_SPINNER),
        PROGRESS (ProgressDialog.STYLE_HORIZONTAL);

        private int style;

        Style(int aStyle) {
            style = aStyle;
        }

        public int
        getStyle() {
            return style;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected void
    onEarlyCancel(boolean started, Object param) {
        super.onEarlyCancel(started, param);
        P.bug(AUtil.isUiThread());
        if (started && null != mWaitingCancelMessage)
            mDialog.setMessage(mWaitingCancelMessage);
        mTask.cancel(param);
    }


    @Override
    protected void
    onEarlyCancelled(Exception ex, Object param) {
        super.onEarlyCancelled(ex, param);
        P.bug(AUtil.isUiThread());
        // See comments in onPostRun
        try {
            mDialog.dismiss();
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    protected void
    onEarlyPostRun(Object result, Exception ex) {
        super.onEarlyPostRun(result, ex);
        // This may be called after context(ie. Activity) is destroyed.
        // In this case, dialog is no more attached windowManager and exception is issued.
        // we need to ignore this exception here with out concern.
        try {
            mDialog.dismiss();
        } catch (IllegalArgumentException ignored) { }
    }

    @Override
    protected Object
    doAsync() throws Exception {
        if (DBG) P.v("* Start background Job : DialogTask\n");
        return mTask.startSync();
    }

    @Override
    protected void
    onEarlyProgressInit(long maxProgress) {
        super.onEarlyProgressInit(maxProgress);
        if (maxProgress > MAX_PROGRESS_RANGE)
            mProgressScaleRatio = MAX_PROGRESS_RANGE / (double)maxProgress;
        mDialog.setMax((int)(maxProgress * mProgressScaleRatio));

    }

    @Override
    protected void
    onEarlyProgress(long progress) {
        super.onEarlyProgress(progress);
        mDialog.setProgress((int)(progress * mProgressScaleRatio));
    }

    @Override
    protected void
    onEarlyStarted() {
        super.onEarlyStarted();
        if (null != mTitle)
            mDialog.setTitle(mTitle);
        if (null != mMessage)
            mDialog.setMessage(mMessage);
        mDialog.setProgressStyle(mStyle.getStyle());
        //mDialog.setMax(mMaxProgress);
        // To prevent dialog is dismissed unexpectedly by back-key
        mDialog.setCancelable(false);

        // To prevent dialog is dismissed unexpectedly by search-key (in Gingerbread)
        mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean
            onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return KeyEvent.KEYCODE_SEARCH == keyCode
                        && 0 == event.getRepeatCount();
            }
        });

        mDialog.setOnDismissListener(this);

        // NOTE
        // See below codes.
        // In case of cancelable dialog, set dummy onClick is registered.
        // And then, REAL onClick is re-registered for the button directly.
        // This is to workaround Android Framework's ProgressDialog policy.
        // According to Android Framework, ProgressDialog dismissed as soon as button is clicked.
        // But, this is not what I expected.
        // Below code is a way of workaround this policy.
        // For details, See "https://groups.google.com/forum/?fromgroups=#!topic/android-developers/-1bIchuFASQ".
        if (null != mCancelButtonText) {
            // Set dummy onClick listener.
            mDialog.setButton(Dialog.BUTTON_POSITIVE,
                              mCancelButtonText,
                              new DialogInterface.OnClickListener() {
                                  @Override
                                  public void
                                  onClick(DialogInterface dialog, int which) {
                                      // Cancel button is clicked.
                                  }
                              });
        }
        mDialog.show();
        if (null != mCancelButtonText)
            mDialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    // Instantiation
    //
    ///////////////////////////////////////////////////////////////////////////
    /**
     */
    protected DialogTask(
            @NonNull Context context,
            @NonNull Task task,
            @NonNull Style style,
            boolean interruptOnCancel,
            CharSequence title,
            CharSequence message,
            CharSequence cancelButtonText,
            CharSequence waitingCancelMessage,
            DialogInterface.OnDismissListener listener) {
        super("DialogTask(" + task.getUniqueName() + ")",
              AppEnv.getUiHandlerAdapter(),
              ThreadEx.TASK_PRIORITY_NORM,
              interruptOnCancel);
        mDialog = new ProgressDialog(context);
        mTask = task;
        mStyle  = style;
        mTitle = title;
        mMessage = message;
        mCancelButtonText = cancelButtonText;
        mWaitingCancelMessage = waitingCancelMessage;
        mOnDismissListener = listener;

        //noinspection unchecked
        mTask.addEventListener(new Task.EventListener<Task, Object>() {
            @Override
            public void
            onProgressInit(@NonNull Task task, long maxProgress) {
                DialogTask.this.publishProgressInit(maxProgress);
            }

            @Override
            public void
            onProgress(@NonNull Task task, long progress) {
                DialogTask.this.publishProgress(progress);
            }
        });
    }

    public static class Builder<B extends Builder> extends Task.Builder<B, DialogTask> {
        protected final Context mContext;
        protected final Task mTask;

        protected DialogTask.Style mStyle = DialogTask.Style.SPIN;
        protected CharSequence mTitle = null;
        protected CharSequence mMessage = null;
        protected CharSequence mCancelButtonText = null;
        protected CharSequence mWaitingCancelMessage = null;
        protected DialogInterface.OnDismissListener mOnDismissListener = null;

        public Builder(@NonNull Context context, @NonNull Task task) {
            super();
            mOwner = AppEnv.getUiHandlerAdapter();
            mContext = context;
            mTask = task;
        }

        @NonNull
        public B
        setStyle(@NonNull DialogTask.Style style) {
            mStyle = style;
            //noinspection unchecked
            return (B)this;
        }

        @Override
        @NonNull
        public B
        setPriority(int pri) {
            if (DBG) P.w("Priority for DialogTask is FIXED");
            //noinspection unchecked
            return (B)this;
        }

        public B
        setTitle(@NonNull CharSequence text) {
            mTitle = text;
            //noinspection unchecked
            return (B)this;
        }

        public B
        setTitle(int text) {
            return setTitle(AUtil.getResText(text));
        }

        public B
        setMessage(@NonNull CharSequence text) {
            mMessage = text;
            //noinspection unchecked
            return (B)this;
        }

        public B
        setMessage(int text) {
            return setMessage(AUtil.getResText(text));
        }

        public B
        setCancelButtonText(@NonNull CharSequence text) {
            mCancelButtonText = text;
            //noinspection unchecked
            return (B)this;
        }

        public B
        setCancelButtonText(int text) {
            return setCancelButtonText(AUtil.getResText(text));
        }

        public B
        setWaitingCancelMessage(@NonNull CharSequence text) {
            mWaitingCancelMessage = text;
            //noinspection unchecked
            return (B)this;
        }

        public B
        setWaitingCancelMessage(int text) {
            return setWaitingCancelMessage(AUtil.getResText(text));
        }


        public B
        setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mOnDismissListener = listener;
            //noinspection unchecked
            return (B)this;
        }

        @Override
        @NonNull
        public DialogTask
        create() {
            return new DialogTask(
                    mContext,
                    mTask,
                    mStyle,
                    mInterruptOnCancel,
                    mTitle,
                    mMessage,
                    mCancelButtonText,
                    mWaitingCancelMessage,
                    mOnDismissListener);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    /**
     * This is used usually to avoid "leaked window..." error.
     * But be careful to use it.
     * This function dismiss ONLY dialog and doens't cancel background job!
     */
    public void
    forceDismissDialog() {
        P.bug(AUtil.isUiThread());
        mDialog.dismiss();
    }

    /**
     * handle event - cancel button is clicked.
     */
    @Override
    public void
    onClick(View view) {
        cancel();
    }

    @Override
    public void
    onDismiss(DialogInterface dialogI) {
        if (null != mOnDismissListener)
            mOnDismissListener.onDismiss(dialogI);
    }
}

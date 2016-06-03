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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.Toast;

import free.yhc.abaselib.AppEnv;
import free.yhc.baselib.Logger;

public class UxUtil {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    private static final Logger P = Logger.create(UxUtil.class, Logger.LOGLV_DEFAULT);

    public interface ConfirmAction {
        void onPositive(@NonNull Dialog dialog);
        void onNegative(@NonNull Dialog dialog);
    }

    public static void
    showTextToast(@NonNull CharSequence text) {
        Toast t = Toast.makeText(
                AppEnv.getAppContext(),
                text,
                Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToast(@NonNull CharSequence text, boolean lengthLong) {
        Toast t = Toast.makeText(
                AppEnv.getAppContext(),
                text,
                lengthLong? Toast.LENGTH_LONG: Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToast(int textid) {
        Toast t = Toast.makeText(
                AppEnv.getAppContext(),
                textid,
                Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void
    showTextToastAtBottom(int textid, boolean lengthLong) {
        Toast t = Toast.makeText(
                AppEnv.getAppContext(),
                textid,
                lengthLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 50);
        t.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //
    //
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    public static AlertDialog
    createAlertDialog(@NonNull Context context,
                      int icon,
                      @NonNull CharSequence title,
                      CharSequence message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
        if (0 != icon)
            dialog.setIcon(icon);
        dialog.setTitle(title);
        if (null != message)
            dialog.setMessage(message);
        return dialog;
    }


    @NonNull
    public static AlertDialog
    createAlertDialog(@NonNull Context context, int icon, int title, int message) {
        CharSequence t = context.getResources().getText(title);
        CharSequence msg = (0 == message)? null: context.getResources().getText(message);
        return createAlertDialog(context, icon, t, msg);
    }

    @NonNull
    public static AlertDialog
    buildConfirmDialog(@NonNull final Context context,
                       @NonNull final CharSequence title,
                       @NonNull final CharSequence description,
                       final int icon,
                       @NonNull final CharSequence positiveText,
                       @NonNull final CharSequence negativeText,
                       @NonNull final ConfirmAction action) {
        final AlertDialog dialog = createAlertDialog(context, icon, title, description);
        dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                         positiveText,
                         new DialogInterface.OnClickListener() {
                             @Override
                             public void
                             onClick(DialogInterface diag, int which) {
                                 action.onPositive(dialog);
                                 dialog.dismiss();
                             }
                         });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                         negativeText,
                         new DialogInterface.OnClickListener() {
                             @Override
                             public void
                             onClick(DialogInterface diag, int which) {
                                 action.onNegative(dialog);
                                 diag.dismiss();
                             }
                         });
        return dialog;
    }

    @NonNull
    public static AlertDialog
    buildConfirmDialog(@NonNull final Context context,
                       final int title,
                       final int description,
                       final int icon,
                       final int positiveText,
                       final int negativeText,
                       @NonNull final ConfirmAction action) {
        return buildConfirmDialog(context,
                                  context.getResources().getText(title),
                                  context.getResources().getText(description),
                                  icon,
                                  context.getResources().getText(positiveText),
                                  context.getResources().getText(negativeText),
                                  action);
    }

}

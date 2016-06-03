package free.yhc.abaselib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;

import java.io.IOException;

import free.yhc.baselib.Baselib;
import free.yhc.baselib.Logger;
import free.yhc.baselib.adapter.android.ALoggerAdapter;
import free.yhc.baselib.adapter.android.AHandlerAdapter;
import free.yhc.baselib.adapter.android.ANetConnAdapter;

public class ABaselib {
    private static final boolean DBG = Logger.DBG_DEFAULT;
    // Logger is available after library initalization.
    private static Logger P = null;

    private static boolean sInitialized = false;

    /*
     * Function to verify that library is initialized.
     *
     * Most modules(actually all modules) create Logger module for logging as class static.
     * So, the best place in where this function is called, is constructure of 'Logger' class.
     */
    static void
    verifyLibReady() {
        if (!sInitialized)
            throw new AssertionError("Baselib is NOT initialized");
    }

    public static void
    initLibraryWithExternalStoragePermission(String tmpDir) throws IOException {
        AppEnv.initTmpDir(tmpDir);
    }

    /**
     * Initialize library.
     * This should be called before using any other modules in this library.
     *
     * @param appContext Android application context.
     * @param uiHandler Android UI thread handler.
     * @param defaultOwner default Handler used as owner of async Tasks.
     *                     {@code null} for create brand-new-handler run on new context with
     *                     foreground-app-priority
     */
    public static void
    initLibrary(@NonNull Context appContext,
                @NonNull Handler uiHandler,
                Handler defaultOwner) {
        // initLibrary is called. and library will be initialized anyway.
        // (Initialized or assert!)
        // And we know what we are doing in this library.
        // So, for convenience - especially to use Logger - let's say 'initialized' at early stage.
        if(sInitialized)
            throw new AssertionError();
        sInitialized = true;
        if (null == defaultOwner) {
            HandlerThread ht = new HandlerThread("", Process.THREAD_PRIORITY_FOREGROUND);
            ht.start();
            defaultOwner = new Handler(ht.getLooper());
        }
        Baselib.initLibrary(
                new AHandlerAdapter(defaultOwner),
                new ALoggerAdapter(),
                new ANetConnAdapter(appContext));
        AppEnv.init(appContext, uiHandler);

        P = Logger.create(Baselib.class, Logger.LOGLV_DEFAULT);
        if (DBG) P.v("initLibrary is done");
    }

}

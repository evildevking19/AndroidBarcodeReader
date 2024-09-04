package com.jose.app.barcodereader.asynctask;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UploadAsyncTask {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback<R> {
        void onComplete(R result);
        void onFailure();
    }

    public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final R result = callable.call();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onComplete(result);
                        }
                    });
                } catch (Exception e) {
                    callback.onFailure();
                }
            }
        });
    }
}
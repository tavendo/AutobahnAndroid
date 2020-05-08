package io.crossbar.autobahn.websocket.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import io.crossbar.autobahn.websocket.interfaces.IThreadMessenger;

public class AndroidThreadMessenger implements IThreadMessenger {
    private final Handler mHandler;
    private OnMessageListener mListener;

    public void setOnMessageListener(OnMessageListener listener) {
        mListener = listener;
    }

    public AndroidThreadMessenger() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (mListener != null) {
                    mListener.onMessage(msg.obj);
                }
            }
        };
    }

    @Override
    public void notify(Object message) {
        Message msg = mHandler.obtainMessage();
        msg.obj = message;
        mHandler.sendMessage(msg);
    }

    @Override
    public void postDelayed(Runnable runnable, long delayMillis) {
        mHandler.postDelayed(runnable, delayMillis);
    }
}

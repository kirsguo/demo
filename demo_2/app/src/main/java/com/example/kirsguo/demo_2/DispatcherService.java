package com.example.kirsguo.demo_2;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.util.Log;


public class DispatcherService extends Service {
    private static final String TAG = "DispatcherService";
    private static final int ForMyself = -1;
    private static final int NORMAL_MESSAGE = 0;
    private static final int MESSAGE_FROM_DISPATCHER = 2;


    private volatile boolean mIsWork = false;
    private boolean mIsDestroy = false;
    private Handler UIHandler;

    private DispatcherBinder mBinder = new DispatcherBinder();
    private HandlerThread dispatcherThread;

    //创建Dispatcher的handler
    private DispatcherHandler mHandler;
    final class DispatcherHandler extends Handler{
        public DispatcherHandler(Looper looper) {
            // 将Handler和looper绑定
            super(looper);
//            Log.d(TAG, "DispatcherHandler: "+Thread.currentThread().getId());
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "CurrentThread in Dispatcher: "+Thread.currentThread().getId());
            // TODO:处理消息的应用逻辑
            switch (msg.what) {
//                初始化加载JSON后，向LDM发送订阅消息SubscribeMsg
                case ForMyself:{
                    while (UIHandler == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (UIHandler != null) {
                        Message subMsg = Message.obtain();
                        subMsg.what = NORMAL_MESSAGE;
                        subMsg.arg1 = MESSAGE_FROM_DISPATCHER;
                        subMsg.obj = "hello";
                        UIHandler.sendMessage(subMsg);
                    }
                    break;
                }
                case NORMAL_MESSAGE:{
                    if (msg != null) {
                        Log.i(TAG, "handle UI Message:" + msg.obj.toString());
                    }
                }
                default:
                    break;
            }
        }
    }

    public class DispatcherBinder extends Binder {
        public void setUIHandler(Handler handler) {
            UIHandler = handler;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate: Dispatcher");
        dispatcherThread = new HandlerThread("Dispatcher Service Thread", Process.THREAD_PRIORITY_BACKGROUND);
        dispatcherThread.start();
        mHandler = new DispatcherHandler(dispatcherThread.getLooper());

        if (mHandler != null) {
            Message msg = Message.obtain();
            msg.what = ForMyself;
            msg.obj = "应用处理完毕~~~";
            mHandler.sendMessage(msg);
        }
        mIsDestroy = false;

        super.onCreate();
//        dispatcherThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Dispatcher");

        mIsWork = true;
//        if (mIsDestroy) {
//            dispatcherThread.start();
//        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: UI bind Dispatcher");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: UI unbind Dispatcher");
        mIsWork = false;
        UIHandler = null;
        return false;   // false 可能再次调用onBind()；true 可能调用onRebind()
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: Dispatcher");
        mIsWork = false;
        dispatcherThread.quitSafely();
        mIsDestroy = true;
        super.onDestroy();
    }

}
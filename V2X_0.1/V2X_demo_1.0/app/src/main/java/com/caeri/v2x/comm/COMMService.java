package com.caeri.v2x.comm;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;


public class COMMService extends Service {
    //Context context;

    public Handler LDMhandler;
    public Handler UIhandler;
    public COMMService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mbinder;
    }

    private COMMBinder mbinder=new COMMBinder();
    public class COMMBinder extends Binder{
        //public Handler getHandler(){return shandler;}
        public void setLDMHandler(Handler handler){LDMhandler=handler;}
        public void setUIHandler(Handler handler){UIhandler=handler;}

        public void run(){

            new COMMEntrance(getBaseContext(),LDMhandler,UIhandler).start();
        }



    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

package com.example.kirsguo.demo_2;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;




public class MainActivity extends AppCompatActivity implements LocationSource,AMapLocationListener {
    private static final String TAG = "MainActivity";

    /***
    * 基础地图
    * */
    private MapView mapView;
    private AMap aMap;

    /****
    * 定位
    *   mLocationClient -- 定位发起端
    *   mLocationOption -- 定位参数
    *   mListener -- 定位监听器
    * */
    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;
    private LocationSource.OnLocationChangedListener mListener = null;


    /***
    * 显示问题
    * myStyle -- 定位蓝点
    * isFirstLocate -- 是否第一次定位
    * */
    private MyLocationStyle myStyle;
    private boolean isFirstLocate = true;

    /***
    * 侧滑菜单
    * */
    private DrawerLayout drawerLayout;

    /***
     *服务
     */
    private boolean mIsBound = false;
    private static final int NORMAL_MESSAGE = 0;
    private Handler mDispatcherHandler = null;
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj != null) {
                Log.i(TAG, "handle Dispatcher Message: "+msg.obj.toString());
            }

            Message msg1 = Message.obtain();
            msg1.what = NORMAL_MESSAGE;
            msg1.obj = "Good Bye!";
            mDispatcherHandler.sendMessage(msg1);
        }
    };
//  测试用
    private ImageView mineState;
    private Dialog appDialog = null;
    private ImageView demo2;
    private ImageView demo3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);


        initMap();
        initLocation();
        initService();
        initBindAppService();



        mineState = (ImageView)findViewById(R.id.car_state);
        mineState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] a ={1,2};
                initDialog(a);

            }
        });
        demo2 = (ImageView)findViewById(R.id.application_image);
        demo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] a ={2};
                initDialog(a);
            }
        });
        demo3 = (ImageView)findViewById(R.id.road_state);
        demo3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });



        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected( MenuItem item) {
//                侧滑菜单点击事件
//                drawerLayout.closeDrawers();
                switch (item.getItemId()){
                    case R.id.setting_COMM:actionStart(MainActivity.this,Setting_COMM.class);break;
                    case R.id.setting_LDM:drawerLayout.closeDrawers();break;
                    case R.id.setting_APP:drawerLayout.closeDrawers();break;
                    case R.id.setting_UI:actionStart(MainActivity.this,Setting_UI.class);break;
                }
//                actionStart(MainActivity.this,Main3Activity.class);
                return true;
            }
        });



    }

    /***
     */
    private void initService(){
        Intent intent = new Intent(this, DispatcherService.class);
        startService(intent);

    }
    private void stopService(){
        Intent appIntent = new Intent(this, DispatcherService.class);
        stopService(appIntent);
    }
    private void initBindAppService(){
        if (!mIsBound){
            Intent intent = new Intent(this, DispatcherService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }

    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            DispatcherService.DispatcherBinder mBinder = (DispatcherService.DispatcherBinder) service;
            mIsBound = true;
            mBinder.setUIHandler(uiHandler);
            mDispatcherHandler = mBinder.getHandler();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            mIsBound = false;
        }
    };
    private void unbindAppService(){
        if (mIsBound){
            mIsBound = false;
            unbindService(connection);
        }
    }


    /***
     * 初始化应用弹出框
     * @param apps  应用名称数组
     */
    private void initDialog(int[]apps){
        appDialog = new Dialog(MainActivity.this, R.style.UICustomDialog);//去白框dialog里面具体设置样式

        appDialog.setContentView(GetView(apps));

        appDialog.show();
    }

    /***
     * 传入应用名称返回界面View
     * @param names 应用名称数组
     * @return  dialog的View
     */
    private View GetView(int[]names)
    {

        int sum = names.length;
        View view =super.getLayoutInflater().inflate(R.layout.warning_dialog,(ViewGroup) findViewById(R.id.apps_view));
        for (int name:names){

            switch (name){
                case 1 :
                    view.findViewById(R.id.app_icw).setVisibility(View.VISIBLE);
                    final ImageView icw_image=(ImageView)view.findViewById(R.id.icw_img);
                    icw_image.setImageResource(R.drawable.icw_gif);//绑定数据源

//                    if (sum != 1){
//                        icw_image.setAdjustViewBounds(true);
//                        icw_image.setMaxHeight(dip2px(MainActivity.this,240));
//                        icw_image.setMaxWidth(dip2px(MainActivity.this,482));
//                    }
                    //启动 动画，因为如果没有启动方法，它没办法自己启动
                    icw_image.post(new Runnable() {

                        @Override
                        public void run() {
                            AnimationDrawable animationDrawable=(AnimationDrawable)icw_image.getDrawable();//获取imageview绘画
                            animationDrawable.start();//开始绘画

                        }
                    });
                    break;
                case 2 :
                    view.findViewById(R.id.app_tlosa).setVisibility(View.VISIBLE);

                    break;

            }



        }

        return view;
    }


    /***
     * dp转为px
     * @param context   当前context
     * @param dpValue   dp值
     * @return  px值
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /***
     * 初始化地图
     */
    private void initMap(){

        if (aMap == null){
            aMap = mapView.getMap();

        }
        //设置定位监听
        aMap.setLocationSource(this);
        myStyle = new MyLocationStyle();
        myStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
        myStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        myStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.caeri));
        aMap.setMyLocationStyle(myStyle);
        // 是否显示定位按钮
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        // 是否可触发定位并显示定位层
        aMap.setMyLocationEnabled(true);

        aMap.moveCamera(CameraUpdateFactory.zoomTo(20));



    }

    /***
     * 初始化定位信息
     */
    private void initLocation(){
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    /***
     *定位的回调函数
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {

//                aMapLocation.setLatitude(28);
//                aMapLocation.setLongitude(111);

                if (isFirstLocate){
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(20));

                    isFirstLocate =false;
                    mListener.onLocationChanged(aMapLocation);
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:" + aMapLocation.getErrorCode() + ", errInfo:" + aMapLocation.getErrorInfo());

//                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }

    }

    /***
     * Activity跳转
     * @param context   当前界面
     * @param activityName  跳转界面
     */
    public static void actionStart(Context context,Class activityName) {
        Intent intent = new Intent(context, activityName);
        context.startActivity(intent);
    }

    /***
     * 激活定位
     * @param onLocationChangedListener
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;

    }

    /***
     * 销毁定位
     */
    @Override
    public void deactivate() {

        mListener = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mLocationClient.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);


    }
}

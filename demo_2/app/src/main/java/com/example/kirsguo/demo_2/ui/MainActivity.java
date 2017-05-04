package com.example.kirsguo.demo_2.ui;

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
import android.widget.TextView;

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
import com.example.kirsguo.demo_2.app.DispatcherService;
import com.example.kirsguo.demo_2.R;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.example.kirsguo.demo_2.util.Constants.APP_ID_APPIntersectionCollisionWarning;
import static com.example.kirsguo.demo_2.util.Constants.APP_ID_APPTrafficLightOptimalSpeedAdvisory;
import static com.example.kirsguo.demo_2.util.Constants.APP_ID_Me;
import static com.example.kirsguo.demo_2.util.Constants.APP_ID_TrafficSign;
import static com.example.kirsguo.demo_2.util.Constants.ERROR_FROM_COMM;
import static com.example.kirsguo.demo_2.util.Constants.ERROR_FROM_DISPATCHER;
import static com.example.kirsguo.demo_2.util.Constants.ERROR_FROM_LDM;
import static com.example.kirsguo.demo_2.util.Constants.ERROR_MESSAGE;
import static com.example.kirsguo.demo_2.util.Constants.NORMAL_MESSAGE;


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
     * 应用信息
     */
    private HashMap<String,HashMap<String,String>> apps_tmp  = new HashMap<String,HashMap<String,String>>();
    private HashMap<String,HashMap<String,String>> apps_last = new HashMap<String,HashMap<String,String>>();
    private HashSet<String> appName_tmp = new HashSet<String>();
    private HashSet<String> appName_last = new HashSet<String>();
    private boolean isSameApps = true;
    /***
     *服务
     */
    private boolean mIsBound = false;

    private Handler mDispatcherHandler = null;
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null) {
//                Log.i(TAG, "handle Dispatcher Message: "+msg.obj.toString());
                switch (msg.what){
                    case NORMAL_MESSAGE:
                        isSameApps = true;
                        if (apps_last.isEmpty()){
                            apps_last = (HashMap<String,HashMap<String,String>>)msg.obj;
                            Set<String> last = apps_last.keySet();
                            for (String appname:last){
                                appName_last.add(appname);
                            }
                            isSameApps = false;
                        }else {
                            appName_tmp = new HashSet<String>();
//                            Log.i(TAG, "handleMessage: "+appName_last.toString()+apps_last.toString());
                            apps_tmp = (HashMap<String,HashMap<String,String>>)msg.obj;
                            Set<String> tmp = apps_tmp.keySet();
                            for (String appname:tmp){
                                appName_tmp.add(appname);
                            }
                            if (!appName_last.equals(appName_tmp)){
                                isSameApps =false;
                            }

                            appName_last = appName_tmp;
                            apps_last = apps_tmp;
                            appName_tmp = null;
                            apps_tmp =  null;
//                            Log.i(TAG, "handleMessage: "+"last:"+appName_last.toString()+appName_last.size());
                        }
                        if (isSameApps){

                            updateDialog();
                        }else if ((appName_last.size() - 2) == 0){
                            if (appDialog.isShowing()){
                                appDialog.dismiss();
                            }
                            initDialog();
                            updateDialog();
                        }
                        break;
                    case ERROR_MESSAGE:
                        switch (msg.arg1){
                            case ERROR_FROM_COMM:break;
                            case ERROR_FROM_LDM:break;
                            case ERROR_FROM_DISPATCHER:break;
                            default:break;

                        }
                        break;
                }

            }

//            Message msg1 = Message.obtain();
//            msg1.what = NORMAL_MESSAGE;
//            msg1.obj = "Good Bye!";
//            mDispatcherHandler.sendMessage(msg1);
        }
    };
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
    /***
     * 主界面控件
     */
    private TextView car_speed;
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
                HashSet<String> a =new HashSet<String>();
                a.add(APP_ID_APPIntersectionCollisionWarning);
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("collisionLevel","1");
                map.put("RVToMe","right");
                apps_last.put(APP_ID_APPIntersectionCollisionWarning,map);
                appName_last = a;
                initDialog();
                updateDialog();
                Message msg1 = Message.obtain();
                msg1.what = NORMAL_MESSAGE;
                msg1.obj = "Good Bye!";
                mDispatcherHandler.sendMessage(msg1);


            }
        });
        demo2 = (ImageView)findViewById(R.id.application_image);
        demo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashSet<String> a =new HashSet<String>();
                a.add(APP_ID_APPIntersectionCollisionWarning);
                a.add(APP_ID_APPTrafficLightOptimalSpeedAdvisory);
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("collisionLevel","1");
                map.put("RVToMe","left");
                HashMap<String,String> map_1 = new HashMap<String, String>();
                map_1.put("leftStatus","2");
                map_1.put("leftTimeLeft","1");
                map_1.put("leftMaxSpeed","50");
                map_1.put("leftMinSpeed","5");
                map_1.put("headStatus","1");
                map_1.put("headTimeLeft","20");
                map_1.put("headMaxSpeed","100");
                map_1.put("headMinSpeed","10");
                map_1.put("rightStatus","7");
                apps_last.put(APP_ID_APPIntersectionCollisionWarning,map);
                apps_last.put(APP_ID_APPTrafficLightOptimalSpeedAdvisory,map_1);
                appName_last = a;
                initDialog();
                updateDialog();
                Message msg1 = Message.obtain();
                msg1.what = NORMAL_MESSAGE;
                msg1.obj = "Good Bye!";
                mDispatcherHandler.sendMessage(msg1);
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
     * 初始化服务
     */
    private void initService(){
        Intent intent = new Intent(this, DispatcherService.class);
        startService(intent);

    }
    /***
     * 停止服务
     */
    private void stopService(){
        Intent appIntent = new Intent(this, DispatcherService.class);
        stopService(appIntent);
    }
    /***
     * 绑定服务
     */
    private void initBindAppService(){
        if (!mIsBound){
            Intent intent = new Intent(this, DispatcherService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }

    }
    /***
     * 解绑服务
     */
    private void unbindAppService(){
        if (mIsBound){
            mIsBound = false;
            unbindService(connection);
        }
    }


    /***
     * 初始化应用弹出框
     *
     */
    private void initDialog(){
        if (appDialog != null){
            appDialog.dismiss();
        }else {
            appDialog = new Dialog(MainActivity.this, R.style.UICustomDialog);//去白框dialog里面具体设置样式
        }
        appDialog.setContentView(GetView());


        appDialog.show();
    }

    /***
     * 返回界面View
     *
     * @return  dialog的View
     */
    private View GetView()
    {
        HashSet<String> names = appName_last;
        int sum = names.size() - 2;
        View view =super.getLayoutInflater().inflate(R.layout.warning_dialog,(ViewGroup) findViewById(R.id.apps_view));
        for (String name:names){
            switch (name){
                case APP_ID_APPIntersectionCollisionWarning:
                    HashMap<String,String> app_1 = apps_last.get(APP_ID_APPIntersectionCollisionWarning);
                    if (app_1.get("collisionLevel").equals("1")){
                        view.findViewById(R.id.app_icw).setVisibility(View.VISIBLE);
                    }
                    break;
                case APP_ID_APPTrafficLightOptimalSpeedAdvisory :
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
     *更新应用数据
     */
    private void updateDialog(){
        for (String appName:apps_last.keySet() ){
            switch (appName){
                case APP_ID_Me:
                    HashMap<String,String> app_1 = apps_last.get(APP_ID_Me);
                    car_speed.setText(app_1.get("speed")+"KM/H");
                    break;
                case APP_ID_TrafficSign:break;
                case APP_ID_APPIntersectionCollisionWarning:
                    HashMap<String,String> app_3 = apps_last.get(APP_ID_APPIntersectionCollisionWarning);
                    if (app_3.get("collisionLevel").equals("1")){
                        final ImageView icw_image=(ImageView)appDialog.findViewById(R.id.icw_img);
                        if (app_3.get("RVToMe").equals("left")){
                            icw_image.setImageResource(R.drawable.icw_gif);//绑定数据源
                        }else {
                            icw_image.setImageResource(R.drawable.icw_gif_right);//绑定数据源
                        }
                        //启动 动画，因为如果没有启动方法，它没办法自己启动
                        icw_image.post(new Runnable() {

                            @Override
                            public void run() {
                                AnimationDrawable animationDrawable=(AnimationDrawable)icw_image.getDrawable();//获取imageview绘画
                                animationDrawable.start();//开始绘画

                            }
                        });

                    }
                    break;
                case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                    HashMap<String,String> app_4 = apps_last.get(APP_ID_APPTrafficLightOptimalSpeedAdvisory);
                    if (!app_4.get("leftStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_left).setVisibility(View.VISIBLE);
                        ImageView left_img = (ImageView)appDialog.findViewById(R.id.tlosa_left_img);
                        switch (app_4.get("leftStatus")){
                            case "1":
                                left_img.setImageResource(R.drawable.tlosa_left_green);break;
                            case "2":
                                left_img.setImageResource(R.drawable.tlosa_left_yellow);break;
                            case "3":
                                left_img.setImageResource(R.drawable.tlosa_left_red);break;
                            case "4":
                                left_img.setImageResource(R.drawable.tlosa_left_red);break;
                            case "5":
                                left_img.setImageResource(R.drawable.tlosa_left_green);break;
                            case "6":
                                left_img.setImageResource(R.drawable.tlosa_left_yellow);break;
                        }
                        TextView left_text = (TextView)appDialog.findViewById(R.id.tlosa_left_value);
                        left_text.setText(app_4.get("leftTimeLeft")+"s");
                        TextView left_max = (TextView)appDialog.findViewById(R.id.tlosa_left_max_value);
                        left_max.setText(app_4.get("leftMaxSpeed"));
                        TextView left_min = (TextView)appDialog.findViewById(R.id.tlosa_left_min_value);
                        left_min.setText(app_4.get("leftMinSpeed"));

                    }
                    if (!app_4.get("headStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_head).setVisibility(View.VISIBLE);
                        ImageView head_img = (ImageView)appDialog.findViewById(R.id.tlosa_head_img);
                        switch (app_4.get("headStatus")){
                            case "1":
                                head_img.setImageResource(R.drawable.tlosa_head_green);break;
                            case "2":
                                head_img.setImageResource(R.drawable.tlosa_head_yellow);break;
                            case "3":
                                head_img.setImageResource(R.drawable.tlosa_head_red);break;
                            case "4":
                                head_img.setImageResource(R.drawable.tlosa_head_red);break;
                            case "5":
                                head_img.setImageResource(R.drawable.tlosa_head_green);break;
                            case "6":
                                head_img.setImageResource(R.drawable.tlosa_head_yellow);break;
                        }
                        TextView head_text = (TextView)appDialog.findViewById(R.id.tlosa_head_value);
                        head_text.setText(app_4.get("headTimeLeft")+"s");
                        TextView head_max = (TextView)appDialog.findViewById(R.id.tlosa_head_max_value);
                        head_max.setText(app_4.get("headMaxSpeed"));
                        TextView head_min = (TextView)appDialog.findViewById(R.id.tlosa_head_min_value);
                        head_min.setText(app_4.get("headMinSpeed"));

                    }
                    if (!app_4.get("rightStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_right).setVisibility(View.VISIBLE);
                        ImageView right_img = (ImageView)appDialog.findViewById(R.id.tlosa_right_img);
                        switch (app_4.get("rightStatus")){
                            case "1":
                                right_img.setImageResource(R.drawable.tlosa_right_green);break;
                            case "2":
                                right_img.setImageResource(R.drawable.tlosa_right_yellow);break;
                            case "3":
                                right_img.setImageResource(R.drawable.tlosa_right_red);break;
                            case "4":
                                right_img.setImageResource(R.drawable.tlosa_right_red);break;
                            case "5":
                                right_img.setImageResource(R.drawable.tlosa_right_green);break;
                            case "6":
                                right_img.setImageResource(R.drawable.tlosa_right_yellow);break;
                        }
                        TextView right_text = (TextView)appDialog.findViewById(R.id.tlosa_right_value);
                        right_text.setText(app_4.get("headTimeLeft")+"s");
                        TextView right_max = (TextView)appDialog.findViewById(R.id.tlosa_right_max_value);
                        right_max.setText(app_4.get("headMaxSpeed"));
                        TextView right_min = (TextView)appDialog.findViewById(R.id.tlosa_right_min_value);
                        right_min.setText(app_4.get("headMinSpeed"));

                    }
                    break;
            }
        }


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

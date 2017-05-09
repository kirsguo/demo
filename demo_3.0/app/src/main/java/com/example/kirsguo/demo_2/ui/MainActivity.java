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
import com.example.kirsguo.demo_2.app.DispatcherService;
import com.example.kirsguo.demo_2.R;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static com.example.kirsguo.demo_2.util.Constants.*;

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
     * UI间隔
     */
    public static  int UI_SIGN = 2;
    public static  int UI_IntersectionCollisionWarning = 2;
    public static  int UI_TrafficLightOptimalSpeedAdvisory = 1;
    /***
     * 应用信息
     */

    private HashMap<String,ArrayList<HashMap<String,String>>> app_last = new HashMap<>();
    private HashMap<String,ArrayList<HashMap<String,String>>> apps_tmp = new HashMap<>();
    private HashMap<String,ArrayList<HashMap<String,String>>> apps_last = new HashMap<>();
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
                switch (msg.what){
                    case NORMAL_MESSAGE:
                        HashMap<String,ArrayList<HashMap<String,String>>> tmp =(HashMap<String,ArrayList<HashMap<String,String>>>)msg.obj;
                        app_last = tmp;
                        for (String name:tmp.keySet()){
                            switch (name){
                                case APP_ID_APPMe:
                                    updateUIMessage(APP_ID_APPMe);
                                    if (!appName_last.isEmpty()){
                                        if (appName_last.contains(APP_ID_APPTrafficSign)){
                                            UI_SIGN --;
                                            if (UI_SIGN < 0){
                                                sign.setVisibility(View.INVISIBLE);
                                                appName_last.remove(APP_ID_APPTrafficSign);
                                                apps_last.remove(APP_ID_APPTrafficSign);
                                            }
                                        }
                                        if (appName_last.contains(APP_ID_APPIntersectionCollisionWarning)){
                                            UI_IntersectionCollisionWarning --;
                                            if (UI_IntersectionCollisionWarning < 0){
                                                appName_last.remove(APP_ID_APPIntersectionCollisionWarning);
                                                apps_last.remove(APP_ID_APPIntersectionCollisionWarning);
                                                appDialog.dismiss();
                                                initDialog();
                                            }
                                        }
                                        if (appName_last.contains(APP_ID_APPTrafficLightOptimalSpeedAdvisory)){
                                            UI_TrafficLightOptimalSpeedAdvisory --;
                                            if (UI_TrafficLightOptimalSpeedAdvisory < 0){
                                                appName_last.remove(APP_ID_APPTrafficLightOptimalSpeedAdvisory);
                                                apps_last.remove(APP_ID_APPTrafficLightOptimalSpeedAdvisory);
                                                appDialog.dismiss();
                                                initDialog();
                                            }
                                        }
                                    }
                                    break;
                                case APP_ID_APPTrafficSign:
                                    if (UI_SIGN < 0){
                                        sign.setVisibility(View.VISIBLE);
                                        appName_last.add(APP_ID_APPTrafficSign);
                                        apps_last.put(APP_ID_APPTrafficSign,tmp.get(APP_ID_APPTrafficSign));
                                    }
                                    UI_SIGN = 2 ;
                                    updateUIMessage(APP_ID_APPTrafficSign);
                                    break;
                                case APP_ID_APPIntersectionCollisionWarning:
                                    for (HashMap<String,String> one :tmp.get(APP_ID_APPIntersectionCollisionWarning)){
                                        if (one.get("collisionLevel").equals("1")){
                                            apps_last.put(APP_ID_APPIntersectionCollisionWarning,tmp.get(APP_ID_APPIntersectionCollisionWarning));
                                            if (UI_IntersectionCollisionWarning < 0 || !appName_last.contains(APP_ID_APPIntersectionCollisionWarning)){
                                                appName_last.add(APP_ID_APPIntersectionCollisionWarning);
                                                initDialog();
                                                updateUICustomDialog();
                                            }else {

                                                updateUICustomDialog();
                                            }

                                            UI_IntersectionCollisionWarning = 2;
                                        }
                                    }
                                    break;
                                case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                                    apps_last.put(APP_ID_APPTrafficLightOptimalSpeedAdvisory,tmp.get(APP_ID_APPTrafficLightOptimalSpeedAdvisory));
                                    if (UI_TrafficLightOptimalSpeedAdvisory < 0 || !appName_last.contains(APP_ID_APPTrafficLightOptimalSpeedAdvisory)){
                                        appName_last.add(APP_ID_APPTrafficLightOptimalSpeedAdvisory);
                                        initDialog();
                                        updateUICustomDialog();
                                    }else {

                                        updateUICustomDialog();
                                    }

                                    UI_TrafficLightOptimalSpeedAdvisory = 1;

                            }
                        }
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
    private Dialog appDialog = null;
    private UIErrorDialog errorDialog = null;
    /***
     * 多媒体
     */
    UISoundPool uiSoundPool = new UISoundPool();
//  测试用
    private ImageView mineState;
    private ImageView sign;
    private ImageView demo2;


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
        uiSoundPool.initSoundPool(this);



        mineState = (ImageView)findViewById(R.id.car_state);
        mineState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashSet<String> a =new HashSet<String>();
                a.add(APP_ID_APPIntersectionCollisionWarning);
                HashMap<String,String> map = new HashMap<String, String>();
                map.put("collisionLevel","1");
                map.put("RVToMe","right");
                map.put("type","1");
                ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String, String>>();
                list.add(map);
                apps_last.put(APP_ID_APPIntersectionCollisionWarning,list);
                appName_last = a;
                initDialog();
                updateUICustomDialog();
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
                map.put("type","1");
                ArrayList<HashMap<String,String>> list_1 =new ArrayList<HashMap<String, String>>();
                list_1.add(map);
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
                map_1.put("type","2");
                ArrayList<HashMap<String,String>> list_2 =new ArrayList<HashMap<String, String>>();
                list_2.add(map_1);
                apps_last.put(APP_ID_APPIntersectionCollisionWarning,list_1);
                apps_last.put(APP_ID_APPTrafficLightOptimalSpeedAdvisory,list_2);
                appName_last = a;
                initDialog();
                updateUICustomDialog();
                Message msg1 = Message.obtain();
                msg1.what = NORMAL_MESSAGE;
                msg1.obj = "Good Bye!";
                mDispatcherHandler.sendMessage(msg1);
            }
        });
        sign = (ImageView)findViewById(R.id.road_state);
        sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initErrorDialog("错误","按时付款了就爱好看了伐");
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
    private void initErrorDialog(String title,String message){
        errorDialog = new UIErrorDialog(MainActivity.this);
        errorDialog.setTitle(title);
        errorDialog.setMessage(message);
        errorDialog.setYesOnclickListener("确定", new UIErrorDialog.onYesOnclickListener() {
            @Override
            public void onYesClick() {
                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                errorDialog.dismiss();
                unbindAppService();
                startActivity(i);
            }
        });
        errorDialog.show();
    }


    /***
     * 初始化应用弹出框
     *
     */
    private void initDialog(){

        if (!appName_last.isEmpty()){
            HashSet<String> tmp_list = appName_last;
            if (tmp_list.contains(APP_ID_APPTrafficSign)){
                tmp_list.remove(APP_ID_APPTrafficSign);
            }
            if (!tmp_list.isEmpty()){
                if (appDialog != null){
                    appDialog.dismiss();
                }else {
                    appDialog = new Dialog(MainActivity.this, R.style.UICustomDialog);//去白框dialog里面具体设置样式
                }

                appDialog.setContentView(GetView());

                appDialog.show();
                int min =3;
                for (String appName:tmp_list){
                    int tmp = Integer.valueOf(apps_last.get(appName).get(0).get("type"));
                    if (tmp < min ){
                        min = tmp;
                    }
                }
                uiSoundPool.play(min);
            }
        }


    }

    /***
     * 返回界面View
     *
     * @return  dialog的View
     */
    private View GetView()
    {
        HashSet<String> names = appName_last;
        View view =super.getLayoutInflater().inflate(R.layout.warning_dialog,(ViewGroup) findViewById(R.id.apps_view));
        for (String name:names){
            switch (name){
                case APP_ID_APPIntersectionCollisionWarning:
                    view.findViewById(R.id.app_icw).setVisibility(View.VISIBLE);
                    break;
                case APP_ID_APPTrafficLightOptimalSpeedAdvisory :
                    view.findViewById(R.id.app_tlosa).setVisibility(View.VISIBLE);
                    break;
            }
        }


        return view;
    }
    /***
     *更新应用数据
     */
    private void updateUIMessage(String name){
        if (!app_last.isEmpty()){

            switch (name){
                case APP_ID_APPMe:
                    HashMap<String,String> app_me = app_last.get(APP_ID_APPMe).get(0);
                    car_speed.setText(app_me.get("speed")+"KM/H");
                    break;
                case APP_ID_APPTrafficSign:
                    HashMap<String,String> app_sign = apps_last.get(APP_ID_APPTrafficSign).get(0);
                    switch (app_sign.get("signType")){
                        case "0":sign.setImageResource(R.drawable.warning);break;
                        case "1":sign.setImageResource(R.drawable.slippy);break;
                        case "2":sign.setImageResource(R.drawable.curve);break;
                        case "3":sign.setImageResource(R.drawable.rock);break;
                        case "4":sign.setImageResource(R.drawable.construction);break;
                    }
                    uiSoundPool.play(2);
                    break;
            }
        }

    }
    /***
     *更新弹出应用数据
     */
    private void updateUICustomDialog(){
        for (String appName:apps_last.keySet() ){
            switch (appName){
                case APP_ID_APPIntersectionCollisionWarning:
                    for (HashMap<String,String> one :apps_last.get(APP_ID_APPIntersectionCollisionWarning)){
                        if (one.get("collisionLevel").equals("1")){
                            final ImageView icw_image=(ImageView)appDialog.findViewById(R.id.icw_img);
                            if (one.get("RVToMe").equals("left")){
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
                    }
                    break;
                case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                    HashMap<String,String> app = apps_last.get(APP_ID_APPTrafficLightOptimalSpeedAdvisory).get(0);
                    if (!app.get("leftStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_left).setVisibility(View.VISIBLE);
                        ImageView left_img = (ImageView)appDialog.findViewById(R.id.tlosa_left_img);
                        switch (app.get("leftStatus")){
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
                        left_text.setText(app.get("leftTimeLeft"));
                        TextView left_max = (TextView)appDialog.findViewById(R.id.tlosa_left_max_value);
                        left_max.setText(app.get("leftMaxSpeed"));
                        TextView left_min = (TextView)appDialog.findViewById(R.id.tlosa_left_min_value);
                        left_min.setText(app.get("leftMinSpeed"));

                    }
                    if (!app.get("headStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_head).setVisibility(View.VISIBLE);
                        ImageView head_img = (ImageView)appDialog.findViewById(R.id.tlosa_head_img);
                        switch (app.get("headStatus")){
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
                        head_text.setText(app.get("headTimeLeft"));
                        TextView head_max = (TextView)appDialog.findViewById(R.id.tlosa_head_max_value);
                        head_max.setText(app.get("headMaxSpeed"));
                        TextView head_min = (TextView)appDialog.findViewById(R.id.tlosa_head_min_value);
                        head_min.setText(app.get("headMinSpeed"));

                    }
                    if (!app.get("rightStatus").equals("7")){
                        appDialog.findViewById(R.id.tlosa_right).setVisibility(View.VISIBLE);
                        ImageView right_img = (ImageView)appDialog.findViewById(R.id.tlosa_right_img);
                        switch (app.get("rightStatus")){
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
                        right_text.setText(app.get("headTimeLeft"));
                        TextView right_max = (TextView)appDialog.findViewById(R.id.tlosa_right_max_value);
                        right_max.setText(app.get("headMaxSpeed"));
                        TextView right_min = (TextView)appDialog.findViewById(R.id.tlosa_right_min_value);
                        right_min.setText(app.get("headMinSpeed"));

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
        aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
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

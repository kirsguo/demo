package com.caeri.v2x.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.caeri.v2x.R;
import com.caeri.v2x.app.DispatcherService;
import com.caeri.v2x.comm.COMMService;
import com.caeri.v2x.ldm.LDMService;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.caeri.v2x.util.Constants.*;

public class MainActivity extends AppCompatActivity  {
    private static final String TAG = "MainActivity";
    /***
     * 地图
     * mapView 地图视图
     * mapController 控制器
     * myLocationOverlay 自身定位覆盖物
     * item_mine 自身覆盖物的信息（经纬度与图标）
     * otherLocationOverlay 其他车辆定位覆盖物
     * item_others 其他覆盖物的信息（经纬度与图标）
     * othersMessage 其他车辆碰撞等级小于1的信息（经纬度）
     */
    private MapView mapView;
    private IMapController mapController;
    private ItemizedIconOverlay<OverlayItem> myLocationOverlay ;
    private ArrayList<OverlayItem> item_mine = new ArrayList<OverlayItem>();
    private ArrayList<OverlayItem> item_others = new ArrayList<OverlayItem>();
    private ItemizedIconOverlay<OverlayItem> otherLocationOverlay;
    private ArrayList<HashMap<String,String>> othersMessage = new ArrayList<>();
    /***
     * 主界面控件
     * mineState 车辆自身状态
     * car_speed 车辆自身速度
     * appIcon 触发应用图标
     * appName 触发应用名称
     * sign 道路信息
     */
    private ImageView mineState;
    private TextView car_speed ;
    private ImageView appIcon;
    private TextView appName;
    private ImageView sign;
    private Dialog appDialog = null;
    private UIErrorDialog errorDialog = null;
    /***
     * 多媒体（音效）
     */
    UISoundPool uiSoundPool = new UISoundPool();
    /***
    * 侧滑菜单
    * */
    private DrawerLayout drawerLayout;
    /***
     * UI间隔计数器
     * UI_SIGN 交通信号计数器
     * UI_IntersectionCollisionWarning 碰撞警告计数器
     * UI_TrafficLightOptimalSpeedAdvisory 红绿灯诱导计数器
     */
    public static  int UI_SIGN = UI_INTERVAL_TrafficSign;
    public static  int UI_IntersectionCollisionWarning = UI_INTERVAL_IntersectionCollisionWarning;
    public static  int UI_TrafficLightOptimalSpeedAdvisory = UI_INTERVAL_TrafficLightOptimalSpeedAdvisory;
    /***
     * 应用信息
     * app_last 收到的应用信息（一个）
     * apps_last 触发的应用信息（多个）
     * appName_last 触发的应用名称（多个）
     */

    private HashMap<String,ArrayList<HashMap<String,String>>> app_last = new HashMap<>();
    private HashMap<String,ArrayList<HashMap<String,String>>> apps_last = new HashMap<>();
    private HashSet<String> appName_last = new HashSet<>();
    /***
     *服务
     */
    private boolean mIsBound = false;
    /***
     * mDispatcherHandler app层的通信handler
     * uiHandler    ui层handler（处理收到的应用消息）
     */
    private Handler mDispatcherHandler = null;
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null) {
                Log.i(TAG, "handleMessage: 这是收到的信息--"+msg.toString());
                switch (msg.what){
                    case NORMAL_MESSAGE:
//                        Log.i(TAG, "handleMessage: 这是收到正常信息内容--"+msg.obj.toString());
                        HashMap<String,ArrayList<HashMap<String,String>>> tmp =(HashMap<String,ArrayList<HashMap<String,String>>>)msg.obj;
                        app_last = tmp;
                        //如果弹出框被手动关闭 清空apps_last与appName_last
                        if (appDialog == null || !appDialog.isShowing()){
                            apps_last.clear();
                            appName_last.clear();
                        }
                        for (String name:tmp.keySet()){
                            switch (name){
                                case APP_ID_APPMe:
                                    updateUIMessage(APP_ID_APPMe);
                                    //收到ME信息 减少触发应用的间隔
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
                                                othersMessage.clear();
                                                updateUIMessage(APP_ID_APPIntersectionCollisionWarning);
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
                                    appName_last.add(APP_ID_APPTrafficSign);
                                    apps_last.put(APP_ID_APPTrafficSign,tmp.get(APP_ID_APPTrafficSign));
                                    if (UI_SIGN < 0){
                                        sign.setVisibility(View.VISIBLE);
                                    }
                                    UI_SIGN = UI_INTERVAL_TrafficSign ;
                                    updateUIMessage(APP_ID_APPTrafficSign);
                                    break;
                                case APP_ID_APPIntersectionCollisionWarning:
                                    othersMessage.clear();
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

                                            UI_IntersectionCollisionWarning = UI_INTERVAL_IntersectionCollisionWarning;
                                        }else {
                                            othersMessage.add(one);
                                        }
                                    }
                                    updateUIMessage(APP_ID_APPIntersectionCollisionWarning);

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

                                    UI_TrafficLightOptimalSpeedAdvisory = UI_INTERVAL_TrafficLightOptimalSpeedAdvisory;
                                    break;
                                default:break;

                            }
                        }
                    case ERROR_MESSAGE:
                        switch (msg.arg1){
                            case ERROR_FROM_COMM:initErrorDialog("comm",String.valueOf(msg.arg2));break;
                            case ERROR_FROM_LDM:initErrorDialog("ldm",String.valueOf(msg.arg2));break;
                            case ERROR_FROM_DISPATCHER:initErrorDialog("app",String.valueOf(msg.arg2));break;
                            default:break;

                        }
                        break;
                }

            }

        }
    };
    /***
     * 与dispatcherHandler连接
     */
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initPermissions();
        initService();
        initBindAppService();
        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/osmdroid/Mapnik.zip");
        if (!file.exists()){
            initErrorDialog("地图文件丢失","osmdroid文件夹中Mapnik.zip为地图离线文件 删除后无法使用");
        }else {
            Log.i(TAG, "onCreate:11111111 "+file.getPath());
        }

        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setMultiTouchControls(true);
        mapView.setUseDataConnection(false);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapController = mapView.getController();

        mapController.setZoom(19);
        mapController.setCenter(new GeoPoint(29.67271867, 106.4798025));

        uiSoundPool.initSoundPool(this);

        mineState = (ImageView)findViewById(R.id.car_state);
        car_speed = (TextView)findViewById(R.id.car_speed);
        appIcon = (ImageView)findViewById(R.id.application_image);
        appName = (TextView)findViewById(R.id.application_text);
        sign = (ImageView)findViewById(R.id.road_state);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected( MenuItem item) {
                //menu点击事件
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
        try {
            Intent intent_comm = new Intent(this, COMMService.class);
            startService(intent_comm);

            Intent intent_ldm = new Intent(this, LDMService.class);
            startService(intent_ldm);

            Intent intent_app = new Intent(this, DispatcherService.class);
            startService(intent_app);


        } catch (Exception e) {

        }

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
     * 初始化错误弹框
     * @param title 标题
     * @param message 内容
     */
    private void initErrorDialog(String title,String message){
        errorDialog = new UIErrorDialog(MainActivity.this);
        errorDialog.setTitle(title);
        errorDialog.setMessage(message);
        errorDialog.setYesOnclickListener("确定", new UIErrorDialog.onYesOnclickListener() {
            @Override
            public void onYesClick() {
//                //重启app
//                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
//                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                errorDialog.dismiss();
//                unbindAppService();
//                startActivity(i);
                //关闭app
                android.os.Process.killProcess(android.os.Process.myPid());
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
     * 更新应用数据
     * @param name
     * （APP_ID_APPMe）更新自身车辆信息 现在暂时为车辆速度与地图上显示定位
     * （APP_ID_APPTrafficSign）更新道理标志信息 现在暂为五种 同时触发音效
     * （APP_ID_APPIntersectionCollisionWarning）更新低于1等级的碰撞信息 车辆的位置显示与颜色（2为橙色 3为绿色）
     */
    private void updateUIMessage(String name){
        if (!app_last.isEmpty()){
            switch (name){
                case APP_ID_APPMe:
                    HashMap<String,String> app_me = app_last.get(APP_ID_APPMe).get(0);
                    car_speed.setText(app_me.get("speed")+"KM/H");
                    Drawable drawable = this.getResources().getDrawable(R.drawable.location_me,getTheme());
                    if (myLocationOverlay == null){
                        myLocationOverlay =new ItemizedIconOverlay<OverlayItem>(item_mine, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                            @Override
                            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                                return false;
                            }

                            @Override
                            public boolean onItemLongPress(int index, OverlayItem item) {
                                return false;
                            }
                        },getApplicationContext());
                    }else {
                        mapView.getOverlays().remove(this.myLocationOverlay);
                        myLocationOverlay.removeAllItems();
                        item_mine.clear();
                    }
                    OverlayItem item = new OverlayItem("ME", "I`m from CAERI", new GeoPoint(Double.valueOf(app_me.get("latitude")), Double.valueOf(app_me.get("longitude"))));
                    item.setMarker(drawable);
                    item_mine.add(item);
                    this.myLocationOverlay.addItems(item_mine);
                    mapView.getOverlays().add(this.myLocationOverlay);
                    mapView.invalidate();
                    mapController.setCenter(new GeoPoint(Double.valueOf(app_me.get("latitude")), Double.valueOf(app_me.get("longitude"))));

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
                case APP_ID_APPIntersectionCollisionWarning:
                    if (otherLocationOverlay == null) {
                        otherLocationOverlay = new ItemizedIconOverlay<OverlayItem>(item_others, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                            @Override
                            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                                return false;
                            }

                            @Override
                            public boolean onItemLongPress(int index, OverlayItem item) {
                                return false;
                            }
                        }, getApplicationContext());
                    }else {
                        mapView.getOverlays().remove(this.otherLocationOverlay);
                        otherLocationOverlay.removeAllItems();
                        item_others.clear();
                    }
                    if (othersMessage.isEmpty()){
                      mapView.invalidate();
                    }else {
                      OverlayItem item_one;
                      double RVLatitude;
                      double RVLongitude;
                      Drawable drawable_2 = this.getResources().getDrawable(R.drawable.location_2,getTheme());
                      Drawable drawable_3 = this.getResources().getDrawable(R.drawable.location_3,getTheme());
                      for (HashMap<String,String> one :othersMessage){
                        RVLatitude = Double.valueOf(one.get("RVLatitude"));
                        RVLongitude =Double.valueOf(one.get("RVLongitude"));
                        item_one = new OverlayItem("Other", "I`m from CAERI", new GeoPoint(RVLatitude,RVLongitude));
                        if (one.get("collisionLevel").equals("2")){
                          item_one.setMarker(drawable_2);
                        }else {
                          item_one.setMarker(drawable_3);
                        }

                        item_others.add(item_one);
                      }
                      this.otherLocationOverlay.addItems(item_others);
                      mapView.getOverlays().add(this.otherLocationOverlay);
                      mapView.invalidate();
                    }
                    break;
                default:break;

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
     * 初始化时申请动态权限
     */
    private void initPermissions(){
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }
    }

    /***
     * 申请动态权限的返回结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            initErrorDialog("权限问题！","使用地图与定位必须同意所有权限才可运行，请前往设置界面修改权限重启程序");
                            finish();
                            return;
                        }
                    }

                }else {
                    initErrorDialog("未知错误！","申请动态权限时出错，未知原因！");
                    finish();
                }
                break;
            default:
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindAppService();
    }
}

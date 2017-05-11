package com.caeri.v2x.ldm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.caeri.v2x.util.Constants.*;

import com.caeri.v2x.comm.*;
import com.caeri.v2x.util.GPSSite;


public class LDMService extends Service {
    private static final String TAG = "LDMService";


    /**
     * LDM Core Data:<br>
     * Me me   <br>
     * Map<String, Map<String, List<Object>>> roadNetwork   <br>
     * Map<String, Map<String, List<Object>>> participants  <br>
     * <p>
     * <p>
     * me保存自身相关信息，如自身车辆信息，所在Link、Lane等等。
     * 该结构信息每隔一段较短的时间周期性地更新。
     * </p>
     * <p>
     * roadNetwork保存局部路网结构，如Link、Lane、Node、Sign、TrafficLight等等。
     * 该结构每隔一段较长的时间周期性的更新。特别地，Me所在路口的TrafficLight信息随Me同时更新。
     * </p>
     * <p>
     * <p>
     * participants保存Me周围实时检测到的交通参与者的信息。
     * 该结构信息采用即时更新。
     * </p>
     * <p>
     * roadNetwork 和 participate 的类型的含义为 Map<SourceID,Map<ClassID, List<Obj>>>。
     * </p>
     */
    private Me me = new Me();
    private HashMap<String, HashMap<String, ArrayList<Object>>> roadNetwork = new HashMap<>();
    private HashMap<String, HashMap<String, ArrayList<Object>>> participants = new HashMap<>();

    /**
     * 匹配订阅信息时使用的结构：<br>
     * Map<String, Map<String, String>> subscribedMsgs   <br>
     * Map<String, Map<String, List<Object>>> publishedMsgs <br>
     * Map<String, Map<String, Boolean>> publishFlag
     * <p>
     * <p>
     * subscribedMsgs 的类型的含义为 Map< ClassID , Map< APPID, FilterCode >>。
     * 保存所有应用订阅的所有信息。 FilterCode 使用16位字符表示订阅的类的空间信息。<br>
     * FONT、BACK、LEFT 和 RIGHT 相应的比特位为 1 时表示订阅符合该条件的实例，0 表示不订阅。
     * ND 表示 Not Defined。LINK、LANE、MOVEMENT 和 CHOOSEPHASE 均占用 2 个（字符），
     * 00 表示不订阅，01 表示只订阅与 me 相同的实例，10 表示只订阅与 me不同的实例，
     * 11 表示 Not Defined。如果 FilterCode 中编码表示“不订阅”，
     * 则 LDM 应该“不加筛选地”返回对应类所有的实例。
     * </p>
     * <p>
     * publishedMsgs 的类型的含义为 Map< APPID , Map< ClassID , List< Obj >>>。
     * 保存上一次发送给 Dispatcher 层的信息。
     * </p>
     * <p>
     * publishFlags 的类型的含义为 Map< APPID , Map< ClassID ,  Flag >>，
     * 是一个用来初始化 toPublishFlag 结构的零值。在收到subscribedMsgs时构造。
     * 其中， Flag 为 TRUE 时表示 APP 订阅的某个 Class 至少有一个实例满足 FilterCode 。
     * </p>
     */
    private HashMap<String, HashMap<String, String>> subscribedMsgs = new HashMap<>();
    private HashMap<String, HashMap<String, ArrayList<Object>>> publishedMsgs = new HashMap<>();
//    private HashMap<String, HashMap<String, Boolean>> publishFlags;


    private LDMHandler ldmHandler;
    private HandlerThread ldmThread;
    private Handler toDispatcherHander = null;
    private Handler toUIHandler = null;
    private boolean mIsCommBound = false;
    private final LDMBinder mBinder = new LDMBinder();


    /**
     * LDMHandler
     * 和线程消息队列关联，并处理消息循环中的消息。
     */
    final class LDMHandler extends Handler {
        public LDMHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage: thread id:" + Thread.currentThread().getId());
            Log.i(TAG, "handleMessage: in handle message");
            Log.i(TAG, "handleMessage: what======" + msg.what);
            Log.i(TAG, "handleMessage: arg1======" + msg.arg1);
            Log.i(TAG, "handleMessage: arg2======" + msg.arg2);
            Log.i(TAG, "handleMessage: msg.obj");

            switch (msg.what) {
                case NORMAL_MESSAGE:
                    if (msg.arg1 == MESSAGE_FROM_DISPATCHER) {
                        // 接收Dispatcher发送的订阅信息
                        subscribedMsgs = (HashMap<String, HashMap<String, String>>) msg.obj;
                        Log.i(TAG, "handleMessage: "+subscribedMsgs.toString());
//                        subscribedMsgs.putAll((HashMap<String, HashMap<String, String>>)msg.obj);
//                        subscribedMsgs = (HashMap<String, HashMap<String, String>>) ((HashMap<String, HashMap<String,String>>) msg.obj).clone();
                    }
                    if (msg.arg1 == MESSAGE_FROM_COMM) {
                        // 处理COMM发过来的数据信息
                        HashMap<String, HashMap<String, ArrayList<Object>>> data = (HashMap<String, HashMap<String, ArrayList<Object>>>) msg.obj;
                        updateData(data);
                    }
                    break;
                case ERROR_MESSAGE: // COMM出错后发送的信息
                    // TODO 错误信息处理
                    Log.i(TAG, "handleMessage: error message");

//                    HashMap<String, HashMap<String, ArrayList<Object>>> data =
//                            (HashMap<String, HashMap<String, ArrayList<Object>>>) msg.obj;
//                    updateData(data);
                    break;
                default:
                    break;
            }
        }
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsCommBound = true;
            COMMService.COMMBinder binder = (COMMService.COMMBinder) service;
            binder.setLDMHandler(ldmHandler);
            binder.setUIHandler(toUIHandler);
            binder.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsCommBound = false;
        }
    };


    final public class LDMBinder extends Binder {
        public void setDispatcherHandler(Handler handler) {
            toDispatcherHander = handler;
        }

        public void setUIHanler(Handler handler) {
            toUIHandler = handler;
        }

        public Handler getLdmHander() {
            return ldmHandler;
        }
    }


    /**
     * 将服务的接口返回，以便与客户端建立通信。
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "bind ldm ");

        Log.i(TAG, "onCreate: " + ldmHandler.toString());
//        ldmHandler.sendMessage(Message.obtain(null, 1));

        bindService(new Intent(LDMService.this, COMMService.class),
                connection, BIND_AUTO_CREATE);

        Log.i(TAG, "onBind: bindService done");
        return mBinder;
    }


    /**
     * 服务被创建时调用。
     * <p>
     * 当服务第一次被使用（通过startService启动或者bindService绑定）时被系统调用。
     * </p>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: " + Thread.currentThread().getId());

        ldmThread = new HandlerThread("LDM Service Thread", Process.THREAD_PRIORITY_BACKGROUND);
        ldmThread.start();
        ldmHandler = new LDMHandler(ldmThread.getLooper());
    }

    /**
     * 服务被解绑时调用。
     * <p>
     * 该方法被调用的时候，将会把dispatcherHandler设为null。
     * </p>
     *
     * @param intent
     * @return true:onBind() method will be recalled; false:onRebind() method will be recalled.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        if (mIsCommBound) {
            unbindService(connection);
            mIsCommBound = false;
        }
        mBinder.setUIHanler(null);
        mBinder.setDispatcherHandler(null);
        return true;
    }


    /**
     * 服务被销毁时调用，需要释放服务所占用的资源，如线程等。
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");

        ldmThread.quitSafely();
        super.onDestroy();
    }

    /**
     * 服务被启动时调用。
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }


    /**
     * 根据消息中的data维护LDM Core Data，更新完成后调用matchSubscribedMsgs()尝试s匹配订阅消息。
     *
     * @param data
     */
    private void updateData(HashMap<String, HashMap<String, ArrayList<Object>>> data) {
        Log.i(TAG, "updateData: ");

        boolean isMeUpdate = false;
        for (Map.Entry<String, HashMap<String, ArrayList<Object>>> sourceItem : data.entrySet()) {
            for (Map.Entry<String, ArrayList<Object>> classItem : sourceItem.getValue().entrySet()) {
                // 根据CLASS_ID进行处理
                if (classItem.getValue().size() == 0) {
                    continue;
                }
                Log.i(TAG, "updateData: classItem.getKey()"+classItem.getKey());
                switch (classItem.getKey()) {
                    case CLASS_ID_ME:
                        Log.i(TAG, "updateData: CLASS_ID_ME");
                        me.setVehicle((BSMVehicle) classItem.getValue().get(0));
                        Log.i(TAG, "BoxID:" + Arrays.toString(me.getVehicle().getBoxID()) +
                                "ParticipantID:" + me.getVehicle().getParticipantID());
                        isMeUpdate = true;
                        break;
                    // 交通参与者
                    case CLASS_ID_BSMVEHICLE:
                        if (SOURCE_WAVE.equals(sourceItem.getKey())) {
                            // 识别自己
//                            final double ME_EPSON = 1.5;    // 单位m
                            if (me.getVehicle()==null){
                                return;
                            }
                            GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(),
                                    me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
                            ArrayList<Object> arrayList = sourceItem.getValue().get(CLASS_ID_BSMVEHICLE);
                            for (int i = 0; i < arrayList.size(); i++) {
                                BSMVehicle vehicle = (BSMVehicle) arrayList.get(i);
                                GPSSite site = new GPSSite(vehicle.getLatitude(),
                                        vehicle.getLongitude(), vehicle.getNSEW());

                                double distance = GPSSite.getDistance(mySite, site);
                                // 单位换算成cm，然后比较
                                BigDecimal b1 = BigDecimal.valueOf(distance * 10000);
                                BigDecimal b2 = BigDecimal.valueOf(EPSILON_DISTANCE * 10);

                                if (b1.compareTo(b2) <= 0) {    // 距离足够小，判定为自己
                                    arrayList.remove(i);    // 将判定为自己的对象移除
                                }
                            }
                        }
                    case CLASS_ID_RSUSELF:
                    case CLASS_ID_RSUOTHER:
                    case CLASS_ID_BSMMOTOR:
                    case CLASS_ID_BSMPEDEST:
                        participants.put(sourceItem.getKey(), sourceItem.getValue());
                        Log.i(TAG, "updateData: " + classItem.getValue().get(0).getClass());
                        participants.get(sourceItem.getKey())
                                .put(classItem.getKey(), classItem.getValue());
                        break;
                    // 逻辑路网
                    case CLASS_ID_BSMSIGN:
                    case CLASS_ID_BSMNODE:
                    case CLASS_ID_BSMTRAFFICLIGHT:
                        if (classItem.getKey().equals(CLASS_ID_BSMTRAFFICLIGHT)) {
                            BSMTrafficLight trafficLight = (BSMTrafficLight) classItem.getValue().get(0);
                            Log.i(TAG, "updateData: TrafficLight:"
                                    + trafficLight.getPhase()[0].getTimeLeft());
                        }
                    case CLASS_ID_BSMLINK:
                    case CLASS_ID_BSMLANE:
                    case CLASS_ID_BSMMOVEMENT:
                    case CLASS_ID_BSMCHOOSEPHASE:
                        Log.i(TAG, "updateData: recieve road network data------------------------------------");
                        Log.i(TAG, "updateData: " + classItem.getKey());
                        roadNetwork.put(sourceItem.getKey(), sourceItem.getValue());
                        Log.i(TAG, "updateData: roadNetwork.size()="+roadNetwork.size()+"-------------" +
                                sourceItem.getValue().entrySet()+"---------------");
                        Log.i(TAG, "updateData: roadNetwork sourceID="+sourceItem.getKey()+" classID="+classItem.getKey());

                        if (classItem.getKey().equals(CLASS_ID_BSMSIGN)) {  // Sign 直接发送
                            // send a message about sign
                            Message msg = Message.obtain();
                            HashMap<String, ArrayList<Object>> map = new HashMap<String, ArrayList<Object>>() {{
                                put(CLASS_ID_ME_FROM_LDM, classItem.getValue());
                            }};
                            HashMap<String, HashMap<String, ArrayList<Object>>> tosend = new HashMap<String, HashMap<String, ArrayList<Object>>>() {{
                                put(APP_ID_APPTrafficSign, map);
                            }};
                            msg.obj = tosend;

                            Log.i(TAG, "updateData: Sign.content==============");
                            Log.i(TAG, msg.obj.toString());

                            toDispatcherHander.sendMessage(msg);
                            publishedMsgs.put(APP_ID_APPMe, map);
                        }
                        Log.i(TAG, "updateData: " + classItem.getValue().get(0).getClass());
//                        roadNetwork.get(sourceItem.getKey())
//                                .put(classItem.getKey(), classItem.getValue());
                        break;
                    default:
                        break;
                }
            }
        }

        if (isMeUpdate) {
            updateMe();
        }
//        Log.i(TAG, "updateData: " + participants.size());
//        Log.i(TAG, "updateData: " + roadNetwork.size());
        // 尝试匹配订阅消息
        Log.i(TAG, "updateData: next to match msgs");
        if (roadNetwork.size()!=0 || participants.size()!=0){
            matchSubscribedMsgs();
        }
    }


    /**
     * 根据me.getVehicle()信息更新me相关的link，lane，movement和choosePhase
     * 并发送一个Message给Dispatcher
     */
    private void updateMe() {
        // 更新和me相关的信息，并发送一个Message
        Log.i(TAG, "updateMe: ");
        updateMyLink();
        updateMyLane();
        updateMyNodes();
        updateMyMovements();
        updateMyChoosePhase();

        Log.i(TAG, "updateMe: send a message");
        // send a message
        assert toDispatcherHander != null;
        // send a message


        Message msg = Message.obtain();
        HashMap<String, ArrayList<Object>> map = new HashMap<String, ArrayList<Object>>() {{
            put(CLASS_ID_ME_FROM_LDM, new ArrayList<Object>() {{
                add(me);
            }});
        }};
        HashMap<String, HashMap<String, ArrayList<Object>>> tosend = new HashMap<String, HashMap<String, ArrayList<Object>>>() {{
            put(APP_ID_APPMe, map);
        }};
        msg.obj = tosend;
        Log.i(TAG, "updateMe: me.content==============");

        Log.i(TAG, msg.obj.toString());

        toDispatcherHander.sendMessage(msg);
        publishedMsgs.put(APP_ID_APPMe, map);
    }

    /**
     * 将所有node以HashMap<LocalID, Node>的形式保存到me
     */
    private void updateMyNodes() {
        if (!valid(roadNetwork, CLASS_ID_BSMNODE)) {
            return;
        }
        HashMap<Byte, BSMNode> nodes = new HashMap<>();
        ArrayList<Object> nodesList = allInstances(CLASS_ID_BSMNODE);
        for (Object o : nodesList) {
            BSMNode node = (BSMNode) o;
            nodes.put(node.getLocalID(), node);
        }
        me.setNodes(nodes);
    }


    /**
     * 根据me所在link的toNodeLocalID更新ChoosePhase
     */
    private void updateMyChoosePhase() {
        if (!valid(roadNetwork, CLASS_ID_BSMCHOOSEPHASE)) {
            return;
        }
        if (me.getLink() == null) {
            return;
        }
        int frontNodeID = me.getLink().getToNodeLocalID();
//        ArrayList<Object> choosePhaseList = roadNetwork.get(SOURCE_COIL).get(CLASS_ID_BSMCHOOSEPHASE);
        ArrayList<Object> choosePhaseList = allInstances(CLASS_ID_BSMCHOOSEPHASE);
        for (Object o : choosePhaseList) {
            BSMChoosePhase choosePhase = (BSMChoosePhase) o;
            byte centerID = choosePhase.getCenterNodeLocalID();
            if (frontNodeID == centerID) {
                me.setChoosePhase(choosePhase);
            }
        }
    }

    /**
     * 根据me所在link的toNodeLocalID更新Movements
     */
    private void updateMyMovements() {
        if (!valid(roadNetwork, CLASS_ID_BSMMOVEMENT)) {
            return;
        }
        if (me.getLink() == null) {
            return;
        }
        int frontNodeID = me.getLink().getToNodeLocalID();

        ArrayList<BSMMovement> movements = new ArrayList<>();
//        ArrayList<Object> movementList = roadNetwork.get(SOURCE_COIL).get(CLASS_ID_BSMMOVEMENT);
        ArrayList<Object> movementList = allInstances(CLASS_ID_BSMMOVEMENT);
        for (Object o : movementList) {
            BSMMovement movement = (BSMMovement) o;
            byte centerID = movement.getCenterNodeLocalID();
            if (frontNodeID == centerID) {
                movements.add(movement);
            }
        }

        Log.i(TAG, "updateMyMovements: " + movements.size());
        me.setMovements(movements);
    }

    /**
     * 根据me所在link的fromNodeLocalID和toNodeLocalID更新lane
     */
    private void updateMyLane() {
        if (!valid(roadNetwork, CLASS_ID_BSMLANE)) {
            return;
        }
        if (me.getLink() == null) {
            return;
        }
        int toNodeID = me.getLink().getToNodeLocalID();
        int fromNodeID = me.getLink().getFromNodeLocalID();
        final int SCALE = 10000;    // km换算成cm

        ArrayList<BSMLane> lanes = new ArrayList<>();
//        ArrayList<Object> laneList = roadNetwork.get(SOURCE_COIL).get(CLASS_ID_BSMLANE);
        ArrayList<Object> laneList = allInstances(CLASS_ID_BSMLANE);
        for (Object o : laneList) {
            BSMLane lane = (BSMLane) o;
            if (fromNodeID == lane.getFromNodeLocalID() && toNodeID == lane.getToNodeLocalID()) {
                lanes.add(lane);
            }
        }


        if (lanes.size() != 0) {
            BSMLink myLink = me.getLink();
            int lineNum = myLink.getLaneNum();
            if (lineNum < 1) {
                return;
            }
            BSMLane nearestLane;
            nearestLane = lanes.get(0);

            GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
            GPSSite fromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
            GPSSite toSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
            double myDistance = GPSSite.getDistance(mySite, fromSite, toSite) * SCALE;    // 换算成cm
            BigDecimal meDis = BigDecimal.valueOf(myDistance);
            BigDecimal nearest = BigDecimal.valueOf(myDistance);

            if (lineNum > 1) {
                if (myLink.getPassPointNum() == 0) {
                    for (BSMLane lane : lanes) {
                        double distance = lane.getIndex() * lane.getWidth() * 100 / 2;    // 换算成cm
                        BigDecimal dis = BigDecimal.valueOf(distance).subtract(meDis).abs();
                        if (dis.compareTo(nearest) <= 0) {
                            nearest = dis;
                            nearestLane = lane;
                        }
                    }
                } else {    // 先找到所在的两个pass point连成的线段
                    PassPoint[] pps = myLink.getPassPoint();
                    int num = pps.length;
                    GPSSite site0 = new GPSSite(pps[0].getLatitude(), pps[0].getLongitude(), pps[0].getNSEW());
                    GPSSite site1 = new GPSSite(pps[num - 1].getLatitude(), pps[num - 1].getLongitude(), pps[num - 1].getNSEW());

                    double distance = GPSSite.getDistance(mySite, fromSite, site0);
                    BigDecimal b1 = BigDecimal.valueOf(distance * SCALE);
                    distance = GPSSite.getDistance(mySite, site1, toSite);
                    BigDecimal b2 = BigDecimal.valueOf(distance * SCALE);

                    BigDecimal nearPP = b2;
                    GPSSite pFromSite = site1;  // 保存所在两个pass point的经纬度
                    GPSSite pToSite = toSite;
                    if (b1.compareTo(nearPP) < 0) {
                        nearPP = b1;
                        pFromSite = fromSite;
                        pToSite = site0;
                    }
                    for (int i = 1; i < num - 1; i++) {
                        site0 = new GPSSite(pps[i - 1].getLatitude(), pps[i - 1].getLongitude(), pps[i - 1].getNSEW());
                        site1 = new GPSSite(pps[i].getLatitude(), pps[i].getLongitude(), pps[i].getNSEW());
                        distance = GPSSite.getDistance(mySite, site0, site1);
                        b1 = BigDecimal.valueOf(distance * SCALE);
                        if (b1.compareTo(nearPP) < 0) {
                            nearPP = b1;
                            pFromSite = site0;
                            pToSite = site1;
                        }
                    }

                    myDistance = GPSSite.getDistance(mySite, pFromSite, pToSite);
                    meDis = BigDecimal.valueOf(myDistance * SCALE);
                    nearest = meDis;
                    for (BSMLane lane : lanes) {
                        double diss = lane.getIndex() * lane.getWidth() * 100 / 2;    // 换算成cm
                        BigDecimal bd = BigDecimal.valueOf(diss).subtract(meDis).abs();
                        if (bd.compareTo(nearest) < 0) {
                            nearest = bd;
                            nearestLane = lane;
                        }
                    }
                }
            }
            me.setLane(nearestLane);
        }
    }

    /**
     * 判断rn中的value是否包含key为classID的项。
     *
     * @param rn      输入的集合
     * @param classID 检索的key
     * @return true 包含相应的key；false 不包含相应的key。
     */
    private boolean valid(HashMap<String, HashMap<String, ArrayList<Object>>> rn, String classID) {
        if (rn.size() == 0) {
            return false;
        }
        for (Map.Entry<String, HashMap<String, ArrayList<Object>>> item : rn.entrySet()) {
            if (item.getValue() == null) {
                continue;
            }
            if (item.getValue().containsKey(classID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据点到直线的距离，找到距离最短的更新me所在的link。
     * 不仅要考虑fromNode和toNode，还要考虑中间点passPoint。
     * 另外，同时更新me的frontNode和backNode字段。
     */
    private void updateMyLink() {
        if (!valid(roadNetwork, CLASS_ID_BSMLINK)) {
            return;
        }
        if (!valid(roadNetwork, CLASS_ID_BSMNODE)) {
            return;
        }

//        ArrayList<Object> linkList = roadNetwork.get(SOURCE_COIL).get(CLASS_ID_BSMLINK);
//        ArrayList<Object> nodeList = roadNetwork.get(SOURCE_COIL).get(CLASS_ID_BSMNODE);
        ArrayList<Object> linkList = allInstances(CLASS_ID_BSMLINK);
        ArrayList<Object> nodeList = allInstances(CLASS_ID_BSMNODE);
        GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
        BSMLink nearestLink = null;
//                new BSMLink();    // 距离最近的link
        double nearestDistance = 100.0; // 单位m
        BSMNode frontNode = new BSMNode();
        BSMNode backNode = new BSMNode();
        for (Object o : linkList) {
            BSMLink link = (BSMLink) o;
            byte fromNodeID = link.getFromNodeLocalID();
            byte toNodeID = link.getToNodeLocalID();
            GPSSite fromSite = new GPSSite();
            GPSSite toSite = new GPSSite();
            int flag = 2;   // 标记是否找到from和to两个Node
            for (Object o1 : nodeList) {
                BSMNode node = (BSMNode) o1;
                if (fromNodeID == node.getLocalID()) {
                    fromSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
                    backNode = node;
                    flag--;
                }
                if (toNodeID == node.getLocalID()) {
                    toSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
                    frontNode = node;
                    flag--;
                }
                if (flag == 0) {
                    break;
                }
            }
            if (flag == 0) {
                if (link.getPassPointNum() == 0) {
                    int lr = GPSSite.getLeftOrRight(fromSite, mySite, fromSite, toSite);
                    if (lr <= 0) {  // link方向筛选 // TODO: 5/10/010
                        continue;
                    }
                    double distance = GPSSite.getDistance(mySite, fromSite, toSite);
                    BigDecimal b1 = BigDecimal.valueOf(distance);
                    BigDecimal b2 = BigDecimal.valueOf(nearestDistance);
                    if (b1.compareTo(b2) <= 0) { // 距离筛选
                        nearestDistance = distance;
                        nearestLink = link;
                    }
                } else {
                    PassPoint[] passPoints = link.getPassPoint();
                    int num = passPoints.length;
                    GPSSite site0 = new GPSSite(passPoints[0].getLatitude(), passPoints[0].getLongitude(), passPoints[0].getNSEW());
                    GPSSite site1 = new GPSSite(passPoints[num - 1].getLatitude(), passPoints[num - 1].getLongitude(), passPoints[num - 1].getNSEW());
                    double distance = GPSSite.getDistance(mySite, fromSite, site0);
                    BigDecimal b1 = BigDecimal.valueOf(distance);
                    BigDecimal b2 = BigDecimal.valueOf(nearestDistance);
                    int lr = GPSSite.getLeftOrRight(fromSite, mySite, fromSite, site0);
                    if (lr > 0) {  // link方向筛选
//                        continue;
                        if (b1.compareTo(b2) <= 0) {
                            nearestDistance = distance;
                            nearestLink = link;
                        }
                    }

                    distance = GPSSite.getDistance(mySite, site1, toSite);
                    b1 = BigDecimal.valueOf(distance);
                    b2 = BigDecimal.valueOf(nearestDistance);
                    lr = GPSSite.getLeftOrRight(site1, mySite, site1, toSite);
                    if (lr > 0) {  // link方向筛选
//                        continue;
                        if (b1.compareTo(b2) <= 0) {
                            nearestDistance = distance;
                            nearestLink = link;
                        }
                    }
//                    if (b1.compareTo(b2) <= 0) {
//                        nearestDistance = distance;
//                        nearestLink = link;
//                    }
                    for (int i = 1; i < num; i++) {
                        site0 = new GPSSite(passPoints[i - 1].getLatitude(), passPoints[i - 1].getLongitude(), passPoints[i - 1].getNSEW());
                        site1 = new GPSSite(passPoints[i].getLatitude(), passPoints[i].getLongitude(), passPoints[i].getNSEW());
                        distance = GPSSite.getDistance(mySite, site0, site1);
                        b1 = BigDecimal.valueOf(distance);
                        b2 = BigDecimal.valueOf(nearestDistance);
                        lr = GPSSite.getLeftOrRight(site0, mySite, site0, site1);
                        if (lr >= 0) {
                            continue;
                        }
                        if (b1.compareTo(b2) <= 0) { // 距离筛选
                            nearestDistance = distance;
                            nearestLink = link;
                        }
                    }
                }
            }
        }

        if (nearestLink != null) {
            me.setLink(nearestLink);
            me.setFrontNode(frontNode);
            me.setBackNode(backNode);
        }
    }

    /**
     * 2Hz的数据更新
     *
     * @param data
     */
//    private void updateMeRSUAndNodeTrafficLight(HashMap<String, ArrayList<Object>> data) {
//        boolean isMeUpdate = false;
//        for (Map.Entry<String, ArrayList<Object>> item : data.entrySet()) {
//            switch (item.getKey()) {
//                case CLASS_ID_ME:
//                    Log.i(TAG, "updateMeRSUAndNodeTrafficLight: ME");
//                    me.setVehicle((BSMVehicle) item.getValue().get(0));
//                    isMeUpdate = true;
//                    break;
//                case CLASS_ID_BSMNODE:
//                    // 根据globalID更新对应的Node
//                    for (Object it : item.getValue()) {
//                        BSMNode obj1 = (BSMNode) it;
//                        ArrayList<Object> array = roadNetwork.get(CLASS_ID_BSMNODE);
//                        for (int i = 0; i < array.size(); i++) {
//                            BSMNode obj2 = (BSMNode) array.get(i);
//                            if (obj1.getGlobalID() == obj2.getGlobalID()) {
//                                array.set(i, obj1);
//                            }
//                        }
//                    }
//                    break;
//                case CLASS_ID_BSMTRAFFICLIGHT:
//                    // 根据信号灯路口的局部ID更新对应的TrafficLight
//                    for (Object it : item.getValue()) {
//                        BSMTrafficLight obj1 = (BSMTrafficLight) it;
//                        ArrayList<Object> array = roadNetwork.get(CLASS_ID_BSMTRAFFICLIGHT);
//                        for (int i = 0; i < array.size(); i++) {
//                            BSMTrafficLight obj2 = (BSMTrafficLight) array.get(i);
//                            if (obj1.getCenterNodeLocalID() == obj2.getCenterNodeLocalID()) {
//                                array.set(i, obj1);
//                            }
//                        }
//                    }
//                    break;
//                case CLASS_ID_RSUOTHER:
//                    for (Object it : item.getValue()) {
//                        BSMRSU obj1 = (BSMRSU) it;
//                        ArrayList<Object> array = roadNetwork.get(CLASS_ID_RSUOTHER);
//                        for (int i = 0; i < array.size(); i++) {
//                            BSMRSU obj2 = (BSMRSU) array.get(i);
//                            if (obj1.getParticipantID() == obj2.getParticipantID()) {
//                                array.set(i, obj1);
//                            }
//                        }
//                    }
//                    break;
//                case CLASS_ID_RSUSELF:
//                    for (Object it : item.getValue()) {
//                        BSMRSU obj1 = (BSMRSU) it;
//                        ArrayList<Object> array = roadNetwork.get(CLASS_ID_RSUSELF);
//                        for (int i = 0; i < array.size(); i++) {
//                            BSMRSU obj2 = (BSMRSU) array.get(i);
//                            if (obj1.getParticipantID() == obj2.getParticipantID()) {
//                                array.set(i, obj1);
//                            }
//                        }
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        if (isMeUpdate) {
//            updateMe();
//        }
//    }


    /**
     * 尝试匹配订阅的消息。若匹配成功，则发送的消息给Dispatcher。
     */
    private void matchSubscribedMsgs() {
        Log.i(TAG, "matchSubscribedMsgs: begin......");
        if (me.getVehicle() == null) {
            return;
        }
        if (subscribedMsgs == null) {
            return;
        }

        // HashMap<APPID, HashMap<ClassID, Flag>>
        HashMap<String, HashMap<String, Boolean>> toPublishFlags = null;
        initializeFlags(toPublishFlags);    // 根据subscribedMsgs初始化toPublishedFlags

        // HashMap<APPID, HashMap<ClassID, ArrayList<Obj>>>
        HashMap<String, HashMap<String, ArrayList<Object>>> toPublishObjList = new HashMap<>();

        Log.i(TAG, "matchSubscribedMsgs: in.......");

        // HashMap<ClassID, HashMap<APPID, Filter>>
        for (Map.Entry<String, HashMap<String, String>> match : subscribedMsgs.entrySet()) {
            // match subscribed messages
            String classID = match.getKey();
            HashMap<String, String> appList = match.getValue();

            Log.i(TAG, "before allInstances: classID="+classID);
            ArrayList<Object> objList = allInstances(classID);
            if (objList.size() == 0) {
                Log.i(TAG, "matchSubscribedMsgs: objList.size()=0"+"++++++++++++++++++++++++++");
                continue;
            }
            Log.i(TAG, "matchSubscribedMsgs: objList.size()="+objList.size()+"========================");
            // HashMap<APPID, FilterCode>
            for (Map.Entry<String, String> item : appList.entrySet()) {
                String appID = item.getKey();
                String filterCode = item.getValue();

                // 清华的盒子的ChoosePhase和Movement是和路口相关的常量数据
                // 因此FilterCode中该字段不起作用
                String filter = filterCode.substring(4, 6);    // Lane
                objList = filterLane(objList, classID, filter);
                if (objList.size() == 0) {
                    continue;
                }

                filter = filterCode.substring(6, 8);       // Link
                objList = filterLink(objList, classID, filter);
                if (objList.size() == 0) {
                    continue;
                }

                filter = filterCode.substring(12, 16);     // 前后左右
                objList = filterFBLR(objList, classID, filter);
                if (objList.size() == 0) {
                    continue;
                }

                // 此时的objList就是即将发送到Dispatcher的对应于classID对象的集合
                if (!toPublishFlags.get(appID).get(classID)) {
                    toPublishFlags.get(appID).put(classID, true);
                    // 添加该类的对象集合到toPublishObjList
                    if (!toPublishObjList.containsKey(appID)) {
                        toPublishObjList.put(appID, new HashMap<>());
                    }
//                    toPublishObjList.putIfAbsent(appID, new HashMap<>());
                    toPublishObjList.get(appID).put(classID, objList);
                }
                if (checkFlagsAllTrue(appID, toPublishFlags)) {  // 该appID对应的订阅信息全部匹配成功
                    // 5/9/009 send a message
                    Log.i(TAG, "matchSubscribedMsgs: ready to send a message to dispatcher");
                    Message msg = Message.obtain();
                    HashMap<String, HashMap<String, ArrayList<Object>>> tosend = new HashMap<String, HashMap<String, ArrayList<Object>>>() {{
                        put(appID, toPublishObjList.get(appID));
                    }};

                    msg.obj = tosend;
                    Log.i(TAG, "matchSubscribedMsgs: match successs =============================");
                    Log.i(TAG, msg.obj.toString());

                    toDispatcherHander.sendMessage(msg);
                    Log.i(TAG, "matchSubscribedMsgs: send a message to dispatcher done");
                    publishedMsgs.put(appID, toPublishObjList.get(appID));
                }
            }
        }
    }

    /**
     * 根据filter过滤classID对应的objList。
     *
     * @param objList classID对应的对象的集合
     * @param classID 类ID
     * @param filter  FilterCode中Front,Back,Left,Right域对应的filter
     * @return objList 过滤后的对象的集合
     */
    private ArrayList<Object> filterFBLR(ArrayList<Object> objList, String classID, String filter) {
        // TODO: 5/9/009  前后左后
        ArrayList<Object> objects1 = new ArrayList<>();
        ArrayList<Object> objects2 = new ArrayList<>();
        switch (filter) {
            case MATCH_FRONT:   // 前
                objList = filterFront(objList, classID, filter);
                break;
            case MATCH_BACK:    // 后
                objList = filterBack(objList, classID, filter);
                break;
            case MATCH_LEFT:    // 左
                objList = filterLeft(objList, classID, filter);
                break;
            case MATCH_RIGHT:   // 右
                objList = filterRight(objList, classID, filter);
                break;
            case MATCH_FRONT_OR_BACK:   // 前或后
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterFront(objects1, classID, MATCH_FRONT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterBack(objects2, classID, MATCH_BACK);

                if (objects2.size() == 0) {
                    break;
                }
                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_LEFT_AND_FRONT:  // 左与前
                objList = filterLeft(objList, classID, MATCH_LEFT);
                objList = filterFront(objList, classID, MATCH_FRONT);
                break;
            case MATCH_LEFT_AND_BACK:   // 左与后
                objList = filterLeft(objList, classID, MATCH_LEFT);
                objList = filterBack(objList, classID, MATCH_BACK);
                break;
            case MATCH_LEFT_OR_FRONT:   // 左或前
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterLeft(objects1, classID, MATCH_LEFT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterFront(objects2, classID, MATCH_FRONT);

                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_RIGHT_AND_FRONT: // 右与前
                objList = filterRight(objList, classID, MATCH_RIGHT);
                objList = filterFront(objList, classID, MATCH_FRONT);
                break;
            case MATCH_RIGHT_AND_BACK:  // 右与后
                objList = filterRight(objList, classID, MATCH_RIGHT);
                objList = filterBack(objList, classID, MATCH_BACK);
                break;
            case MATCH_RIGHT_OR_BACK:   // 右或后
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterRight(objects1, classID, MATCH_RIGHT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterBack(objects2, classID, MATCH_BACK);

                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_LEFT_OR_RIGHT:   // 左或右
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterLeft(objects1, classID, MATCH_LEFT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterRight(objects2, classID, MATCH_RIGHT);

                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_RIGHT_OR_FRONT:  // 右或前
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterRight(objects1, classID, MATCH_RIGHT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterFront(objects2, classID, MATCH_FRONT);

                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_LEFT_OR_BACK:    // 左或后
                objects1.clear();
                objects1.addAll(objList);
                objects1 = filterLeft(objects1, classID, MATCH_LEFT);

                objects2.clear();
                objects2.addAll(objList);
                objects2 = filterBack(objects2, classID, MATCH_BACK);

                objects2.removeAll(objects1);
                objects2.addAll(objects1);
                objList.clear();
                objList.addAll(objects2);
                break;
            case MATCH_AROUND:          // 全部订阅（左右车道和前后的实例）
                objects1.clear();
                objects1.addAll(objList);
                objects2.clear();
                objects2.addAll(objList);
                ArrayList<Object> objects3 = new ArrayList<>();
                ArrayList<Object> objects4 = new ArrayList<>();
                objects3.addAll(objList);
                objects4.addAll(objList);
                objects1 = filterFront(objects1, classID, MATCH_FRONT);
                objects2 = filterBack(objects2, classID, MATCH_BACK);
                objects3 = filterLeft(objects3, classID, MATCH_LEFT);
                objects4 = filterRight(objects4, classID, MATCH_RIGHT);

                objects4.removeAll(objects1);
                objects4.removeAll(objects2);
                objects4.removeAll(objects3);
                objects4.addAll(objects1);
                objects4.addAll(objects2);
                objects4.addAll(objects3);
                objList.clear();
                objList.addAll(objects4);
                break;
            case MATCH_ANY:     // 不订阅（即不筛选）
            default:
                break;
        }
        return objList;
    }

    /**
     * 匹配右边的对象。
     *
     * @param objList 对象的集合
     * @param classID 对象所属的类
     * @param filter  过滤码
     * @return 匹配后的对象的集合。
     */
    private ArrayList<Object> filterRight(ArrayList<Object> objList, String classID, String filter) {
        if (!filter.equals(MATCH_RIGHT) || objList.size() == 0) {
            return objList;
        }

        GPSSite myFromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
        switch (classID) {
            case CLASS_ID_BSMVEHICLE:
            case CLASS_ID_BSMMOTOR:
            case CLASS_ID_BSMPEDEST:
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    GPSSite site = null;
                    if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                        BSMVehicle vehicle = (BSMVehicle) o;
                        site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                        BSMMotor motor = (BSMMotor) o;
                        site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMPEDEST)) {
                        BSMPedest pedest = (BSMPedest) o;
                        site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                    }
                    GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
                    int lr = GPSSite.getLeftOrRight(myFromSite, site, myFromSite, mySite);
                    if (lr <= 0) {  // 剔除左边和共线的
                        objList.remove(i);
                    }
                }
                break;
            case CLASS_ID_BSMLANE:
                int indexMyLane = me.getLane().getIndex();
                for (int i = 0; i < objList.size(); i++) {
                    BSMLane lane = (BSMLane) objList.get(i);
                    if (lane.getIndex() <= indexMyLane) {
                        objList.remove(i);
                    }
                }
                break;
            case CLASS_ID_BSMLINK:
                GPSSite myToSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
                GPSSite fromNodeSite = null;
                GPSSite toNodeSite = null;
                for (int i = 0; i < objList.size(); i++) {
                    BSMLink link = (BSMLink) objList.get(i);
                    BSMNode fromNode = me.getNodes().get(link.getFromNodeLocalID());
                    BSMNode toNode = me.getNodes().get(link.getToNodeLocalID());

                    fromNodeSite = new GPSSite(fromNode.getLatitude(), fromNode.getLongitude(), fromNode.getNSEW());
                    toNodeSite = new GPSSite(toNode.getLatitude(), toNode.getLongitude(), toNode.getNSEW());
                    int lr = GPSSite.getLeftOrRight(toNodeSite, fromNodeSite, myFromSite, myToSite);
                    if (lr <= 0) {  // 剔除左边和共线的
                        objList.remove(i);
                    }
                }
                break;
            default:
                break;
        }
        return objList;
    }

    /**
     * 匹配左边的对象。
     *
     * @param objList 对象的集合
     * @param classID 对象所属的类
     * @param filter  过滤码
     * @return 匹配后的对象的集合。
     */
    private ArrayList<Object> filterLeft(ArrayList<Object> objList, String classID, String filter) {
        if (!filter.equals(MATCH_LEFT) || objList.size() == 0) {
            return objList;
        }

        GPSSite myFromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
        switch (classID) {
            case CLASS_ID_BSMVEHICLE:
            case CLASS_ID_BSMMOTOR:
            case CLASS_ID_BSMPEDEST:
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    GPSSite site = null;
                    if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                        BSMVehicle vehicle = (BSMVehicle) o;
                        site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                        BSMMotor motor = (BSMMotor) o;
                        site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMPEDEST)) {
                        BSMPedest pedest = (BSMPedest) o;
                        site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                    }
                    GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
                    int lr = GPSSite.getLeftOrRight(myFromSite, site, myFromSite, mySite);
                    if (lr >= 0) {  // 剔除右边和共线的
                        objList.remove(i);
                    }
                }
                break;
            case CLASS_ID_BSMLANE:
                int indexLane = me.getLane().getIndex();
                for (int i = 0; i < objList.size(); i++) {
                    BSMLane lane = (BSMLane) objList.get(i);
                    if (lane.getIndex() >= indexLane) {
                        objList.remove(i);
                    }
                }
                break;
            case CLASS_ID_BSMLINK:
                GPSSite myToSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
                GPSSite fromNodeSite = null;
                GPSSite toNodeSite = null;
                for (int i = 0; i < objList.size(); i++) {
                    BSMLink link = (BSMLink) objList.get(i);
                    BSMNode fromNode = me.getNodes().get(link.getFromNodeLocalID());
                    BSMNode toNode = me.getNodes().get(link.getToNodeLocalID());

                    fromNodeSite = new GPSSite(fromNode.getLatitude(), fromNode.getLongitude(), fromNode.getNSEW());
                    toNodeSite = new GPSSite(toNode.getLatitude(), toNode.getLongitude(), toNode.getNSEW());
                    int lr = GPSSite.getLeftOrRight(toNodeSite, fromNodeSite, myFromSite, myToSite);
                    if (lr >= 0) {  // 剔除右边和共线的
                        objList.remove(i);
                    }
                }
                break;
            default:
                break;
        }
        return objList;
    }

    /**
     * 匹配后方的对象。
     *
     * @param objList 对象集合
     * @param classID 对象所属的类
     * @param filter  过滤码
     * @return 匹配后的对象的集合。
     */
    private ArrayList<Object> filterBack(ArrayList<Object> objList, String classID, String filter) {
        if (!filter.equals(MATCH_BACK) || objList.size() == 0) {
            return objList;
        }
        GPSSite myFromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
        GPSSite myToSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
        GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
        switch (classID) {
            case CLASS_ID_BSMVEHICLE:
            case CLASS_ID_BSMMOTOR:
            case CLASS_ID_BSMPEDEST:
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    GPSSite site = null;
                    if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                        BSMVehicle vehicle = (BSMVehicle) o;
                        site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                        BSMMotor motor = (BSMMotor) o;
                        site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMPEDEST)) {
                        BSMPedest pedest = (BSMPedest) o;
                        site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMSIGN)) {
                        BSMSign sign = (BSMSign) o;
                        site = new GPSSite(sign.getPosLat(), sign.getPosLon(), sign.getNSEW());
                    }
                    double distance = GPSSite.getDistance(site, myFromSite, myToSite);
                    BigDecimal b1 = BigDecimal.valueOf(distance * 10000);
                    BigDecimal b2 = BigDecimal.valueOf(EPSILON_DISTANCE * 10);
                    if (b1.compareTo(b2) >= 0) {
                        objList.remove(i);
                        continue;   // 剔除不在同一个link、lane上的
                    }
                    double radians = GPSSite.getRadians(myFromSite, mySite, myFromSite, site);
                    b1 = BigDecimal.valueOf((180 - Math.toDegrees(radians)) * 100); // 后方
                    b2 = BigDecimal.valueOf(EPSILON_ANGLE * 100);
                    if (b1.compareTo(b2) > 0) {
                        objList.remove(i);  //  剔除不在后面的
                    }
                }
                break;
            case CLASS_ID_BSMLANE:
            case CLASS_ID_BSMLINK:  // nodeID==me.frontNodeID
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    int fromNodeID = 0;
                    int toNodeID = 0;
                    if (classID.equals(CLASS_ID_BSMLANE)) {
                        BSMLane lane = (BSMLane) o;
                        fromNodeID = lane.getFromNodeLocalID();
                        toNodeID = lane.getToNodeLocalID();
                    } else if (classID.equals(CLASS_ID_BSMLINK)) {
                        BSMLink link = (BSMLink) o;
                        fromNodeID = link.getFromNodeLocalID();
                        toNodeID = link.getToNodeLocalID();
                    }
                    if (!((toNodeID == me.getBackNode().getLocalID() && fromNodeID != me.getFrontNode().getLocalID())
                            || (fromNodeID == me.getBackNode().getLocalID() && toNodeID != me.getFrontNode().getLocalID()))) {
                        objList.remove(i);  // 剔除不相交的，和与me相同的或者反向的
                    }
                }

                for (int i = 0; i < objList.size(); i++) { // 剔除和me所在link、lane夹角大于EPSILON_ANGLE的
                    Object o = objList.get(i);
                    GPSSite fromNodeSite = null;
                    GPSSite toNodeSite = null;
                    byte fromID = -1;
                    byte toID = -1;
                    if (classID.equals(CLASS_ID_BSMLANE)) {
                        BSMLane lane = (BSMLane) o;
                        fromID = lane.getFromNodeLocalID();
                        toID = lane.getToNodeLocalID();
                    } else if (classID.equals(CLASS_ID_BSMLINK)) {
                        BSMLink link = (BSMLink) o;
                        fromID = link.getFromNodeLocalID();
                        toID = link.getToNodeLocalID();
                    }

//                    ArrayList<Object> nodes = allInstances(CLASS_ID_BSMNODE);
//                    for (Object no : nodes) {
//                        BSMNode node = (BSMNode) no;
//                        if (fromID == node.getLocalID()) {
//                            fromNodeSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
//                        }
//                        if (toID == node.getLocalID()) {
//                            toNodeSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
//                        }
//                    }
                    BSMNode fromNode = me.getNodes().get(fromID);
                    BSMNode toNode = me.getNodes().get(toID);
                    fromNodeSite = new GPSSite(fromNode.getLatitude(), fromNode.getLongitude(), fromNode.getNSEW());
                    toNodeSite = new GPSSite(toNode.getLatitude(), toNode.getLongitude(), toNode.getNSEW());

                    double radians = GPSSite.getRadians(fromNodeSite, toNodeSite, myFromSite, myToSite);
                    BigDecimal b1 = BigDecimal.valueOf((180 - Math.toDegrees(radians)) * 100);
                    BigDecimal b2 = BigDecimal.valueOf(EPSILON_ANGLE * 100);
                    if (b1.compareTo(b2) > 0) {    // 剔除和me所在link、lane夹角小于180-EPSILON_ANGLE的
                        objList.remove(i);
                    }
                }
                break;
            default:
                break;
        }
        return objList;
    }

    /**
     * 匹配前方的对象。
     *
     * @param objList 对象集合
     * @param classID 对象所属的类
     * @param filter  过滤码
     * @return 匹配后的对象集合。
     */
    private ArrayList<Object> filterFront(ArrayList<Object> objList, String classID, String filter) {
        if (!filter.equals(MATCH_FRONT) || objList.size() == 0) {
            return objList;
        }
        if (me.getBackNode()==null||me.getFrontNode()==null){
            return objList;
        }
        GPSSite myFromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
        GPSSite myToSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
        GPSSite mySite = new GPSSite(me.getVehicle().getLatitude(), me.getVehicle().getLongitude(), me.getVehicle().getNSEW());
        switch (classID) {
            case CLASS_ID_BSMVEHICLE:
            case CLASS_ID_BSMMOTOR:
            case CLASS_ID_BSMPEDEST:
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    GPSSite site = null;
                    if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                        BSMVehicle vehicle = (BSMVehicle) o;
                        site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                        BSMMotor motor = (BSMMotor) o;
                        site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMPEDEST)) {
                        BSMPedest pedest = (BSMPedest) o;
                        site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                    } else if (classID.equals(CLASS_ID_BSMSIGN)) {
                        BSMSign sign = (BSMSign) o;
                        site = new GPSSite(sign.getPosLat(), sign.getPosLon(), sign.getNSEW());
                    }
                    double distance = GPSSite.getDistance(site, myFromSite, myToSite);
                    BigDecimal b1 = BigDecimal.valueOf(distance * 10000);
                    BigDecimal b2 = BigDecimal.valueOf(EPSILON_DISTANCE * 10);
                    if (b1.compareTo(b2) >= 0) {
                        objList.remove(i);
                        continue;   // 剔除不在同一个link、lane上的
                    }
                    double radians = GPSSite.getRadians(myFromSite, mySite, myFromSite, site);
                    b1 = BigDecimal.valueOf(Math.toDegrees(radians) * 100);
                    b2 = BigDecimal.valueOf(EPSILON_ANGLE * 100);
                    if (b1.compareTo(b2) > 0) {
                        objList.remove(i);  //  剔除不在同一个方向上的
                    }
                }
                break;
            case CLASS_ID_BSMLANE:
            case CLASS_ID_BSMLINK:  // nodeID==me.frontNodeID
                for (int i = 0; i < objList.size(); i++) {
                    Object o = objList.get(i);
                    int fromNodeID = 0;
                    int toNodeID = 0;
                    if (classID.equals(CLASS_ID_BSMLANE)) {
                        BSMLane lane = (BSMLane) o;
                        fromNodeID = lane.getFromNodeLocalID();
                        toNodeID = lane.getToNodeLocalID();
                    } else if (classID.equals(CLASS_ID_BSMLINK)) {
                        BSMLink link = (BSMLink) o;
                        fromNodeID = link.getFromNodeLocalID();
                        toNodeID = link.getToNodeLocalID();
                    }
                    if (!((toNodeID == me.getFrontNode().getLocalID() && fromNodeID != me.getBackNode().getLocalID())
                            || (fromNodeID == me.getFrontNode().getLocalID() && toNodeID != me.getBackNode().getLocalID()))) {
                        objList.remove(i);  // 剔除不相交的，和与me相同的或者反向的
                    }
                }

                for (int i = 0; i < objList.size(); i++) { // 剔除和me所在link、lane夹角大于EPSILON_ANGLE的
                    Object o = objList.get(i);
                    GPSSite fromNodeSite = null;
                    GPSSite toNodeSite = null;
                    byte fromID = -1;
                    byte toID = -1;
                    if (classID.equals(CLASS_ID_BSMLANE)) {
                        BSMLane lane = (BSMLane) o;
                        fromID = lane.getFromNodeLocalID();
                        toID = lane.getToNodeLocalID();
                    } else if (classID.equals(CLASS_ID_BSMLINK)) {
                        BSMLink link = (BSMLink) o;
                        fromID = link.getFromNodeLocalID();
                        toID = link.getToNodeLocalID();
                    }

//                    ArrayList<Object> nodes = allInstances(CLASS_ID_BSMNODE);
//                    for (Object no : nodes) {
//                        BSMNode node = (BSMNode) no;
//                        if (fromID == node.getLocalID()) {
//                            fromNodeSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
//                        }
//                        if (toID == node.getLocalID()) {
//                            toNodeSite = new GPSSite(node.getLatitude(), node.getLongitude(), node.getNSEW());
//                        }
//                    }
                    BSMNode fromNode = me.getNodes().get(fromID);
                    BSMNode toNode = me.getNodes().get(toID);
                    fromNodeSite = new GPSSite(fromNode.getLatitude(), fromNode.getLongitude(), fromNode.getNSEW());
                    toNodeSite = new GPSSite(toNode.getLatitude(), toNode.getLongitude(), toNode.getNSEW());

                    double radians = GPSSite.getRadians(fromNodeSite, toNodeSite, myFromSite, myToSite);
                    BigDecimal b1 = BigDecimal.valueOf(Math.toDegrees(radians) * 100);
                    BigDecimal b2 = BigDecimal.valueOf(EPSILON_ANGLE * 100);
                    if (b1.compareTo(b2) > 0) {    // 剔除和me所在link、lane夹角大于EPSILON_ANGLE的
                        objList.remove(i);
                    }
                }
                break;
            default:
                break;
        }
        return objList;
    }

    /**
     * 根据filter过滤classID对应的objList。
     *
     * @param objList classID对应的对象的集合
     * @param classID 类ID
     * @param filter  FilterCode中Link域对应的filter
     * @return objList 过滤后的对象的集合
     */
    private ArrayList<Object> filterLink(ArrayList<Object> objList, String classID, String filter) {
        switch (filter) {
            case MATCH_ONLY_WITH_ME:  // 只订阅与me相同的实例
            case MATCH_ONLY_NOT_WITH_ME:  // 只订阅与me不同的实例
                boolean flag = false;   // MATCH_ONLY_WITH_ME:true  MATCH_ONLY_NOT_WITH_ME:false
                if (MATCH_ONLY_WITH_ME.equals(filter)) {
                    flag = true;
                }
                switch (classID) {
                    case CLASS_ID_BSMPEDEST:
                    case CLASS_ID_BSMMOTOR:
                    case CLASS_ID_BSMVEHICLE:
                        // 5/8/008 根据经纬度，点到直线的距离判断
                        GPSSite fromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
                        GPSSite toSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
                        GPSSite site = null;
                        for (int i = 0; i < objList.size(); i++) {
                            Object o = objList.get(i);
                            if (classID.equals(CLASS_ID_BSMPEDEST)) {
                                BSMPedest pedest = (BSMPedest) o;
                                site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                            } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                                BSMMotor motor = (BSMMotor) o;
                                site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                            } else if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                                BSMVehicle vehicle = (BSMVehicle) o;
                                site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                            }
                            double distance = GPSSite.getDistance(site, fromSite, toSite);
                            // 换算成cm进行比较
                            BigDecimal b1 = BigDecimal.valueOf(distance * 10000);
                            BigDecimal b2 = BigDecimal.valueOf(EPSILON_DISTANCE * 10);
                            if (flag) { // 只要相同的
                                if (b1.compareTo(b2) > 0) {
                                    objList.remove(i);
                                }
                            } else {    // 只要不同的
                                if (b1.compareTo(b2) <= 0) {
                                    objList.remove(i);
                                }
                            }
                        }
                        break;
                    case CLASS_ID_BSMLANE:  // TODO: 5/9/009 目前清华的盒子区分不了同一个link上的不同的lane
                    case CLASS_ID_BSMLINK:
                        // 5/8/008 根据fromNode和toNode判断
                        int fromNode = me.getBackNode().getLocalID();
                        int toNode = me.getFrontNode().getLocalID();
                        for (int i = 0; i < objList.size(); i++) {
                            Object o = objList.get(i);
                            int oFromNode = -1;
                            int oToNode = -1;
                            if (classID.equals(CLASS_ID_BSMLANE)) {
                                BSMLane node = (BSMLane) o;
                                oFromNode = node.getFromNodeLocalID();
                                oToNode = node.getToNodeLocalID();
                            } else if (classID.equals(CLASS_ID_BSMLINK)) {
                                BSMLink link = (BSMLink) o;
                                oFromNode = link.getFromNodeLocalID();
                                oToNode = link.getToNodeLocalID();
                            }
                            if (flag) {   // 只要相同的
                                if (fromNode != oFromNode || toNode != oToNode) {
                                    objList.remove(i);
                                }
                            } else {     // 只要不同的
                                if (fromNode == oFromNode && toNode == oToNode) {
                                    objList.remove(i);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                break;
            case MATCH_ANY_WITH_ME:  // 不订阅
            case MATCH_NOT_DEFINED_WITH_ME:  // not defined
            default:
                break;
        }
        return objList;
    }

    /**
     * 根据filter过滤classID对应的objList。
     *
     * @param objList classID对应的对象的集合
     * @param classID 类ID
     * @param filter  FilterCode中Lane域对应的filter
     * @return objList 过滤后的对象的集合
     */
    private ArrayList<Object> filterLane(ArrayList<Object> objList, String classID, String filter) {
        switch (filter) {
            case MATCH_ONLY_WITH_ME:  // 只订阅与me相同的实例
            case MATCH_ONLY_NOT_WITH_ME:  // 只订阅与me不同的实例
                boolean flag = false;   // MATCH_ONLY_WITH_ME:true  MATCH_ONLY_NOT_WITH_ME:false
                if (MATCH_ONLY_WITH_ME.equals(filter)) {
                    flag = true;
                }
                switch (classID) {
                    case CLASS_ID_BSMPEDEST:
                    case CLASS_ID_BSMMOTOR:
                    case CLASS_ID_BSMVEHICLE:
                        // 5/8/008 根据经纬度，点到直线的距离判断
                        // TODO: 5/9/009 目前根据清华盒子不能区分同一link上的不同lane
                        GPSSite fromSite = new GPSSite(me.getBackNode().getLatitude(), me.getBackNode().getLongitude(), me.getBackNode().getNSEW());
                        GPSSite toSite = new GPSSite(me.getFrontNode().getLatitude(), me.getFrontNode().getLongitude(), me.getFrontNode().getNSEW());
                        GPSSite site = null;
                        for (int i = 0; i < objList.size(); i++) {
                            Object o = objList.get(i);
                            if (classID.equals(CLASS_ID_BSMPEDEST)) {
                                BSMPedest pedest = (BSMPedest) o;
                                site = new GPSSite(pedest.getLatitude(), pedest.getLongitude(), pedest.getNSEW());
                            } else if (classID.equals(CLASS_ID_BSMMOTOR)) {
                                BSMMotor motor = (BSMMotor) o;
                                site = new GPSSite(motor.getLatitude(), motor.getLongitude(), motor.getNSEW());
                            } else if (classID.equals(CLASS_ID_BSMVEHICLE)) {
                                BSMVehicle vehicle = (BSMVehicle) o;
                                site = new GPSSite(vehicle.getLatitude(), vehicle.getLongitude(), vehicle.getNSEW());
                            }
                            double distance = GPSSite.getDistance(site, fromSite, toSite);
                            // 换算成cm进行比较
                            BigDecimal b1 = BigDecimal.valueOf(distance * 10000);
                            BigDecimal b2 = BigDecimal.valueOf(EPSILON_DISTANCE * 10);
                            if (flag) { // 只要相同的
                                if (b1.compareTo(b2) > 0) {
                                    objList.remove(i);
                                }
                            } else {    // 只要不同的
                                if (b1.compareTo(b2) <= 0) {
                                    objList.remove(i);
                                }
                            }
                        }
                        break;
                    case CLASS_ID_BSMLANE:  // TODO: 5/9/009 目前清华的盒子区分不了同一个link上的不同的lane
                    case CLASS_ID_BSMLINK:
                        // 5/8/008 根据fromNode和toNode判断
                        int fromNode = me.getBackNode().getLocalID();
                        int toNode = me.getFrontNode().getLocalID();
                        for (int i = 0; i < objList.size(); i++) {
                            Object o = objList.get(i);
                            int oFromNode = -1;
                            int oToNode = -1;
                            if (classID.equals(CLASS_ID_BSMLANE)) {
                                BSMLane node = (BSMLane) o;
                                oFromNode = node.getFromNodeLocalID();
                                oToNode = node.getToNodeLocalID();
                            } else if (classID.equals(CLASS_ID_BSMLINK)) {
                                BSMLink link = (BSMLink) o;
                                oFromNode = link.getFromNodeLocalID();
                                oToNode = link.getToNodeLocalID();
                            }
                            if (flag) {   // 只要相同的
                                if (fromNode != oFromNode || toNode != oToNode) {
                                    objList.remove(i);
                                }
                            } else {     // 只要不同的
                                if (fromNode == oFromNode && toNode == oToNode) {
                                    objList.remove(i);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                break;
            case MATCH_ANY_WITH_ME:  // 不订阅
            case MATCH_NOT_DEFINED_WITH_ME:  // not defined
            default:
                break;
        }
        return objList;
    }

    /**
     * 取出容器中所有classID对应类的实例的集合。
     *
     * @param classID 类的ID
     * @return classID对应的类的实例的集合
     */
    private ArrayList<Object> allInstances(String classID) {
        ArrayList<Object> objList = new ArrayList<>();
        // 根据CLASS_ID进行处理
        switch (classID) {
            // 交通参与者
            case CLASS_ID_BSMVEHICLE:
            case CLASS_ID_RSUSELF:
            case CLASS_ID_RSUOTHER:
            case CLASS_ID_BSMPEDEST:
                for (Map.Entry<String, HashMap<String, ArrayList<Object>>> sourceItem : participants.entrySet()) {
                    for (Map.Entry<String, ArrayList<Object>> classItem : sourceItem.getValue().entrySet()) {
                        if (classID.equals(classItem.getKey())) {
                            objList.addAll(classItem.getValue());
                        }
                    }
                }
//                for (Map.Entry<String, HashMap<String, ArrayList<Object>>> sourceItem : participants.entrySet()) {
//                    sourceItem.getValue().entrySet().stream().filter(classItem ->
//                            classItem.getKey().equals(classID)).forEach(classItem ->
//                            objList.addAll(classItem.getValue()));
//                }
                break;
            // 逻辑路网
            case CLASS_ID_BSMMOTOR:
            case CLASS_ID_BSMSIGN:
            case CLASS_ID_BSMNODE:
            case CLASS_ID_BSMTRAFFICLIGHT:
            case CLASS_ID_BSMLINK:
            case CLASS_ID_BSMLANE:
            case CLASS_ID_BSMMOVEMENT:
            case CLASS_ID_BSMCHOOSEPHASE:
                for (Map.Entry<String, HashMap<String, ArrayList<Object>>> sourceItem : roadNetwork.entrySet()) {
                    for (Map.Entry<String, ArrayList<Object>> classItem : sourceItem.getValue().entrySet()) {
                        if (classID.equals(classItem.getKey())) {
                            objList.addAll(classItem.getValue());
                        }
                    }
                }
//                for (Map.Entry<String, HashMap<String, ArrayList<Object>>> sourceItem : roadNetwork.entrySet()) {
//                    sourceItem.getValue().entrySet().stream().filter(classItem ->
//                            classItem.getKey().equals(classID)).forEach(classItem ->
//                            objList.addAll(classItem.getValue()));
//                }
                break;
            default:
                break;
        }
        return objList;
    }

    /**
     * 根据subscribedMsgs初始化flags。
     *
     * @param flags 需要初始化的集合，格式为HashMap<APPID, HashMap<ClassID, Flag>>。
     */
    private void initializeFlags(HashMap<String, HashMap<String, Boolean>> flags) {
        if (flags == null) {
            flags = new HashMap<>();
        }

        // HashMap<ClassID, HashMap<APPID, FilterCode>>
        for (Map.Entry<String, HashMap<String, String>> item : subscribedMsgs.entrySet()) {
            for (Map.Entry<String, String> it : item.getValue().entrySet()) {
                if (!flags.containsKey(it.getKey())) {
                    flags.put(it.getKey(), new HashMap<>());
                }
//                flags.putIfAbsent(it.getKey(), new HashMap<>());
                flags.get(it.getKey()).put(item.getKey(), false);
            }
        }
    }

    /**
     * 检查对应于appID和classID的flags的值是否全部为true。
     *
     * @param appID 应用的ID
     * @param flags 用于标记的容器
     * @return true if 全部为true; else false.
     */
    private boolean checkFlagsAllTrue(String appID, HashMap<String, HashMap<String, Boolean>> flags) {
        if (flags.size() == 0) {
            return false;
        }
        if (!flags.containsKey(appID)) {
            return false;
        }

        for (Map.Entry<String, Boolean> it : flags.get(appID).entrySet()) {
            if (!it.getValue()) {
                return false;
            }
        }
        return true;
    }

}

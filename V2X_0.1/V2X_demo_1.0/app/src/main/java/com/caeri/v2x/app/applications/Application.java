package com.caeri.v2x.app.applications;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by JeRome on 2017/4/28.
 */

public abstract class Application {
    private String appId;
    //    发布的消息，String代表订阅类ID，Object代表类实例
    public abstract ArrayList<HashMap<String,String>> trigger();
    public abstract void setAllParameter(HashMap<String, ArrayList<Object>> pubMap, HashMap<String, ArrayList<Object>> meMap);
}

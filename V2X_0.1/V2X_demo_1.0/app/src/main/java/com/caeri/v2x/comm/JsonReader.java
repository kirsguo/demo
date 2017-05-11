package com.caeri.v2x.comm;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import java.io.InputStream;
/**
 * Created by wind on 2017/4/19.
 */

public class JsonReader {


    public static void readFromAssets(Context context,String confName,StringBuffer targetip,
                                      StringBuffer targetport,StringBuffer myport,StringBuffer appname,StringBuffer boxtype) {
        try {
            InputStream is = context.getAssets().open(confName);//此处为要加载的json文件名称
            String text = readTextFromSDcard(is);
            handleCitiesResponse(text,targetip,targetport,myport,appname,boxtype);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.d("readFromAssets",e.toString());
        }
    }
    //将传入的is一行一行解析读取出来出来
    private static String readTextFromSDcard(InputStream is) throws Exception {
        InputStreamReader reader = new InputStreamReader(is,"GB2312");
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            buffer.append(str);
            buffer.append("\n");
        }
        return buffer.toString();//把读取的数据返回
    }
    //把读取出来的json数据进行解析
    private static boolean handleCitiesResponse(String response,StringBuffer targetip,
                                                StringBuffer targetport,StringBuffer myport,StringBuffer appname,StringBuffer boxtype) {
        try {

            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                targetip.delete(0,targetip.length());
                targetip.append(jsonObject.getString("targetip"));

                //Log.i("a", targetip);
                targetport.delete(0,targetport.length());
                targetport.append(jsonObject.getString("targetport"));

                myport.delete(0,myport.length());
                myport.append(jsonObject.getString("myport"));
                //myport=jsonObject.getString("myport");
                appname.delete(0,appname.length());
                appname.append(jsonObject.getString("appname"));
                //appname=jsonObject.getString("appname");
                boxtype.delete(0,boxtype.length());
                boxtype.append(jsonObject.getString("boxtype"));
                //boxtype=jsonObject.getString("boxtype");

//                Log.i("ip", jsonObject.getString("ip"));//""内填写你要读取的数据
//                Log.i("port", jsonObject.getString("port"));//""内填写你要读取的数据
//                Log.i("boxID", jsonObject.getString("boxID"));//""内填写你要读取的数据
            }

            return true;

        } catch (Exception e) {
            Log.d("handleCitiesResponse", e.toString());
        }
        return false;
    }




}

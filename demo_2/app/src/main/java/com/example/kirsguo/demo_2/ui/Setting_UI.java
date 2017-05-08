package com.example.kirsguo.demo_2.ui;

import android.content.Context;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.kirsguo.demo_2.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import Bean.ConfigurationBean;

import static com.example.kirsguo.demo_2.util.Constants.APP_ID_APPIntersectionCollisionWarning;
import static com.example.kirsguo.demo_2.util.Constants.APP_ID_APPTrafficLightOptimalSpeedAdvisory;


public class Setting_UI extends AppCompatActivity {
    private static final String TAG = "Setting_UI";
    private CheckBox setting_UI_icw;
    private CheckBox setting_UI_tlosa;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting__ui);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar !=null){
            actionBar.hide();
        }
//        设置标题栏文字
        TextView title = (TextView) findViewById(R.id.title_text);
        title.setText("应用");
        setting_UI_icw = (CheckBox)findViewById(R.id.setting_UI_icw);
        setting_UI_tlosa = (CheckBox) findViewById(R.id.setting_UI_tlosa);
        InputStream inputStream = null;
        try {
//            inputStream = getApplicationContext().getAssets().open("configuration.json");
            inputStream = openFileInput("configuration.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ConfigurationBean configurationBean = new GsonBuilder().create().fromJson(new InputStreamReader(inputStream),ConfigurationBean.class);
        for (ConfigurationBean.APPBean bean:configurationBean.getAPP() ){
            switch (bean.getID()){
                case APP_ID_APPIntersectionCollisionWarning:
                    if (bean.getEnabled() == 1){
                        setting_UI_icw.setChecked(true);
                    }else {
                        setting_UI_icw.setChecked(false);
                    }
                    break;
                case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                    if (bean.getEnabled() == 1){
                        setting_UI_tlosa.setChecked(true);
                    }else {
                        setting_UI_tlosa.setChecked(false);
                    }
                    break;

            }
        }

        setting_UI_icw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int enabled = 0;
                if (isChecked){
//                    选中
                    enabled = 1;
                    Log.i(TAG, "onCheckedChanged: true" );
                }else {
                    enabled = 0;
                    Log.i(TAG, "onCheckedChanged: flase");
                }
                for (ConfigurationBean.APPBean bean:configurationBean.getAPP() ){
                    switch (bean.getID()){
                        case APP_ID_APPIntersectionCollisionWarning:
                            bean.setEnabled(enabled);
                            break;

                    }
                }
                output2Json(configurationBean);

//                String[] files = fileList();
//                for (String file :files){
//                    Log.i(TAG, "onCheckedChanged: "+file );
//                }

            }
        });
        setting_UI_tlosa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int enabled = 0;
                if (isChecked){
//                    选中
                    enabled = 1;
                    Log.i(TAG, "onCheckedChanged: true" );
                }else {
                    enabled = 0;
                    Log.i(TAG, "onCheckedChanged: flase");
                }
                for (ConfigurationBean.APPBean bean:configurationBean.getAPP() ){
                    switch (bean.getID()){

                        case APP_ID_APPTrafficLightOptimalSpeedAdvisory:
                            bean.setEnabled(enabled);
                            break;

                    }
                }
                output2Json(configurationBean);
            }
        });


    }
    private void output2Json(ConfigurationBean jsonBean){
        Gson gson = new Gson();
        String filename = "configuration.json";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(gson.toJson(jsonBean).getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

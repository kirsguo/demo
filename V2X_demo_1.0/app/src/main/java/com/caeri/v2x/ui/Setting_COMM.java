package com.caeri.v2x.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import com.caeri.v2x.R;
import com.caeri.v2x.util.ConfigurationBean;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created  on 2017/4/20.
 *
 * @author Kirsguo
 *
 * 用于设置-设备配置设置
 */
public class Setting_COMM extends AppCompatActivity {
    private EditText targetIP ;
    private EditText targetPort;
    private EditText myPort;
    private EditText appName;
    private EditText boxType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting__comm);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar !=null){
            actionBar.hide();
        }
        TextView title = (TextView) findViewById(R.id.title_text);
        targetIP = (EditText)findViewById(R.id.setting_comm_targetip);
        targetPort = (EditText)findViewById(R.id.setting_comm_targetport);
        myPort = (EditText)findViewById(R.id.setting_comm_myport);
        appName = (EditText)findViewById(R.id.setting_comm_appname);
        boxType = (EditText)findViewById(R.id.setting_comm_boxtype);

        title.setText("设备");
        InputStream inputStream = null;
        try {
            inputStream = openFileInput("configuration.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final ConfigurationBean configurationBean = new GsonBuilder().create().fromJson(new InputStreamReader(inputStream),ConfigurationBean.class);
        ConfigurationBean.COMMBean bean = configurationBean.getCOMM().get(0);
        targetIP.setText(bean.getTargetip());
        targetPort.setText(bean.getTargetport());
        myPort.setText(bean.getMyport());
        appName.setText(bean.getAppname());
        boxType.setText(bean.getBoxtype());
    }
}

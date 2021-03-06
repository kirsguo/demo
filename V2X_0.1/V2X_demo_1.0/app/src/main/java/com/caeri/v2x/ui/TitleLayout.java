package com.caeri.v2x.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.caeri.v2x.R;

/**
 * Created  on 2017/4/6.
 *
 * @author Kirsguo
 *
 * 用于设置界面标题栏 一个返回按钮 一个文本框
 */

public class TitleLayout extends LinearLayout{
    public TitleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //LayoutInflater.from(context)构建一个LayoutInflater对象 动态加载一个布局
        LayoutInflater.from(context).inflate(R.layout.title,this);

//        返回按钮的点击事件
        Button titleBack = (Button) findViewById(R.id.title_back);
        titleBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)getContext()).finish();
            }
        });
    }
}

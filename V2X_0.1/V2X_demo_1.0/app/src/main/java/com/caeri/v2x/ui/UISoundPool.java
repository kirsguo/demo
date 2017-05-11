package com.caeri.v2x.ui;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;


import com.caeri.v2x.R;

import java.util.HashMap;

/**
 * Created  on 2017/5/8.
 *
 * @author Kirsguo
 *
 * 用于应用触发时提示音效
 */

public class UISoundPool {

    private SoundPool mSoundPool = null;
    private HashMap<Integer, Integer> soundID = new HashMap<Integer, Integer>();

    public void initSoundPool(Context context){
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(3);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_ALARM);
        builder.setAudioAttributes(attrBuilder.build());
        mSoundPool = builder.build();
        soundID.put(1,mSoundPool.load(context, R.raw.warning,1));
        soundID.put(2,mSoundPool.load(context,R.raw.alter,1));
        soundID.put(3,mSoundPool.load(context,R.raw.message,1));
    }
    public  void play(int i){
        switch (i){
            case 1:
                mSoundPool.play(soundID.get(1),1,1,0,0,1);
                break;
            case 2:
                mSoundPool.play(soundID.get(2),1,1,0,0,1);
                break;
            case 3:
                mSoundPool.play(soundID.get(3),1,1,0,0,1);
        }

    }
}

package com.softsquared.Hybrid.src.main;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.webkit.WebView;

import androidx.annotation.Nullable;

public class ForcedTerminationService extends Service {

    final static MainActivity mainActivity = new MainActivity();

    static WebView fWebView = mainActivity.mWebView;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //강제종료시 구현할 로직
//    @Override
//    public void onTaskRemoved(Intent rootIntent) { //핸들링 하는 부분
//        Log.e("Error","onTaskRemoved - 강제 종료 " + rootIntent);
//        Toast.makeText(this, "onTaskRemoved ", Toast.LENGTH_SHORT).show();
//
//        fWebView.loadUrl("javascript:android_back_press()");
//
//
//        stopSelf(); //서비스 종료
//    }

}

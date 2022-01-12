package com.softsquared.Hybrid.config;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    public static final String PREFERENCES_NAME = "rebuild_preference";
    private static final String DEFAULT_VALUE_STRING = "";

    @Override
    public void onTokenRefresh(){
//        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        Log.d("IDService","Refreshed token : "+refreshedToken);
//
//        SharedPreferences sf = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
//
//
//        String fcmToken = sf.getString("fcmToken","");
//
//        Log.d("IDService","sharedToekn : "+fcmToken);
//
//        if(refreshedToken != fcmToken) {
//            SharedPreferences.Editor editor = sf.edit();
//            editor.putString("fcmToken",refreshedToken );
//            editor.commit();
//        }
    }



}

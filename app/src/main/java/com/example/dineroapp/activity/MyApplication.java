package com.example.dineroapp.activity;

import android.app.Application;

import com.example.dineroapp.R;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token)); // ¡IMPORTANTE!
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}

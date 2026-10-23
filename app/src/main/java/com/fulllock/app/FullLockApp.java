package com.fulllock.app;

public class FullLockApp extends android.app.Application {
    @Override public void onCreate(){super.onCreate();Profiles.initialize(this);}
}

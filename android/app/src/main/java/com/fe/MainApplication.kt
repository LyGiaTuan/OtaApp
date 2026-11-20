package com.fe

import android.app.Application
import android.content.Intent
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactInstanceEventListener
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.bridge.ReactContext
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.fe.ota.OtaService
import com.google.gson.Gson
import java.io.File

class MainApplication : Application(), ReactApplication {

    val gson = Gson()
    val otaService by lazy { OtaService(this, gson) };

    override val reactHost: ReactHost by lazy {
        val bundleFilePath = otaService.getBundleFilePath()
        Log.d("found bundle file", "${bundleFilePath}");
        otaService.getBundle()

        getDefaultReactHost(
            context = applicationContext,
            packageList = PackageList(this).packages.apply {},
            jsBundleFilePath = bundleFilePath,
            useDevSupport = false,
            exceptionHandler = { e ->
                otaService.handleFail(e)
                restartApp();
            }
        )
    }


    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onCreate() {
        super.onCreate()
        loadReactNative(this);
    }
}

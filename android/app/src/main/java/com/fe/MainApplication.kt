package com.fe

import android.app.Application
import android.content.Intent
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.fe.ota.OtaService
import com.google.gson.Gson

class MainApplication : Application(), ReactApplication {


    override val reactHost: ReactHost by lazy {
        val gson = Gson()
        val otaService = OtaService(this, gson)

        otaService.getBundle()
        val bundleFile = otaService.getBundleFilePath()

        getDefaultReactHost(
            context = applicationContext,
            packageList = PackageList(this).packages.apply {},
            jsBundleFilePath = bundleFile,
            exceptionHandler = { e ->
                System.out.print(e);
                otaService.handleFail();
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
        loadReactNative(this)
    }
}

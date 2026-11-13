package com.fe.ota

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class OtaService(val context: Context, val gson: Gson) {
    private val prefs = context.getSharedPreferences("OTA_SERVICE", Context.MODE_PRIVATE)

    private fun insertBundleFile(bundleByteStream: InputStream) {
        val file = File(context.filesDir, "/index.android.bundle");
        bundleByteStream.use { `in` ->
            FileOutputStream(file).use { out ->
                val buffer = ByteArray(8192)
                var read: Int
                while ((`in`.read(buffer).also { read = it }) != -1) {
                    out.write(buffer, 0, read)
                }
            }
        }
    }

    fun getBundleFilePath(): String? {
        val canUseCustomBundle = prefs.getBoolean("canUseCustomBundle", false)
        if (canUseCustomBundle) {
            val bundleFile = File(context.filesDir, "/index.android.bundle")
            return if (bundleFile.exists()) bundleFile.absolutePath else null;
        }
        return null;
    }

    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()


    fun getBundle(
    ) {
        val nativeVersion = "2.0.0"
        val appId = "5"
        val deviceId = "1";

        val getBundleRequest = GetBundleRequest(nativeVersion, appId, deviceId)
        val body = gson.toJson(getBundleRequest)
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url("http://192.168.1.48:8000/bundle").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("gg HTTP", "Lỗi: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    prefs.edit().putBoolean("canUseCustomBundle", true).commit();
                    if (response.code == 200) {
                        insertBundleFile(response.body!!.byteStream())
                    }
                }
            }
        })
    }

    fun handleFail() {
        prefs.edit().putBoolean("canUseCustomBundle", false).commit();
    }

    class GetBundleRequest(val nativeVersion: String, val appId: String, val deviceId: String) {

    }
}
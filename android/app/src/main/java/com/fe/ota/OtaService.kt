package com.fe.ota

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        Log.d("Load bundle file path", "${canUseCustomBundle}")
        if (canUseCustomBundle) {
            val bundleFile = File(context.filesDir, "/index.android.bundle")
            Log.d("Is bundle exist", "${bundleFile.exists()} ${bundleFile.length()}")
            return if (bundleFile.exists()) bundleFile.absolutePath else null;
        }
        return null;
    }

    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS).writeTimeout(300, TimeUnit.SECONDS).build()

    fun getBundle(
    ) {
        val nativeVersion = "1.0.0"
        val appId = "7"
        val deviceId = "1";

        val url = "http://192.168.0.102:3000/bundle".toHttpUrl().newBuilder()
            .addQueryParameter("nativeVersion", nativeVersion)
            .addQueryParameter("appId", appId)
            .addQueryParameter("deviceId", deviceId)   // tự encode
            .build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("gg HTTP", "Lỗi gọi ${call.request().url}", e)

                // In chuỗi nguyên nhân
                var c: Throwable? = e
                while (c != null) {
                    Log.e("gg HTTP", "  -> ${c.javaClass.name}: ${c.message}")
                    c = c.cause
                }
                // OkHttp thử nhiều địa chỉ (IPv4/IPv6...) và gắn lỗi của từng lần thử vào suppressed
                e.suppressed.forEach {
                    Log.e(
                        "gg HTTP",
                        "  suppressed: ${it.javaClass.name}: ${it.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.code == 200) {
                        Log.d("Get bundle", "Success")
                        prefs.edit().putBoolean("canUseCustomBundle", true).commit();
                        insertBundleFile(response.body!!.byteStream())
                    }
                }
            }
        })
    }

    fun handleFail(e: Exception) {
        Log.d("Fail load bundle", "${e.message}")
        prefs.edit().putBoolean("canUseCustomBundle", false).commit();
    }

    class GetBundleRequest(val nativeVersion: String, val appId: String, val deviceId: String) {

    }
}
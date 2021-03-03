package com.sneyder.tjheboisalerts

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun isOreoOrLater(): Boolean {
    return Build.VERSION.SDK_INT >= 26
}

fun Context.notificationManager(): NotificationManager =
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val NOTFS_ENABLED = "NOTFS_ENABLED"
        const val CHANNEL_NAME = "Notificaciones de alertas"
    }

    private val scope = MainScope()
    private val prefs by lazy { getSharedPreferences("TheBois", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (isOreoOrLater()) createNotificationChannel()
        loadLabelColor()
        uploadFirebaseTokenId()
    }

    private fun loadLabelColor() {
        if (notfsAreEnabled()) {
            notfsEnabledLabel.setTextColor(Color.GREEN)
        } else {
            notfsEnabledLabel.setTextColor(Color.RED)
        }
    }

    private fun notfsAreEnabled(): Boolean {
        return prefs.getBoolean(NOTFS_ENABLED, false)
    }

    private fun saveNotfsEnabledToPrefs(enabled: Boolean) {
        prefs.edit {
            putBoolean(NOTFS_ENABLED, enabled)
        }
        loadLabelColor()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create the NotificationChannels
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        with(notificationManager()) {
            val notificationChannel = NotificationChannel(
                "Regular",
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Notificaciones necesarias"
            createNotificationChannel(notificationChannel)
        }
    }

    private fun uploadFirebaseTokenId() {
        scope.launch {
            val token = getFirebaseTokenId() ?: return@launch
            Log.d(TAG, "uploadFirebaseTokenId: $token")
            val result = try {
                withContext(IO) { sendFirebaseTokenId(token) }
            } catch (e: Exception) {
                null
            }
            if (result == true) {
                Toast.makeText(
                    this@MainActivity,
                    "Notifications enabled successfully",
                    Toast.LENGTH_LONG
                ).show()
            }
            saveNotfsEnabledToPrefs(result == true)
        }
    }

    private suspend fun sendFirebaseTokenId(token: String): Boolean {
        return provideTheBoisApi(provideRetrofit(provideHttpClient(), gson())).sendToken(token)
    }

    private suspend fun getFirebaseTokenId(): String? {
        return suspendCoroutine { cont ->
            FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(
                            "MainActivity",
                            "FirebaseInstanceId getInstance failed ${task.exception}"
                        )
                        cont.resume(null)
                        return@OnCompleteListener
                    }
                    val token = task.result?.token
                    Log.d("MainActivity", "FirebaseInstanceId getInstance token=$token")
                    cont.resume(token)
                })
        }
    }

    private fun provideHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun gson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    private fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(TheBoisApi.END_POINT)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private fun provideTheBoisApi(retrofit: Retrofit): TheBoisApi =
        retrofit.create(TheBoisApi::class.java)

}
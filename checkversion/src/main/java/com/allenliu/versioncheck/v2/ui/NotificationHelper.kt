package com.allenliu.versioncheck.v2.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.allenliu.versioncheck.R
import com.allenliu.versioncheck.core.JumpActivity
import com.allenliu.versioncheck.core.VersionFileProvider
import com.allenliu.versioncheck.utils.ALog
import com.allenliu.versioncheck.v2.builder.BuilderManager
import com.allenliu.versioncheck.v2.builder.DownloadBuilder
import com.allenliu.versioncheck.v2.builder.NotificationBuilder
import java.io.File

/**
 * Created by allenliu on 2018/1/19.
 */
class NotificationHelper(private val context: Context) {
    var notification: NotificationCompat.Builder? = null
    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var isDownloadSuccess = false
    private var isFailed = false
    private var currentProgress = 0
    private var contentText: String? = null

    /**
     * update notification progress
     *
     * @param progress the progress of notification
     */
    fun updateNotification(progress: Int) {

        BuilderManager.doWhenNotNull {
            if (isShowNotification) {
                notification?.let {

                    if (progress - currentProgress > 5 && !isDownloadSuccess && !isFailed) {
                        ALog.e("update progress notification")
                        contentText?.let { text ->

                            it.setContentText(String.format(text, progress))
                            it.setProgress(100, progress, false)
                            manager.notify(NOTIFICATION_ID, it.build())

                            currentProgress = progress
                        }


                    }
                }

            }
        }

    }

    /**
     * show notification
     */
    fun showNotification() {
        currentProgress = 0
        isDownloadSuccess = false
        isFailed = false

        BuilderManager.doWhenNotNull {
            if (isShowNotification) {
                ALog.e("reset notification")
                notification = createNotification(this)
                manager.notify(NOTIFICATION_ID, notification?.build())
            }
        }

    }

    /**
     * show download success notification
     */
    fun showDownloadCompleteNotifcation(file: File) {
        currentProgress = 0
        BuilderManager.doWhenNotNull {
            isDownloadSuccess = true
            if (!isShowNotification) return@doWhenNotNull
            val i = Intent(Intent.ACTION_VIEW)
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = VersionFileProvider.getUriForFile(
                    context,
                    context.packageName + ".versionProvider",
                    file
                )
                ALog.e(context.packageName + "")
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(file)
            }
            //设置intent的类型
            i.setDataAndType(
                uri,
                "application/vnd.android.package-archive"
            )
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                i,
                PendingIntent.FLAG_IMMUTABLE
            )
            createInstallNotification().apply {
                setContentIntent(pendingIntent)
                setContentText(context.getString(R.string.versionchecklib_download_finish))
                manager.cancelAll()
                manager.notify(INSTALL_NOTIFICATION_ID, build())
            }


        }

    }

    fun showDownloadFailedNotification() {
        isDownloadSuccess = false
        isFailed = true
        BuilderManager.doWhenNotNull {
            if (isShowNotification) {
                notification?.let {
                    ALog.e("show download failed notification")
                    val intent = Intent(context, JumpActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val pendingIntent =
                        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    it.setContentIntent(pendingIntent)
                    it.setContentText(context.getString(R.string.versionchecklib_download_fail))

                    it.setProgress(0, 0, false)
                    manager.notify(NOTIFICATION_ID, it.build())
                }

            }
        }

    }

    private fun createNotification(versionBuilder: DownloadBuilder): NotificationCompat.Builder {

        val builder: NotificationCompat.Builder?
        val libNotificationBuilder: NotificationBuilder = versionBuilder.notificationBuilder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelid,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(false)
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }
        builder = NotificationCompat.Builder(context, channelid)
        builder.setAutoCancel(true)
        builder.setSmallIcon(versionBuilder.notificationBuilder.icon)
        //set content title
        var contentTitle: String? = context.getString(R.string.app_name)
        if (libNotificationBuilder.contentTitle != null) contentTitle =
            libNotificationBuilder.contentTitle
        builder.setContentTitle(contentTitle)
        //set ticker
        var ticker: String? = context.getString(R.string.versionchecklib_downloading)
        if (libNotificationBuilder.ticker != null) ticker = libNotificationBuilder.ticker
        builder.setTicker(ticker)
        //set content text
        contentText = context.getString(R.string.versionchecklib_download_progress)
        if (libNotificationBuilder.contentText != null) contentText =
            libNotificationBuilder.contentText
        builder.setContentText(String.format(contentText!!, 0))
        builder.setContentIntent(null);
        if (libNotificationBuilder.isRingtone) {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r?.play()
        }
        return builder
    }

    private fun createInstallNotification(): NotificationCompat.Builder {
        val notifcationBuilder = NotificationCompat.Builder(context, channelid)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.versionchecklib_version_service_runing))
            .setAutoCancel(false)

        BuilderManager.getDownloadBuilder()?.notificationBuilder?.icon?.let {
            notifcationBuilder.setSmallIcon(
                it
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelid,
                "version_service_name",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }
        return notifcationBuilder
    }

    fun onDestroy() {
        manager.cancel(NOTIFICATION_ID)
    }

    //            notificationChannel.setLightColor(getColor(R.color.versionchecklib_theme_color));

    companion object {
        const val NOTIFICATION_ID = 1
        const val INSTALL_NOTIFICATION_ID = 2

        private const val channelid = "version_service_id"
        private const val channelName = "version_service_name"


        fun createSimpleNotification(context: Context): Notification {
            val notifcationBuilder = NotificationCompat.Builder(context, channelid)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.versionchecklib_version_service_runing))
                .setAutoCancel(false)

            BuilderManager.getDownloadBuilder()?.notificationBuilder?.icon?.let {
                notifcationBuilder.setSmallIcon(
                    it
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    channelid,
                    channelName, NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }
            return notifcationBuilder.build()
        }
    }

    init {
        currentProgress = 0
    }
}
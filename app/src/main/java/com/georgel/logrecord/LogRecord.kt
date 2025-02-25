package com.georgel.logrecord

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.text.format.DateFormat
import android.widget.RemoteViews

class LogRecord {
    private constructor()
    companion object {
       private var logRecord: LogRecord?=null
        fun get():LogRecord{
            if(logRecord==null){
                logRecord=LogRecord()
            }
            return logRecord!!
        }
    }
    private var context: Context?=null
    private var start_action="log_start"
    private var stop_action="log_stop"
    private var prefix:String=""
    private var folder:String="ALog"
    private var informat:String="yyyy-MM-dd"
    private var notifyId=11
    private var notifydesc:String="日志录制中"
    private var ondesc:String="开始LOG记录"
    private var offdesc:String="停止LOG记录"
    private var notifyIcon:Int=R.mipmap.ic_launcher
    private val handler=android.os.Handler(Looper.getMainLooper())
     fun init(context: Context):LogRecord{
         this.context=context
        return this
    }
    private var broadcastReceiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                start_action->{
                    startRecordLog(true)
                }
                stop_action->{
                    startRecordLog(false)
                }
            }
        }
    }
    fun setControlBroadcastAction(start_action:String, stop_action:String):LogRecord{
        this.start_action=start_action
        this.stop_action=stop_action
        val myiFilter = IntentFilter()
        myiFilter.addAction(start_action)
        myiFilter.addAction(stop_action)
        context?.registerReceiver(broadcastReceiver, myiFilter)
        return this
    }
    fun setFolder(folder:String):LogRecord{
        if(folder!=null)
        this.folder=folder
        return this
    }
    fun setPrefix(prefix:String):LogRecord{
        if(prefix!=null)
        this.prefix=prefix
        return this
    }
    fun setInformat(informat:String):LogRecord{
        if(informat!=null)
        this.informat=informat
        return this
    }
    fun setNotifyId(notifyId:Int):LogRecord{
        if(notifyId>0)
        this.notifyId=notifyId
        return this
    }
    fun setNotifyDesc(notifydesc:String):LogRecord{
        if(notifydesc!=null)
        this.notifydesc=notifydesc
        return this
    }
    fun setNotifyIcon(notifyIcon:Int):LogRecord{
        if(notifyIcon>0)
        this.notifyIcon=notifyIcon
        return this
    }
    fun setOnDesc(ondesc:String):LogRecord{
        if(ondesc!=null)
        this.ondesc=ondesc
        return this
    }
    fun setOffDesc(offdesc:String):LogRecord{
        if(offdesc!=null)
        this.offdesc=offdesc
        return this
    }

    fun release(){
        context?.unregisterReceiver(broadcastReceiver)
        LogcatFileManager.getInstance().stopLogcatManager()
    }
    private fun startRecordLog(start: Boolean) {
        if (start) {
            LogcatFileManager.getInstance().startLogcatManager(
                "${folder}/" + DateFormat.format(informat, System.currentTimeMillis()), prefix, context
            )
        } else {
            LogcatFileManager.getInstance().stopLogcatManager()
        }
        handler.postDelayed(Runnable {
           showNotifyLogView(
                context!!, notifyId, notifyIcon, prefix+notifydesc
            )
        }, 200)
    }
    fun showNotifyLogView(context: Context, id: Int, iconId: Int, msg: String?): Notification? {
        val builder = Notification.Builder(context)
        builder.setAutoCancel(false)
            .setSmallIcon(iconId)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 28) {
            val channel = NotificationChannel(
                "" + id,
                "channel$id",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            builder.setChannelId("" + id) //不设置一致 不会显示
            notificationManager.createNotificationChannel(channel)
        }
        builder.setContentTitle(msg)
        builder.setSound(null)
        val notification = builder.build()
        val remoteViews = RemoteViews(context.packageName, R.layout.lognotify)
        notification.bigContentView = remoteViews
        var action = start_action
        val prefix = LogcatFileManager.getInstance().prefix
        if (LogcatFileManager.getInstance().isStart) {
            action = stop_action
            remoteViews.setTextViewText(R.id.log_start, prefix + offdesc)
        } else {
            remoteViews.setTextViewText(R.id.log_start, prefix + ondesc)
        }
        remoteViews.setOnClickPendingIntent(
            R.id.log_start, PendingIntent.getBroadcast(
                context,
                11, Intent(action), 0
            )
        )
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
        notificationManager.notify(id, notification)
        return notification
    }
}
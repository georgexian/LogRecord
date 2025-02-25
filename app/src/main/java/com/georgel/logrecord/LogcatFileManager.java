package com.georgel.logrecord;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class LogcatFileManager {
    private static final String TAG = LogcatFileManager.class.getSimpleName();
    private static LogcatFileManager INSTANCE = null;
    private static String PATH_LOGCAT;
    private LogDumper mLogDumper = null;
    private int mPId;
    private SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMdd-HH-mm-ss");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean isStart;
    private boolean mPause;
    private Context mCxt;
    private static String SWITCH_LOG_FILE_ACTION = "SWITCH_LOG_FILE";    //切换日志文件action
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };
    private BroadcastReceiver logTaskReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction());
            if (SWITCH_LOG_FILE_ACTION.equals(intent.getAction())) {
                stopLogcatManager();
                startLogcatManager(logDir, prefix,context);
            }
        }
    };
    private String folderPath;

    private long getAvailableSize(String path) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getStorageState(new File(path)))) {
            try {
                StatFs statFs = new StatFs(path);
                long blockSize = statFs.getBlockSizeLong();
                long blockavailable = statFs.getAvailableBlocksLong();
                long blockavailableTotal = blockSize * blockavailable / 1024 / 1024;

                return blockavailableTotal;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("dou", "getAvailableSize---error");
                return -1;
            }

        }
        return -1;
    }

    private Runnable checkremainLogSpace = new Runnable() {
        @Override
        public void run() {
            new CheckLogSizeThread().start();
            mHandler.postDelayed(this, 30 * 1000);

        }
    };
    int count=0;
    private class CheckLogSizeThread extends Thread{
        @Override
        public void run() {
            File file =new File(folderPath);
            List< File> files = Arrays.asList(file.listFiles());
            Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    String s1=o1.getName().substring(0,o1.getName().lastIndexOf("."));
                    String s2=o2.getName().substring(0,o2.getName().lastIndexOf("."));
                   // Log.e("FileComparator",s1+"="+s2);
                    return s1.compareTo(s2);
                }
            });


            if (count>0&&files != null && files.size() > count) {
                Log.d(TAG,"日志文件个数："+files.size());
                for (int i = 0; i<files.size()-count; i++) {
                    File file1 = files.get(i);
                    Log.d(TAG,"删除过期日志文件：" + file1.getAbsolutePath());
                    if (file1.exists())
                        file1.delete();
                }
            }else if(files != null){
                for (int i =0;  i<files.size(); i++) {
                    File file1 = files.get(i);
                    long availableSize = getAvailableSize("/sdcard");
                    if (availableSize<=800) {
                        Log.d(TAG, "checkremainLogSpace 空间低于800M 删除旧文件");
                        Log.d(TAG,"删除过期日志文件：" + file1.getAbsolutePath());
                        if (file1.exists())
                            file1.delete();
                        continue;
                    }else{
                        break;
                    }
                }
            }
        }
    }
    public void setLimitLogs(int num){
        count=num;
    }
    public boolean isStart() {
        return isStart;
    }

    public static LogcatFileManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogcatFileManager();
        }
        return INSTANCE;
    }

    public void pause(boolean pause) {
        mPause = pause;
    }

    public interface LogCallback {
        void log(String text);
    }

    private LogCallback callback;

    public void setCallback(LogCallback callback) {
        this.callback = callback;
    }

    private LogcatFileManager() {
        mPId = android.os.Process.myPid();
    }

    private String logDir = "MMF-Logcat";
    private String prefix = "";//前缀

    public String getPrefix() {
        return prefix;
    }

    public void setDateNameFormat(String format) {
        simpleDateFormat1 = new SimpleDateFormat(format);
    }

    public void startLogcatManager(String logDir,String prefix, Context context) {
        mCxt = context;
        this.logDir = logDir;
        if(!TextUtils.isEmpty(prefix))
        this.prefix = prefix;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + logDir;
        } else {
            folderPath = context.getFilesDir().getAbsolutePath() + File.separator + logDir;
        }
        LogcatFileManager.getInstance().start(folderPath);
        IntentFilter logTaskFilter = new IntentFilter();
        logTaskFilter.addAction(SWITCH_LOG_FILE_ACTION);
        mCxt.registerReceiver(logTaskReceiver, logTaskFilter);

        deploySwitchLogFileTask();
        mHandler.post(checkremainLogSpace);
    }

    /**
     * 部署日志切换任务，每天凌晨切换日志文件
     */
    private void deploySwitchLogFileTask() {
        Intent intent = new Intent(SWITCH_LOG_FILE_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(mCxt, 0, intent, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // 部署任务
        AlarmManager am = (AlarmManager) mCxt.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, sender);
    }

    public void stopLogcatManager() {
        LogcatFileManager.getInstance().stop();
        if(mHandler!=null){
            mHandler.removeCallbacks(checkremainLogSpace);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mCxt, ""+folderPath, Toast.LENGTH_SHORT).show();
                }
            });
            mHandler.removeCallbacksAndMessages(null);
        }
        try{
           mCxt.unregisterReceiver(logTaskReceiver);
        }catch(Exception e){
          e.printStackTrace();
        }
    }


    private void setFolderPath(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("The logcat folder path is not a directory: " + folderPath);
        }
        PATH_LOGCAT = folderPath.endsWith("/") ? folderPath : folderPath + "/";

    }


    private void start(String saveDirectoy) {
        setFolderPath(saveDirectoy);
        stop();
        if (mLogDumper == null) {
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
        }
        mLogDumper.start();
        isStart = true;
    }


    private void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
        isStart = false;
    }


    private class LogDumper extends Thread {
        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        String cmds = null;
        private String mPID;
        private FileOutputStream out = null;
        private  File file;
        private String dir;


        public LogDumper(String pid, String dir) {
            this.dir=dir;
            mPID = pid;
            try {
                file = new File(dir, simpleDateFormat1.format(new Date()) + ".log");
                out = new FileOutputStream(file, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            /**
             * * * log level：*:v , *:d , *:w , *:e , *:f , *:s * * Show the
             * current mPID process level of E and W log. * *
             * * */
            // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
            cmds = "logcat | grep " + mPID ;
        }


        public void stopLogs() {
            mRunning = false;
        }

        @Override
        public void run() {
            try {
                Runtime.getRuntime().exec("logcat -G 20m");
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
                if (out != null){
                    StringBuffer buffer =new StringBuffer();
                    buffer.append(Build.MODEL).append(" ").append(Build.MANUFACTURER).append("\n");
                    buffer.append(Build.DISPLAY).append(" ").append(mCxt.getPackageName()).append("\n");
                    out.write(buffer.toString().getBytes());
                }
                String line = null;
                while (mRunning ) {
                    if (!mRunning) {
                        break;
                    }
                    if ((line = mReader.readLine()) == null||line.length() == 0) {
                        continue;
                    }
                    StringBuffer stringBuffer =new StringBuffer();
                    if (out != null && line.contains(mPID)) {
                        stringBuffer.append(simpleDateFormat2.format(new Date())).append("  ").append(line).append("\n");
                        //String l = simpleDateFormat2.format(new Date()) + "  " + line;
                        long length = file.length();
                        if(length>300*1024*1024){
                          /*  String absolutePath = file.getAbsolutePath();
                            file.delete(); //文件过大，丢弃日志
                            file=new File(absolutePath);*/
                            file = new File(dir, prefix+simpleDateFormat1.format(new Date()) + ".log");
                            try{
                                out = new FileOutputStream(file, true);
                            }catch(Exception e){
                              e.printStackTrace();
                            }
                        }
                        if(!file.exists()){
                            file = new File(dir, simpleDateFormat1.format(new Date()) + ".log");
                            try{
                                out = new FileOutputStream(file, true);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        String l=stringBuffer.toString();
                        if (!mPause&&!l.contains("BufferPoolAccessor")&&!l.contains("CrashReport"))
                            out.write((l).getBytes());
                        if (callback != null) {
                            String d = simpleDateFormat2.format(new Date()) + "  " + line;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.log(d);
                                }
                            });

                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            out = null;
                        }
                    }
                }


            }
        }

    }
}

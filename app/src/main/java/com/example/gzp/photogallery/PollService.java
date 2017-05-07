package com.example.gzp.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.gzp.photogallery.Model.GalleryItem;
import com.example.gzp.photogallery.Util.FlickrFetchr;
import com.example.gzp.photogallery.Util.QueryPreferences;

import java.util.List;

/**
 * Created by Ben on 2017/3/12.
 * 接收到首个命令（Intent）时，IntentService完成启动，触发后台线程，然后将命令放入队列
 * 对每一条命令在后台线程上调用onHandleIntent（Intent）方法
 */

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL=1000*60;//60 seconds 更新间隔
    public static final String ACTION_SHOW_NOTIFICATION=
            "com.example.gzp.photogallery.SHOW_NOTIFICARION";       //过滤器参数
    public static final String PERM_PRIVATE =       //私有权限，传入sendBroadcast
            "com.example.gzp.photogallery.PRIVATE";   //三个地方同时出现，
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    /**
     * 使用PendingIntent。使用PendingIntent打包一个intent：“我想启动PollService服务。”
     * 然后，将其发送给系统中的其他部件，如AlarmManager。
     * 实现一个启停定时器的setServiceAlarm(Context,boolean)方法，
     * @param context
     * @param isOn 是否开启服务
     */
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);

        /*打包了一个Context.startService(Intent) 方法的调用
            四个参数：发送Intent的Context，区分PendingIntent来源的请求代码
            一个待发送的Intent对象、一组来决定如何创建PendingIntent的标识符
        */
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService
                (context.ALARM_SERVICE);

        /*设置和取消定时器
        * 设置定时器用AlarmManager.setRepeating 方法
        * 四个参数：定时器时间基准常量、定时器启动的时间，
        * 定时器循环的时间间隔以及一个到时要发送的PendingIntent。
        *AlarmManager.ELAPSED_REALTIME 是基准时间值，
        *  这表明我们是以SystemClock.elapsedRealtime()走过的时间来确定何时启动时间的。
        * */
        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
            Log.i(TAG, "setServiceAlarm: alarm on");
        } else {
            alarmManager.cancel(pi);  //撤销定时器
            pi.cancel();//  取消当前活动PendingIntent
            Log.i(TAG, "setServiceAlarm: alarm off");
        }
        QueryPreferences.setAlarmOn(context,isOn);
    }


    /**
     * 一个PendingIntent只能登记一个计时器。因此判断是否存在来确认计时器是否激活
     * @param context 当前活动
     * @return 返回PendingIntent是否存在
     */
    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi!=null;
    }
    public PollService() {
        super(TAG);
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetworkAvailableAndConnected()){
            Log.i(TAG, "onHandleIntent: Connected failed");
            return;
        }
        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos(1);
        } else {
            items = new FlickrFetchr().searchPhotos(query,1);
        }

        if (items.size() == 0) {
            return;
        }
        String resultId=items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "onHandleIntent: Got an old result: " + resultId);
        } else {
            Log.i(TAG, "onHandleIntent: Got a new result: "+resultId);
            Resources resources=getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)    //通知的构造方法
                    .setTicker(resources.getString(R.string.new_picture_title))//设置时钟标题
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)//小图标
                    //通知在下拉抽屉中的外观
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getString(R.string.new_picture_text))
                    .setContentIntent(pi)//点击通知要启动的动作
                    .setAutoCancel(true)//点击时清除抽屉中图标
                    .build();
//            //从当前的context中取出一个实例，调用notify贴出通知
//            NotificationManagerCompat notificationManager=NotificationManagerCompat.from(this);
//            notificationManager.notify(0, notification);
//            //只要有新结果就会对外广播
//            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION),PERM_PRIVATE);
            showBackgroundNotification(0,notification);
            Log.i(TAG, "onHandleIntent: send notification");

        }

        QueryPreferences.setLastResultId(this,resultId);
    }
    private void showBackgroundNotification(int request, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, request);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i,PERM_PRIVATE,null,null,
                Activity.RESULT_OK,null,null);//resultcode 初始值

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable=cm.getActiveNetworkInfo()!=null;
        boolean isNetworkConnected=isNetworkAvailable&&cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}

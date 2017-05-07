package com.example.gzp.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.gzp.photogallery.Util.QueryPreferences;

/**
 * Created by Ben on 2017/3/21.
 * 通过监听带有BOOT_COMPLETED操作的broadcast intent，可获知设备是否已完成启动
 * 登记broadcast receiver，首先要创建它,并在配置文件中注册
 * 使用receiver标签并在其中包含相应的intent-filter。
 * StartupReceiver会监听BOOT_COMPLETED操作，而该操作也需要配置使用权限。
 */

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: "+intent.getAction());
        /**
         * 点开工具栏的Start Polling 打开服务，并写入SharedPreferences 为true
         * 重启设备，开机后 运行此方法，SharedPreferences 返回isOn=true 然后会启动服务
         */
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context,isOn);
    }
}

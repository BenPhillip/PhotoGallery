package com.example.gzp.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Ben on 2017/3/21.
 * 使用动态的broadcast receiver 在代码中登记和取消登记
 * receiver 像监听器一样作为内部类实现
 *
 */

public abstract class VisibleFragment extends Fragment {
    private static final String TAG="VisibleFragment";


    @Override
    public void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        //传入自定义的权限
        getActivity().registerReceiver(mOnShowNotification, filter,PollService.PERM_PRIVATE,null);

    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: cancling notification");
            //如果在前台 就收到 并修改结果码
            setResultCode(Activity.RESULT_CANCELED);

        }
    };
}

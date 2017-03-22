package com.example.gzp.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.SearchView;

/**
 * Created by Ben on 2017/3/12.
 *通过shared preferences (共享参数)来保存查询的字符串
 * QueryPreferences类用于读取和写入查询的字符串
 * 最好使用PreferenceManager.getDefaultSharedPreferences(Context)方法，
 * 该方法会返回具有私有权限和默认名称的实例（仅在当前应用内可用）。
 */
public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String PREF_IS_ALARM_ON="isAlarmOn";

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID,null);
    }

    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID,lastResultId)
                .apply();
    }

    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY,null);//第二个参数为默认返回值
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()             //获取Editor。类似FragmentTransaction 操作
                .putString(PREF_SEARCH_QUERY,query)
                .apply();           //提交一组数据操作
    }

    public static boolean isAlarmOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON, false);
    }

    public static void setAlarmOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON,isOn)
                .apply();
    }
}

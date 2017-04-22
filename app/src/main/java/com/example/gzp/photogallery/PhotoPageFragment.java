package com.example.gzp.photogallery;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by Ben on 2017/3/23.
 * 1.告诉WebView要打开的URL
 * 2.启用JavaScript.默认是禁用的。
 * 3.覆盖webView的shouldOverrideUrlLoading(WebView,String)方法
 */

public class PhotoPageFragment extends VisibleFragment implements PhotoPageActivity.CallBacks {
    private static final String TAG = "PhotoPageFragment";
    private static final String ARG_URI = "photo_page_url";

    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;

    public static PhotoPageFragment newInstance(Uri uri) {

        Bundle args = new Bundle();
        args.putParcelable(ARG_URI,uri);

        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUri=getArguments().getParcelable(ARG_URI);
    }

    /**
     * 使用JavaScript 会提示警告信息，
     * 使用 @SuppressLint可以阻止Android Lint 的警告
     */
    @SuppressLint("SetJavaScriptEnable")//
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_photo_page_progress_bar);
        mProgressBar.setMax(100);

        mWebView = (WebView) v.findViewById(R.id.fragment_photo_page_web_view);
        //获取WebSetting的实例，并启用JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView webView, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }
            @Override
            public void onReceivedTitle(WebView webView, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });

        //WebClient是事件的接口，可以自己实现来响应各种渲染事件
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                Log.d(TAG, "shouldOverrideUrlLoading: "+scheme);
                if(scheme.equalsIgnoreCase("HTTP")||scheme.equalsIgnoreCase("HTTPS")){
                    return false;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    getActivity().startActivity(intent);
                    return true;
                }
            }

        });

        mWebView.loadUrl(mUri.toString());//加载URL必须在配置完后进行
        return v;
    }
    //实现回调接口
    @Override
    public boolean doGoback() {
        if(mWebView.canGoBack()){
            mWebView.goBack();
            return false;
        }
        return true;
    }
}

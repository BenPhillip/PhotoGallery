package com.example.gzp.photogallery;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Ben on 2017/3/5.
 * ThumbnailDownloader类使用了<T>泛型参数。
 * ThumbnailDownloader类的使用者（这里指PhotoGalleryFragment），
 * 需要使用某些对象来识别每次下载，并确定该使用下载图片更新哪个UI元素。
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;


    private Boolean mHasQuit = false;
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloaderListener<T> {
        void onThumbnailDownloaded(T target, byte[] bytes);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloaderListener<T> listener) {
        mThumbnailDownloadListener=listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler=responseHandler;
    }

    /**
     * HandlerThread.onLooperPrepared()是在Looper首次检查消息队列之前调用的
     * 该方法成了创建Handler实现的好地方
     * 首先检查消息类型，再获取obj值，然后将其传递给handleRequest(...)方法处理
     */
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.d(TAG, "handleMessage: Got a request for URL:" + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }
/**
 * 缩略图队列。它也是PhotoAdapter在其onBindViewHolder(...)实现方法中要调用的方法
 * 使用PhotoHolder和URL的对应关系更新mRequestMap
 * 随后， 我们会从mRequestMap中取出图片URL， 以保证总是使用了匹配PhotoHolder实例的最新下载请求URL
 * RecyclerView中的ViewHolder是会不断回收重用的
 * @param target 标识具体那次下载 这里传入的是ViewHolder
 * @param url 传入的是URL下载链接
 *
 */
    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "queueThumbnail: Got a URL:" + url);
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            /*消息带一个what属性，和obj类(这里指PhotoHolder)*/
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }

    }

    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    /**
     * 处理请求，先传进来PhotoHolder，从中取出url，判断是否有图片url 没有就结束方法。
     * 有url获取byte数组，然后再进行解码 转换为位图
     * 通过主线程的Handler发送信息，并实现处理信息的方法
     * 处理信息先判断url是否为要处理的url||是否停止下载请求
     * (因为循环使用holder 完成Bitmap后 对应的holder--url 发生改变)
     * 最后，从requestMap中删除配对的PhotoHolder-URL，然后将位图设置到目标PhotoHolder上。
     * @param target 为一个PhotoHolder，不能在方法中再被赋值
     */
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }

            /*最后，使用BitmapFactory把getUrlBytes(...)返回的字节数组转换为位图。*/
            final byte[] bitmapBytes = new FlickrFetchr().getUrlBytesByOkHttp(url);
//            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

            final String  pictureData=new FlickrFetchr().getUrlStringByOkHttp(url);
            Log.i(TAG, "handleRequet: Bitmap created");


            /*
            Handler.post(Runnable)是一个发布Message的便利方法

            Runnable myRunnable = new Runnable() {
                @Override
                 public void run() {
                 }
               };
               Message m = mHandler.obtainMessage();
               m.callback = myRunnable;
               Message设有回调方法时，
            它从消息队列取出后，是不会发给target Handler的。
            */
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url||mHasQuit) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,
                            bitmapBytes);
                    Log.d(TAG, "Request send Bitmap");
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "handleRequet: Error downloading image", ioe);
        }
    }
}

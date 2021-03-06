package com.example.gzp.photogallery.Model;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Ben on 2017/3/2.
 * 保存图片的信息
 *从Flickr下载的图片都有对应的关联网页
 * 可按以下格式创建单个图片的URL：
 *http://www.flickr.com/photos/user-id/photo-id
 *
 */

public class GalleryItem {
    @SerializedName("title")
    private String mCaption;    //  图片说明
    @SerializedName("id")
    private String mId;
    @SerializedName("url_s")
    private String mUrl;
    @SerializedName("owner")
    private String mOwner; //owner属性值就是user-id



    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public Uri getPhotoPageUri() {
        return Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }



    @Override
    public String toString() {
        return mCaption;
    }


    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

}

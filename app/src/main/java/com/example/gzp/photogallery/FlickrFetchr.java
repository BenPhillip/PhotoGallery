package com.example.gzp.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;



import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by Ben on 2017/3/1.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickrFetch";
    private static final String API_KEY="68ad1e04da47c96f8018a749c40466c9";
    private static final String FETCH_RECENTS_METHOD="flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    public static final String  PER_PAGE="20";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s")
            .appendQueryParameter("per_page",PER_PAGE)
            .build();




    public String getUrlStringByOkHttp(String urlSpec)throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(urlSpec)
                .build();
        Response response=client.newCall(request).execute();
        return response.body().string();
    }

    public byte[] getUrlBytesByOkHttp(String urlSpec)throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(urlSpec)
                .build();
        Response response=client.newCall(request).execute();
        return response.body().bytes();
    }

    public List<GalleryItem> fetchRecentPhotos(int page) {
        String url=buildUrl(FETCH_RECENTS_METHOD,null,page);
        //        return downloadGalleryItems(url);
        return downloadGalleryItemsByGson(url);
    }

    public List<GalleryItem> searchPhotos(String query,int page) {
        String url = buildUrl(SEARCH_METHOD, query,page);
        //        return downloadGalleryItems(url);
        return downloadGalleryItemsByGson(url);
    }



    private List<GalleryItem> downloadGalleryItemsByGson(String url) {
        List<GalleryItem> items = new ArrayList<>();
        Log.i(TAG, url);
        Gson gson=new Gson();
        try {
            FlickrJson flickrJson = gson.fromJson
                    (getUrlStringByOkHttp(url), FlickrJson.class);
            items=flickrJson.photos.getPhoto();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;

    }

    private String buildUrl(String method, String query,int page) {
        Uri.Builder urlBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page",String.valueOf(page));
        if (method.equals(SEARCH_METHOD)) {
            urlBuilder.appendQueryParameter("text", query);
        }
        return urlBuilder.build().toString();
    }
}

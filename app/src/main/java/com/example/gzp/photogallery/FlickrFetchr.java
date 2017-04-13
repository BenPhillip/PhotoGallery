package com.example.gzp.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


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



    public byte[]getUrlBytes(String urlSpec)throws IOException{
        URL url = new URL(urlSpec);
        //        创建一个要访问URL的连接对象
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream=connection.getInputStream();
/*            虽然HttpURLConnection对象提供了一个连接，
                但只有在调用getInputStream（）方法时才会真正建立连接对象
                如果是POST请求则调用getOutStream
*/
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " :with " + urlSpec);
            }

            int bytesRead=0;
            byte[] buffer = new byte[1024];
            /*一次最多向输入流读取buffer.length长度的数据，并返回读取的字节数*/
            while((bytesRead= inputStream.read(buffer))>0){
                 /*向输出流写入从buffer[0]到buffer[byteRead]的数据*/
                outputStream.write(buffer,0,bytesRead);
            }
            outputStream.close();
              /*创建一个新分配的 byte 数组。
              其大小是此输出流的当前大小，并且缓冲区的有效内容已复制到该数组中*/
            return outputStream.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

/*    public String getUrlString(String urlSpec) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request=new Request.Builder()
                .url(urlSpec)
                .build();
        Response response=client.newCall(request).execute();
        Log.i(TAG, response.body().toString());
        return response.body().toString();
    }*/

    public String getUrlString(String urlSpec)throws  IOException{
          /*解码指定的 byte 数组，构造一个新的 String。
          新 String 的长度是字符集的函数，因此可能不等于 byte 数组的长度。 */
        return new String(getUrlBytes(urlSpec));

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

    private List<GalleryItem> downloadGalleryItems(String url) {

        List<GalleryItem> items = new ArrayList<>();

        try {
/*            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();*/
            String jsonString = getUrlString(url);
            Log.i(TAG, "fetchItems: recrived josn " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);
        } catch (JSONException je) {
            Log.e(TAG, "fetchItems: Failed to parse JSON",je );
        } catch (IOException ioe) {
            Log.e(TAG, "fetchItems: Failed to fetch items", ioe);
        }
        return items;
    }

    /*取出每张照片的信息
        并对每张照片的url_s属性进行检查，并不是每张照片都有这属性，如果有则添加
    */
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException,JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray=photosJsonObject.getJSONArray("photo");

        for (int i = 0; i <photoJsonArray.length() ; i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item=new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }

    }

    private List<GalleryItem> downloadGalleryItemsByGson(String url) {
        List<GalleryItem> items = new ArrayList<>();
        Log.i(TAG, url);
        Gson gson=new Gson();
        try {
            FlickrJson flickrJson = gson.fromJson(getUrlString(url), FlickrJson.class);
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

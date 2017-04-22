# PhotoGallery
## 本代码根据Android编程权威指南的基础上进行改进
### *改动内容说明*
1. 用Gson代替JsonObeject处理JSON数据(挑战练习：Gson)
2. 增加了上拉下拉加载更多和刷新（挑战练习：分页)
3. 挑战练习：动态调整布局
4. 使用Okttp代替HttpURLConnection 访问网络
5. 使用Glide处理图片
6. 挑战练习：使用后退键浏览历史网页
7. 挑战练习：非HTTP 链接支持
***
#### 具体说明
* Gson
``` java
public class FlickrJson {
    JsonPhotos photos;

    public class JsonPhotos{
        private int page;
        private int pages;
        private int perpage;
        private int total;
        @SerializedName("photo")
        private List<GalleryItem> mPhotos;

        public int getPage() {
            return page;
        }

        public int getPages() {
            return pages;
        }

        public int getPerpage() {
            return perpage;
        }

        public int getTotal() {
            return total;
        }

        public List<GalleryItem> getPhoto() {
            return mPhotos;
        }
    }
}
```
> 获取图片数据 
``` java 
  items=flickrJson.photos.getPhoto();
```

* 下拉刷新
``` XML
<android.support.v4.widget.SwipeRefreshLayout
     xmlns:android="http://schemas.android.com/apk/res/android"
     android:id="@+id/swipe_refresh_layout"
     android:layout_height="match_parent"
     android:layout_width="match_parent">
	<RecyclerView
		......
		/>
</android.support.v4.widget.SwipeRefreshLayout>
```
``` java
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
 +            @Override
 +            public void onRefresh() {
 +                new Thread(new Runnable() {
 +                    @Override
 +                    public void run() {
 +                    refreshItems();
 +                    }
 +                }).start();
 +            }
 +        });
 ```

* 上拉加载更多
>实现RecyclerView.OnScrollListener
>并在onScrolled判断 是否拉到最低实现加载更多
``` java
+    private abstract class PhotoScrollListener extends RecyclerView.OnScrollListener{
 +        private LinearLayoutManager mLayoutManager;
 +        private final int MAX_PHOTOS=1000;
 +        private boolean loading = true;
 +        private int previousTotal = 0;
 +        private int firstVisibleItem, visibleItemCount, totalItemCount;
 +        private int currentPage = 1;
 +
 +        public PhotoScrollListener(LinearLayoutManager layoutManager) {
 +            this.mLayoutManager=layoutManager;
 +        }
 +        @Override
 +        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
 +            super.onScrolled(recyclerView, dx, dy);
 +            //已经滑出去的viewHolder
 +            visibleItemCount=recyclerView.getChildCount();
 +            //Returns the number of items in the adapter bound to the parent RecyclerView
 +            totalItemCount=mLayoutManager.getItemCount();
 +            firstVisibleItem=mLayoutManager.findFirstVisibleItemPosition();
 +
 +            if (loading) {
 +                if (totalItemCount > previousTotal) {
 +                    loading = false;
 +                    previousTotal = totalItemCount;
 +                }
 +            }
 +            if (!loading
 +                    && (totalItemCount - visibleItemCount) <= firstVisibleItem) {
 +
 +                final int per_page=Integer.parseInt(FlickrFetchr.PER_PAGE);
 +                if(MAX_PHOTOS/per_page>currentPage){
 +                    onLoadMore(++currentPage);
 +                    loading = true;
 +                }
 +
 +            }
 +        }
 ```

* 动态调整布局
> 实现ViewTreeObserver.OnGlobalLayoutListener
> 监听器方法和计算列数的onGlobalLayout()方法，然后使用addOnGlobalLayoutListener()把监听器添加给RecyclerView视图。
``` java
+        ViewTreeObserver treeObserver=v.getViewTreeObserver();
 +        treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
 +            public void onGlobalLayout() {
 +                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
 +                int w=mPhotoRecyclerView.getWidth();
 +                LinearLayoutManager layoutManager=new GridLayoutManager(getActivity(), w/350);
 +                mPhotoRecyclerView.setLayoutManager(layoutManager);
 +                mPhotoRecyclerView.addOnScrollListener(new PhotoScrollListener(layoutManager) {
 +                    @Override
 +                    public void onLoadMore(int page) {
 +                        loadMorePhoto(page);
 +                        Log.d(TAG, "onLoadMore: ");
 +                    }
 +                });
              }
          });
```

* 使用Okhttp
``` java
+    public byte[] getUrlBytesByOkHttp(String urlSpec)throws IOException{
 +        OkHttpClient client=new OkHttpClient();
 +        Request request=new Request.Builder()
 +                .url(urlSpec)
 +                .build();
 +        Response response=client.newCall(request).execute();
 +        return response.body().bytes();
      }
```
* 使用Glide
``` java
+        public void bindDrawable(byte[]  bitmapBytes) {
 +            Glide.with(PhotoGalleryFragment.this)
 +                    .load(bitmapBytes)
 +                    .asBitmap()
 +                    .placeholder(R.drawable.bill_up_close)
 +                    .error(R.drawable.error)
 +                    .into(mImageView);
 +        }
```

* 使用后退键浏览历史网页
>覆盖后退键方法Activity.onBackPressed()就能实现这个行为。在该方法内，再搭配使用
WebView的历史记录浏览方法（WebView.canGoBack()和WebView.goBack()）实现我们的浏览
逻辑。如果WebView 里有历史浏览记录， 就回到前一个历史网页， 否则调用super.
onBackPressed()方法回到PhotoPageActivity。

``` java
    @Override
    public void onBackPressed(){
        CallBacks callBacks=(CallBacks) getSupportFragmentManager().findFragmentById(R.id.activity_container);
        if(callBacks.doGoback()){
            super.onBackPressed();
        }
    }
	
    public interface  CallBacks{
        boolean doGoback();
    }
```
``` java
    //实现回调接口
    @Override
    public boolean doGoback() {
        if(mWebView.canGoBack()){
            mWebView.goBack();
            return false;
        }
        return true;
    }
```
* 非HTTP 链接支持
>加载URI前，先检查它的scheme，如果不是HTTP或HTTPS，就发送一个针对目标URI的Intent.ACTION_VIEW。
``` java
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
```
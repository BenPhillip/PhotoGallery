package com.example.gzp.photogallery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ben on 2017/3/1.
 */

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();
//        Intent i = PollService.newIntent(getActivity());
//        getActivity().startService(i);



/*
*创建并启动线程
* 这是一种保证线程就绪的处理方式，可以避免潜在竞争（尽管极少发生）。
*在getLooper（）中 :如果线程已经启动,一直等到Looper创建
* 调用getLooper()方法之前，没办法保证onLooperPrepared()方法已得到调用。
* Looper创建时会调用onLooperPrepared()
*
*/
        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                Log.d(TAG, "onThumbnailDownloaded: 准备加载bitmap");
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();//就绪态
        mThumbnailDownloader.getLooper();//确保已经是运行态，运行run()方法
        Log.i(TAG, "onCreate: Background thread started");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragement_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*退出线程*/
        mThumbnailDownloader.quit();
        Log.i(TAG, "onDestroy: Background thread destoryed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery,menu);

        /*从菜单中取出MenuItem并把它保存在searchItem中，
         然后使用getActionView()从这个变量取出SearchView对象
        */
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView=(SearchView)searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit: "+s);
                QueryPreferences.setStoredQuery(getActivity(),s);
                searchView.clearFocus();//清除焦点，隐藏键盘
                searchView.onActionViewCollapsed();//收起SearchView视图
                updateItems();
                return  true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                Log.i(TAG, "onOptionsItemSelected: "+shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }
    private void setAdapter() {
        if (isAdded()) {
            // Return true if the fragment is currently added to its activity.
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem>mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindDrawable(placeholder);
            //调用线程的queueThumbnail()方法，并传入放置图片的PhotoHolder和GalleryItem的URL
            mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;
        private ProgressDialog mProgressDialog;

        public FetchItemsTask(String query) {
            mQuery=query;
        }

        /**
         * 在执行doInBackground调用
         * 用来初始化进度框
         */
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
            mProgressDialog.setMessage(getString(R.string.progeress_dialog_message));
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if(mQuery==null)
                return new FlickrFetchr().fetchRecentPhotos();
            else
                return new FlickrFetchr().searchPhotos(mQuery);

        }

        /**
         * 在doInBackground后执行
         * 销毁进度框,更新RecyclerView.adapter 更新数据模型
         * @param items 下载好的图片信息容器
         */
        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems=items;
            setAdapter();
            mProgressDialog.dismiss();
        }
    }


}
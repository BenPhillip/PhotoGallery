package com.example.gzp.photogallery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.gzp.photogallery.Model.GalleryItem;
import com.example.gzp.photogallery.Util.FlickrFetchr;
import com.example.gzp.photogallery.Util.QueryPreferences;
import com.example.gzp.photogallery.Util.ThumbnailDownloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ben on 2017/3/1.
 */

public class PhotoGalleryFragment extends VisibleFragment  {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<GalleryItem> mItems = new ArrayList<>();
//    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private ProgressDialog mProgressDialog;
    private LinearLayoutManager layoutManager;


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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       final View v = inflater.inflate(R.layout.fragement_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        ViewTreeObserver treeObserver=v.getViewTreeObserver();
        treeObserver.addOnGlobalLayoutListener( new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int w=mPhotoRecyclerView.getWidth();
                layoutManager=new GridLayoutManager(getActivity(),w/350);
                mPhotoRecyclerView.setLayoutManager(layoutManager);
                mPhotoRecyclerView.addOnScrollListener(new PhotoScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int page) {
                        loadMorePhoto(page);
                    }
                });
            }

        });


        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                    refreshItems();
                    }
                }).start();
            }
        });
        setAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
//                updateItems();
                refreshItems();
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
        new FetchItemsTask(query){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setTitle(getString(R.string.progress_dialog_title));
                mProgressDialog.setMessage(getString(R.string.progeress_dialog_message));
                mProgressDialog.setCancelable(true);
                mProgressDialog.show();
            }

            @Override
            protected void onPostExecute(List<GalleryItem> items) {
                super.onPostExecute(items);
                mProgressDialog.dismiss();
            }
        }.execute(1);
    }

    private void loadMorePhoto(int page) {

        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query){
            @Override
            protected void onPostExecute(List<GalleryItem> items) {
                Log.d(TAG, "onPostExecute: ");
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();

            }
        }.execute(page);

    }
    private void setAdapter() {
        if (isAdded()) {
            // Return true if the fragment is currently added to its activity.
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));

        }
    }

    private void refreshItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query){
            @Override
            protected void onPostExecute(List<GalleryItem> items){
                super.onPostExecute(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
                mPhotoRecyclerView.addOnScrollListener(new PhotoScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int Page) {
                        loadMorePhoto(Page);
                    }
                });
            }
        }.execute(1);


    }

    private class PhotoHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener{
        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            mImageView.setOnClickListener(this);
        }


        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem=galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity
                    .newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(i);
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
            holder.bindGalleryItem(galleryItem);
//            Drawable placeholder = getResources().getDrawable(R.drawable.placeholder);
//            holder.bindDrawable(placeholder);
            //调用线程的queueThumbnail()方法，并传入放置图片的PhotoHolder和GalleryItem的URL
//            mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
            Glide.with(PhotoGalleryFragment.this)
                    .load(galleryItem.getUrl())
                    .asBitmap()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private abstract class PhotoScrollListener extends RecyclerView.OnScrollListener{
        private LinearLayoutManager mLayoutManager;
        private boolean loading = true;
        private int previousTotal = 0;
        private int firstVisibleItem, visibleItemCount, totalItemCount;
        private int currentPage = 1;

        public abstract void onLoadMore(int Page);

        public PhotoScrollListener(LinearLayoutManager layoutManager) {
            this.mLayoutManager=layoutManager;
        }
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            //已经滑出去的viewHolder
            visibleItemCount=recyclerView.getChildCount();
            //Returns the number of items in the adapter bound to the parent RecyclerView
            totalItemCount=mLayoutManager.getItemCount();
            firstVisibleItem=mLayoutManager.findFirstVisibleItemPosition();
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading
                    && (totalItemCount - visibleItemCount) <= firstVisibleItem) {
                currentPage++;
                onLoadMore(currentPage);
                loading = true;
            }
        }


    }


    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;


        public FetchItemsTask(String query) {
            mQuery=query;
        }


        @Override
        protected List<GalleryItem> doInBackground(Integer ... params) {
            if(mQuery==null)
                return new FlickrFetchr().fetchRecentPhotos(params[0]);
            else
                return new FlickrFetchr().searchPhotos(mQuery,params[0]);

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
        }

    }



}

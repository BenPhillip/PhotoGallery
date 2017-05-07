package com.example.gzp.photogallery.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Ben on 2017/3/30.
 */

public class FlickrJson {
    public JsonPhotos photos;

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

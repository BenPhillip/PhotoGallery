package com.example.gzp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by Ben on 2017/3/23.
 */

public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return new PhotoPageFragment().newInstance(getIntent().getData());
    }

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
}

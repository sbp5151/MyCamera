package com.jld.camera;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "AppCompatActivity";
    private FrameLayout mFrameLayout;
    private ImageButton mImageButton;
    private PhotoFragment mPhotoFragment;
    private VcrFragment mVcrFragment;
    private boolean isPhoto = true;//是否在拍照fragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFrameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        mImageButton = (ImageButton) findViewById(R.id.imageButton);
        mImageButton.setOnClickListener(this);
        mPhotoFragment = new PhotoFragment();
        mVcrFragment = new VcrFragment();
        mVcrFragment.setInterface(new VcrFragment.StateInterface() {
            @Override
            public void vcrState() {
                mImageButton.setVisibility(View.GONE);
            }
            @Override
            public void vcrStop() {
                mImageButton.setVisibility(View.VISIBLE);
            }
        });
        defaultFragment();
    }

    private void defaultFragment() {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.frameLayout, mPhotoFragment);
        transaction.commit();
    }

    @Override
    public void onClick(View view) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
//        transaction.setCustomAnimations(R.animator.fragment_in,R.animator.fragment_out);
        int id = view.getId();
        switch (id) {
            case R.id.imageButton:
                if (isPhoto) {
                    transaction.replace(R.id.frameLayout, mVcrFragment);
                    transaction.commit();
                    mImageButton.setImageResource(R.mipmap.photo);
                    isPhoto = false;
                } else {
                    transaction.replace(R.id.frameLayout, mPhotoFragment);
                    transaction.commit();
                    mImageButton.setImageResource(R.mipmap.vcr);
                    isPhoto = true;
                }
                break;
        }
    }
}

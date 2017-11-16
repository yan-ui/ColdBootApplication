package com.example.administrator.myapplication;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by Administrator on 2017/11/16.
 */

public class SplashFragment extends Fragment {

    public SplashFragment(){

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View ret  = inflater.inflate(R.layout.fragment,container,false);
        initView(ret);
        return ret;
    }

    private void initView(View ret) {
        if(ret != null){
            ImageView imageView = (ImageView) ret.findViewById(R.id.laucher_logo);
            playAnimator(imageView);
        }
    }

    private void playAnimator(ImageView imageView) {
        if(imageView != null){
            PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat("alpha",1f,0.7f,0.1f);
            ObjectAnimator.ofPropertyValuesHolder(imageView,pvhA).setDuration(2000).start();
        }
    }
}

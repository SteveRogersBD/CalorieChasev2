package com.example.caloriechase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class InfoSlideFragment extends Fragment {
    
    private static final String ARG_TEXT = "text";
    private static final String ARG_IMAGE = "image";
    
    public static InfoSlideFragment newInstance(String text, String imageName) {
        InfoSlideFragment fragment = new InfoSlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        args.putString(ARG_IMAGE, imageName);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info_slide, container, false);
        
        TextView textView = view.findViewById(R.id.tv_slide_text);
        ImageView imageView = view.findViewById(R.id.iv_slide_image);
        
        if (getArguments() != null) {
            String text = getArguments().getString(ARG_TEXT);
            String imageName = getArguments().getString(ARG_IMAGE);
            
            textView.setText(text);
            
            // Set image based on name
            int imageResource = getImageResource(imageName);
            if (imageResource != 0) {
                imageView.setImageResource(imageResource);
            }
        }
        
        return view;
    }
    
    private int getImageResource(String imageName) {
        switch (imageName) {
            case "running":
                return R.drawable.running;
            case "treasure":
                return R.drawable.treasure;
            case "map":
                return R.drawable.map;
            default:
                return R.drawable.ic_launcher_foreground;
        }
    }
}
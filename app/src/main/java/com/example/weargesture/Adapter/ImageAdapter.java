package com.example.weargesture.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.weargesture.R;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    ArrayList<String> title;
    ArrayList<Bitmap> imageID;
    public ImageAdapter(Context c, ArrayList<String> title, ArrayList<Bitmap> imageID ) {
        mContext = c;
        this.imageID = imageID;
        this.title = title;
    }
    @Override
    public int getCount() {
        return title.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View grid;
        if (view == null) {  // if it's not recycled, initialize some attributes
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(     Context.LAYOUT_INFLATER_SERVICE );
            grid = inflater.inflate(R.layout.list_single, viewGroup, false);
        } else {
            grid = (View) view;
        }

        TextView textView = (TextView) grid.findViewById(R.id.grid_text);
        ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
        textView.setText(title.get(i));
        imageView.setImageBitmap(imageID.get(i));

        return grid;
    }
}

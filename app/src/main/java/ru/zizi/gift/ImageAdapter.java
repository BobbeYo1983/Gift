package ru.zizi.gift;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private Point displaySize;

    // references to our images
    private	Integer[] images = { R.drawable.kat_1, R.drawable.kat_2,
            R.drawable.kat_3, R.drawable.kat_4};

    public ImageAdapter(Context context, Point displaySize) {
        this.context = context;
        this.displaySize = displaySize;
    }

    public int getCount() {
        return images.length;
    }

    public Object getItem(int position) {
        return images[position];
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageView imageView;

        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(context);
            int x = Math.round(displaySize.x/2)-1;
            int y = (int) (x * 1.777); // соотношение 16:9
            imageView.setLayoutParams(new GridView.LayoutParams(x, y));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(images[position]);
        return imageView;
    }


}

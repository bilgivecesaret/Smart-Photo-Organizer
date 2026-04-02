package com.example.smart_photo_organizer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.ListData;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class SpinnerAdapter extends ArrayAdapter<ListData> {
    private ArrayList<ListData> data;
    private Context context;
    private LayoutInflater inflater;

    public SpinnerAdapter(@NonNull Context context, int resource, ArrayList<ListData> data) {
        super(context, resource, data);
        this.context = context;
        this.data = data;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        return getCustomView(pos, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.spinner_layout, parent, false);
        }

        ImageView image = rowView.findViewById(R.id.left_pic);
        TextView country = rowView.findViewById(R.id.tv_listitem);


        // Get individual object from  ArrayList<ListData> and set ListView items
        ListData temp_data = data.get(position);
        image.setImageResource(temp_data.getImage());
        country.setText(temp_data.getCountry());

        return rowView;
    }
}
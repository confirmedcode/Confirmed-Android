/*
 * Copyright (C) 2012 Tobias Brunner
 * Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

package com.trytunnels.android.ui.adapter;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trytunnels.android.data.CountrySelectionModel;

import org.strongswan.android.R;

import java.util.List;

public class CountrySelectionAdapter extends ArrayAdapter<CountrySelectionModel> {

    Context context;
    int groupid;

    public CountrySelectionAdapter(Context context, int groupid, int id) {
        super(context, id);
        this.context = context;
        this.groupid = groupid;
    }

    /**
     * Set new data for this adapter.
     *
     * @param data the new data (null to clear)
     */
    public void setData(List<CountrySelectionModel> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(this.groupid, parent, false);
        }

        CountrySelectionModel item = getItem(position);
        ImageView img = (ImageView) view.findViewById(R.id.img);
        img.setImageResource(item.getImageId());

        TextView txt = (TextView) view.findViewById(R.id.txt);
        txt.setText(item.getText());

        view.setTag(item.getEndpoint());

        return view;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = getView(position, convertView, parent);
        // align items back to left and set bg color
        ((LinearLayout) view).setGravity(Gravity.NO_GRAVITY);
        view.setBackground(ContextCompat.getDrawable(context, R.drawable.tt_spinner_item_bg));
        WindowManager windowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int _width = size.x;

        view.setMinimumWidth(_width);
        return view;
    }
}

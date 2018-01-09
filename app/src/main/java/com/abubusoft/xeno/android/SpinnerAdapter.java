package com.abubusoft.xeno.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class SpinnerAdapter<E> extends ArrayAdapter<E> {
    public SpinnerAdapter(Context context, int layoutId) {
        this(context, layoutId, new ArrayList<E>());
    }

    public SpinnerAdapter(Context context, int layoutId, List<E>
            list) {
        super(context, layoutId, list);
        this.list = list;
        this.layoutId = layoutId;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected List<E> list;

    protected LayoutInflater inflater;

    protected int layoutId;

    protected abstract void bindView(View view, E item);

    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = inflater.inflate(layoutId, parent, false);
        bindView(itemView, list.get(position));

        return itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup
            parent) {
        return getView(position, convertView, parent);
    }
}
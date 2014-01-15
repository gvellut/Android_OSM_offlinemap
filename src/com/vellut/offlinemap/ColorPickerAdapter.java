package com.vellut.offlinemap;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ColorPickerAdapter extends ArrayAdapter<Integer> {

	private int resourceId;
	private Context context;

	public ColorPickerAdapter(Context context, int resource, Integer[] objects) {
		super(context, resource, objects);
		this.context = context;
		this.resourceId = resource;
	}

	public ColorPickerAdapter(Context context, int resource, List<Integer> objects) {
		super(context, resource, objects);
		this.context = context;
		this.resourceId = resource;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	private View getCustomView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resourceId, parent, false);
		}
		convertView.setBackgroundColor(getItem(position));
		return convertView;
	}

}

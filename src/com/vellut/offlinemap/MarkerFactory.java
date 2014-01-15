package com.vellut.offlinemap;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

public class MarkerFactory {
	private Map<Integer, Drawable> normalIcons;
	private Map<Integer, Drawable> starIcons;
	
	public int defaultSize;
	
	@SuppressLint("UseSparseArrays")
	public MarkerFactory(Context context) {
		normalIcons = new HashMap<Integer, Drawable>();
		starIcons = new HashMap<Integer, Drawable>();
		defaultSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				Utils.DEFAULT_MARKER_SIZE_DP, context.getResources().getDisplayMetrics());
	}
	
	public Drawable getNormalIcon(int color) {
		Drawable icon = normalIcons.get(color);
		if(icon == null) {
			// divide size by 2: CricleDrawable takes radius as input
			icon = new CircleDrawable(color, defaultSize / 2);
			normalIcons.put(color, icon);
		}
		return icon;
	}
	
	public Drawable getStarIcon(int color) {
		Drawable icon = starIcons.get(color);
		if(icon == null) {
			icon = new StarDrawable(color, (int) (defaultSize * 1.25));
			starIcons.put(color, icon);
		}
		return icon;
	}
	
	
}

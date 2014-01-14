package com.vellut.offlinemap;

import java.lang.reflect.Field;
import java.util.List;

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {
	public static final String TAG = "TOM";

	public static final String MAP_NAME = "Tokyo.map";
	public static final double INITIAL_LAT = 35.682817;
	public static final double INITIAL_LON = 139.74678;
	public static final byte INITIAL_ZOOM = 14;

	public static final String EXTRA_IS_NEW = "extra_is_new";
	public static final String EXTRA_TITLE = "extra_title";
	public static final String EXTRA_DESCRIPTION = "extra_description";
	public static final String EXTRA_COLOR = "extra_color";
	public static final String EXTRA_IS_BOOKMARK = "extra_is_bookmark";

	public static final String PREF_MAP_DATA = "pref_map_data";
	public static final String PREF_SAVED_LOCATIONS = "pref_saved_locations";

	public static final int CODE_MAP_ANNOTATION_EDIT = 150;

	public static Bitmap viewToBitmap(Context c, View view) {
		view.measure(MeasureSpec.getSize(view.getMeasuredWidth()),
				MeasureSpec.getSize(view.getMeasuredHeight()));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.setDrawingCacheEnabled(true);
		Drawable drawable = new BitmapDrawable(c.getResources(),
				android.graphics.Bitmap.createBitmap(view.getDrawingCache()));
		view.setDrawingCacheEnabled(false);
		return convertToBitmap(drawable);
	}

	public static Bitmap convertToBitmap(Drawable drawable) {
		Bitmap bitmap;
		if (drawable instanceof BitmapDrawable) {
			bitmap = ((BitmapDrawable) drawable).getBitmap();
		} else {
			int width = drawable.getIntrinsicWidth();
			int height = drawable.getIntrinsicHeight();
			bitmap = android.graphics.Bitmap.createBitmap(width, height,
					Config.ARGB_8888);
			android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

			Rect rect = drawable.getBounds();
			drawable.setBounds(0, 0, width, height);
			drawable.draw(canvas);
			drawable.setBounds(rect);
		}

		return bitmap;
	}
	

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static final void setBackground(View view, Drawable background) {
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			view.setBackground(background);
		} else {
			view.setBackgroundDrawable(background);
		}
	}
	
	public static OverlayItem getItem(ArrayItemizedOverlay overlays, int index) {
		try {
			Field itemsField = ArrayItemizedOverlay.class.getDeclaredField("overlayItems");
			itemsField.setAccessible(true);
			List<OverlayItem> items = (List<OverlayItem>) itemsField.get(overlays);
			return items.get(index);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static void showErrorToast(Context context, String text) {
		Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		toast.getView().setBackgroundColor(Color.RED);
		TextView textView = (TextView) toast.getView().findViewById(
				android.R.id.message);
		textView.setTextColor(Color.WHITE);
		toast.show();
	}
}
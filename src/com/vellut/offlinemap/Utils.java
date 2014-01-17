package com.vellut.offlinemap;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.List;

public class Utils {
	public static final String TAG = "TOM";

	public static final String MAP_NAME = "Tokyo.map";
	public static final double INITIAL_LAT = 35.682817;
	public static final double INITIAL_LON = 139.74678;
	public static final byte INITIAL_ZOOM = 14;
	public static final String OFM_FILE_NAME_FOR_EXPORT = "tokyo";


	public static final int DEFAULT_MARKER_COLOR = Color.CYAN;
	public static final int DEFAULT_MARKER_SIZE_DP = 24;
	public static final int LOCATION_UPDATE_TIMEOUT = 10000;

	public static final String EXTRA_IS_NEW = "extra_is_new";
	public static final String EXTRA_TITLE = "extra_title";
	public static final String EXTRA_DESCRIPTION = "extra_description";
	public static final String EXTRA_COLOR = "extra_color";
	public static final String EXTRA_IS_BOOKMARK = "extra_is_bookmark";
	public static final String EXTRA_START_PATH = "start_path";
	public static final String EXTRA_EXTENSION_FILTER = "extension_filter";
	public static final String EXTRA_CHOOSE_DIRECTORY_ONLY = "choose_directory";
	public static final String EXTRA_FILE_PATH = "file_path";
	public static final String EXTRA_EDITING_CONTEXT_INDEX = "editing_context_index";
	public static final String EXTRA_EDITING_CONTEXT_IS_CREATION = "editing_context_is_creation";

	public static final String PREF_MAP_DATA = "pref_map_data";
	public static final String PREF_SAVED_LOCATIONS = "pref_saved_locations";
	public static final String PREF_IS_FIRST_TIME_RUN = "pref_is_first_time_run";

	public static final int CODE_MAP_ANNOTATION_EDIT_REQUEST = 150;
	public static final int CODE_CONNECTION_FAILURE_RESOLUTION_REQUEST = 151;
	public static final int CODE_IMPORT_FILE_EXPLORER_REQUEST = 152;
	public static final int CODE_EXPORT_FILE_EXPLORER_REQUEST = 153;

	public static final String OFM_FILE_EXTENSION = "ofm";
	public static final Integer[] MAP_ANNOTATION_COLORS = { Color.BLUE, Color.RED,
			Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA };

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
			Field itemsField = ArrayItemizedOverlay.class
					.getDeclaredField("overlayItems");
			itemsField.setAccessible(true);
			List<OverlayItem> items = (List<OverlayItem>) itemsField
					.get(overlays);
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

	public static String getExtension(String fileName) {
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			return fileName.substring(i+1);
		}

		return "";
	}

	public static String convertStreamToString(InputStream is)
			throws IOException {

		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	public static String pathRelativeTo(String filePath, String relativeToFolderPath) {
		if(relativeToFolderPath.endsWith("/")) {
			relativeToFolderPath = relativeToFolderPath.substring(0, relativeToFolderPath.length() - 1);
		}

		if(filePath.startsWith(relativeToFolderPath)) {
			String path = filePath.substring(relativeToFolderPath.length());
			if(path.startsWith("/")) {
				path = path.substring(1);
			}
			return path;
		}

		return filePath;
	}

	public static void showErrorToast(Context context, String text) {
		Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		toast.getView().setBackgroundColor(Color.RED);
		TextView textView = (TextView) toast.getView().findViewById(
				android.R.id.message);
		textView.setTextColor(Color.WHITE);
		toast.show();
	}
	
	public static void d(String text) {
		Log.d(TAG, text);
	}


	public static void e(String text, Throwable t) {
		Log.e(TAG, text, t);
	}
	

}

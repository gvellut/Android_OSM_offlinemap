package com.vellut.offlinemap;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class CurrentPositionDrawable extends Drawable {

	private boolean showAccuracy;
	private float accuracy;
	private Paint accuracyPaintSurface;
	private Paint accuracyPaintBorder;
	private Drawable currentPositionMarker;
	private MapView mapView;

	// Mapview is necessary to get the conversion factor from accuracy to pixels
	public CurrentPositionDrawable(MapView mapView, Drawable currentPositionMarker,
			boolean showAccuracy, int accuracyColor) {
		this.currentPositionMarker = currentPositionMarker;
		this.showAccuracy = showAccuracy;
		this.mapView = mapView;

		// Center the marker
		ItemizedOverlay.boundCenter(currentPositionMarker);
		setBounds(currentPositionMarker.copyBounds());

		accuracyPaintSurface = new Paint();
		accuracyPaintSurface.setStyle(Style.FILL);
		accuracyPaintSurface.setColor(accuracyColor);
		accuracyPaintSurface.setAlpha(0x33);
		accuracyPaintBorder = new Paint();
		accuracyPaintBorder.setStyle(Style.STROKE);
		accuracyPaintBorder.setStrokeWidth(2);
		accuracyPaintBorder.setColor(accuracyColor);
	}

	@Override
	public void draw(Canvas canvas) {
		int x = getBounds().centerX();
		int y = getBounds().centerY();

		if (showAccuracy && accuracy != 0) {
			int pixelAccuracy = getPixelAccuracy();
			canvas.drawCircle(x, y, pixelAccuracy, accuracyPaintSurface);
			canvas.drawCircle(x, y, pixelAccuracy, accuracyPaintBorder);
		}

		Rect savBounds = currentPositionMarker.copyBounds();
		int left = savBounds.left + x;
		int right = savBounds.right + x;
		int top = savBounds.top + y;
		int bottom = savBounds.bottom + y;
		currentPositionMarker.setBounds(left, top, right, bottom);
		currentPositionMarker.draw(canvas);
		// restore
		currentPositionMarker.setBounds(savBounds);
	}

	@Override
	public int getIntrinsicWidth() {
		if (showAccuracy) {
			int pixelAccuracy = getPixelAccuracy();
			return Math
					.max(currentPositionMarker.getIntrinsicWidth(), pixelAccuracy);
		} else {
			return currentPositionMarker.getIntrinsicWidth();
		}
	}

	@Override
	public int getIntrinsicHeight() {
		if (showAccuracy) {
			int pixelAccuracy = getPixelAccuracy();
			return Math.max(currentPositionMarker.getIntrinsicHeight(),
					pixelAccuracy);
		} else {
			return currentPositionMarker.getIntrinsicHeight();
		}
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSPARENT;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
		if(showAccuracy) {
			setBounds(getIntrinsicWidth() / -2, getIntrinsicHeight() / -2,
				getIntrinsicWidth() / 2, getIntrinsicHeight() / 2);
		}
	}
	
	private int getPixelAccuracy() {
		Projection projection = mapView.getProjection();
		int pixelAccuracy = (int) projection.metersToPixels(accuracy, mapView
				.getMapPosition().getZoomLevel());
		return pixelAccuracy;
	}

	@Override
	public void setAlpha(int alpha) {
		currentPositionMarker.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		currentPositionMarker.setColorFilter(cf);
	}

}

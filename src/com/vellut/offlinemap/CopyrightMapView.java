package com.vellut.offlinemap;

import org.mapsforge.android.maps.MapView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;

import com.vellut.offlinemap.kansai.R;

public class CopyrightMapView extends MapView {

	private Paint copyrightPaint;

	public CopyrightMapView(Context context) {
		super(context);

		setupCopyrightText();
	}

	private void setupCopyrightText() {
		this.copyrightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.copyrightPaint.setTypeface(Typeface.DEFAULT);
		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				12, getResources().getDisplayMetrics());
		this.copyrightPaint.setTextSize(size);
		this.copyrightPaint.setColor(Color.BLACK);
		this.copyrightPaint.setAlpha(128);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		canvas.drawText(
				getResources().getString(R.string.osm_copyright),
				5, getHeight() - 5, this.copyrightPaint);
	}

}

package com.vellut.offlinemap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class StarDrawable extends Drawable {

	private Paint paint, paintBorder;
	private Path path, translatedPath;
	private int size;

	private static final int STAR_OPP_ANGLE = 72;
	private static final int STAR_ANGLE_HALF = 18;

	public StarDrawable(int color, int size) {
		this.size = size;
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(color);

		paintBorder = new Paint();
		paintBorder.setStyle(Style.STROKE);
		paintBorder.setColor(Color.WHITE);
		paintBorder.setStrokeWidth(2);
		paintBorder.setAntiAlias(true);

		int halfSize = size / 2;
		setBounds(-halfSize, -halfSize, halfSize, halfSize);
		computePath();
		translatedPath = new Path();
	}

	private void computePath() {
		Rect bounds = getBounds();
		float width = bounds.width();
		float height = bounds.height();
		float[] vertices = {0.0f, 0.375f, 0.375f, 0.375f, 0.5f, 0.0f, 0.625f, 0.375f, 1.0f, 0.375f,
				0.6875f, 0.625f, 0.8125f, 1.0f, 0.5f, 0.75f, 0.1875f, 1.0f, 0.3125f, 0.625f};

		path = new Path();
		// draw pentagram
		path.moveTo(vertices[0] * width, vertices[1] * height);
		for (int i = 2; i < vertices.length; i += 2) {
			path.lineTo(vertices[i] * width, vertices[i + 1] * height);
		}
		path.close();
	}

	@Override
	public void draw(Canvas canvas) {
		Matrix matrix = new Matrix();
		matrix.setTranslate(getBounds().left, getBounds()
				.top);
		path.transform(matrix, translatedPath);
		canvas.drawPath(translatedPath, paint);
		canvas.drawPath(translatedPath, paintBorder);
	}

	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paint.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public int getIntrinsicHeight() {
		return size;
	}

	@Override
	public int getIntrinsicWidth() {
		return size;
	}
}

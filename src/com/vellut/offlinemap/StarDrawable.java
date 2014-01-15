package com.vellut.offlinemap;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class StarDrawable extends Drawable {

	private Paint paint;
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

		int halfSize = size / 2;
		setBounds(-halfSize, -halfSize, halfSize, halfSize);
		computePath();
		translatedPath = new Path();
	}

	private void computePath() {
		Rect bounds = getBounds();
		int minDim = Math.min(bounds.width(), bounds.height());

		double bigHypot = (minDim / Math.cos(Math.toRadians(STAR_ANGLE_HALF)));
		double bigB = minDim;
		double bigA = Math.tan(Math.toRadians(18)) * bigB;

		double littleHypot = bigHypot
				/ (2 + Math.cos(Math.toRadians(STAR_OPP_ANGLE)) + Math.cos(Math
						.toRadians(STAR_OPP_ANGLE)));
		double littleA = Math.cos(Math.toRadians(STAR_OPP_ANGLE)) * littleHypot;
		double littleB = Math.sin(Math.toRadians(STAR_OPP_ANGLE)) * littleHypot;

		int topXPoint = bounds.left + bounds.width() / 2;
		int topYPoint = bounds.top;

		path = new Path();
		// draw pentagram
		// start at the top point
		path.moveTo(topXPoint, topYPoint);

		// top to bottom right point
		path.lineTo((int) (topXPoint + bigA), (int) (topYPoint + bigB));

		// bottom right to middle left point
		path.lineTo((int) (topXPoint - littleA - littleB),
				(int) (topYPoint + littleB));

		// middle left to middle right point
		path.lineTo((int) (topXPoint + littleA + littleB),
				(int) (topYPoint + littleB));

		// middle right to bottom left point
		path.lineTo((int) (topXPoint - bigA), (int) (topYPoint + bigB));

		// bottom left to top point
		path.lineTo(topXPoint, topYPoint);
		path.close();
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);

		Utils.d(bounds.toShortString());
	}

	@Override
	public void draw(Canvas canvas) {
		Matrix matrix = new Matrix();
		matrix.setTranslate(getBounds().exactCenterX(), getBounds()
				.exactCenterY());
		path.transform(matrix, translatedPath);
		canvas.drawPath(translatedPath, paint);
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

package com.vellut.tokyoofflinemap;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class CircleDrawable extends Drawable {

	private float radius;
	private Paint paint;
	
	public CircleDrawable(int color, float radius) {
		this.radius = radius;
		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(color);
	}
	
	@Override
	public void draw(Canvas canvas) {
		float cx = (getBounds().right + getBounds().left) / 2;
		float cy = (getBounds().top + getBounds().bottom) / 2;
		canvas.drawCircle(cx, cy, radius, paint);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int alpha) {
		
	}

	@Override
	public void setColorFilter(ColorFilter cf) {

	}

}

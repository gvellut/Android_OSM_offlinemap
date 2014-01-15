package com.vellut.offlinemap;


public class MapAnnotation {
	
	public double latitude, longitude;
	public String title, description;
	public int color;
	public boolean isBookmarked;
	
	public MapAnnotation() {
	}
	
	public MapAnnotation(double latitude, double longitude, int color) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.color = color;
	}
	
}

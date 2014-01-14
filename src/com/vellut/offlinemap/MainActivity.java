package com.vellut.offlinemap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.MercatorProjection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vellut.offlinemap.tokyo.R;

public class MainActivity extends MapActivity implements ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener {

	private File mapFile;
	private LocationClient locationClient;
	private MapView mapView;
	private boolean zoomToCurrentPositionOnConnected = false;

	private MapData mapData;
	private ArrayList<MapAnnotation> mapAnnotations;

	private ArrayItemizedOverlay currentPositionOverlay;
	private ArrayItemizedOverlay mapAnnotationsOverlay;
	private ArrayItemizedOverlay bubbleTextOverlay;

	private OverlayItem editedOverlayItem;
	private MapAnnotation editedMapAnnotation;
	private boolean isCreation;

	private MapAnnotation currentMapAnnotationForBubble;
	private GestureDetector gestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationClient = new LocationClient(this, this, this);
		restorePreferences();

		init();
		createMapView();
		configureMapView();
	}

	@Override
	public void onStart() {
		super.onStart();

		locationClient.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();

		GeoPoint center = mapView.getMapPosition().getMapCenter();
		mapData.mapLatitude = center.getLatitude();
		mapData.mapLongitude = center.getLongitude();
		mapData.mapZoom = mapView.getMapPosition().getZoomLevel();

		savePreferences();
	}

	@Override
	protected void onStop() {
		super.onStop();

		locationClient.disconnect();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_current_location:
			zoomToCurrentPosition();
			return true;
		case R.id.action_initial_view:
			zoomToInitialPosition();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void zoomToCurrentPosition() {
		if (isGooglePlayServicesConnected()) {
			LocationRequest lr = new LocationRequest();
			lr.setNumUpdates(1).setFastestInterval(1).setInterval(1)
					.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			locationClient.requestLocationUpdates(lr, this);
		} else {
			zoomToCurrentPositionOnConnected = true;
		}
	}

	private void zoomToInitialPosition() {
		mapView.getController().setCenter(
				new GeoPoint(Utils.INITIAL_LAT, Utils.INITIAL_LON));
		mapView.getController().setZoom(Utils.INITIAL_ZOOM);
	}

	private void init() {
		Log.d(Utils.TAG, getApplicationContext().getPackageName());
		String mapName = Utils.MAP_NAME;
		mapFile = new File(getExternalCacheDir(), mapName);
		if (!mapFile.exists() || hasNewVersion(mapFile)) {
			Log.d(Utils.TAG, "Installing map");
			createFileFromInputStream(mapFile, mapName);
		}

		gestureDetector = new GestureDetector(this,
				new SimpleOnGestureListener() {
					public void onLongPress(MotionEvent motionEvent) {
						addMarkerFromLongTap(motionEvent);
					};
				});

		restorePreferences();
	}

	public void restorePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		String sMapData = settings.getString(Utils.PREF_MAP_DATA, null);
		if (sMapData != null) {
			try {
				Type type = new TypeToken<MapData>() {
				}.getType();
				mapData = (MapData) deserializeFromString(sMapData, type);
			} catch (Exception ex) {
				Log.e(Utils.TAG, "Error getting saved MapData", ex);
			}
		} else {
			mapData = new MapData();
		}

		String sSavedLocations = settings.getString(Utils.PREF_SAVED_LOCATIONS,
				null);
		if (sMapData != null) {
			try {
				Type type = new TypeToken<ArrayList<MapAnnotation>>() {
				}.getType();
				mapAnnotations = (ArrayList<MapAnnotation>) deserializeFromString(
						sSavedLocations, type);
			} catch (Exception ex) {
				Log.e(Utils.TAG, "Error getting saved SavedLocations", ex);
			}
		} else {
			mapAnnotations = new ArrayList<MapAnnotation>();
		}
	}

	public void savePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();

		Type type = new TypeToken<MapData>() {
		}.getType();
		editor.putString(Utils.PREF_MAP_DATA, serializeToString(mapData, type));

		type = new TypeToken<ArrayList<MapAnnotation>>() {
		}.getType();
		editor.putString(Utils.PREF_SAVED_LOCATIONS,
				serializeToString(mapAnnotations, type));

		editor.apply();
	}

	private String serializeToString(Object object, Type type) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(object);
	}

	private Object deserializeFromString(String json, Type type) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.fromJson(json, type);
	}

	private void createMapView() {
		mapView = new CopyrightMapView(this) {
			@Override
			public boolean onTouchEvent(MotionEvent motionEvent) {
				if (gestureDetector.onTouchEvent(motionEvent)) {
					return true;
				}
				if (motionEvent.getAction() != MotionEvent.ACTION_MOVE) {
					gestureDetector.setIsLongpressEnabled(true);
				}

				return super.onTouchEvent(motionEvent);
			}
		};

		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(false);
		mapView.setMapFile(mapFile);
		setContentView(mapView);
	}

	private void configureMapView() {
		if (mapData.mapZoom == 0) {
			// Zoom on Tokyo
			zoomToInitialPosition();
		} else {
			// mapData is valid: Zoom on previous position
			mapView.getController().setCenter(
					new GeoPoint(mapData.mapLatitude, mapData.mapLongitude));
			mapView.getController().setZoom(mapData.mapZoom);
		}

		Drawable currentPositionMarker = getResources().getDrawable(
				R.drawable.ic_maps_indicator_current_position_anim1);
		currentPositionOverlay = new ArrayItemizedOverlay(currentPositionMarker);
		mapView.getOverlays().add(currentPositionOverlay);

		int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				12, getResources().getDisplayMetrics());
		CircleDrawable circle = new CircleDrawable(Color.RED, size);
		mapAnnotationsOverlay = new ArrayItemizedOverlay(circle, false) {
			@Override
			protected boolean onTap(int index) {
				MainActivity.this.onMapAnnotationTap(index);
				return true;
			}

			@Override
			public boolean onTap(GeoPoint geoPoint, MapView mapView) {
				if(editedOverlayItem != null) {
					return true;
				}
				
				boolean hasHit = checkItemHit(geoPoint, mapView, EventType.TAP);
				/*if(!hasHit) {
					// close balloon: mapAnnotationsOv only layer
					// that can handle taps => OK to close balloon
					// if no hit
					MainActivity.this.clearBubble();
				}*/
				
				return hasHit;
			}

		};
		mapView.getOverlays().add(mapAnnotationsOverlay);

		// FIXME remove
		mapAnnotations.clear();

		// FIXME dummy annotation
		MapAnnotation mapAnnotation1 = new MapAnnotation();
		mapAnnotation1.latitude = Utils.INITIAL_LAT;
		mapAnnotation1.longitude = Utils.INITIAL_LON;
		mapAnnotation1.title = "Annotation 1";
		mapAnnotation1.description = "Description for Annot1";
		mapAnnotations.add(mapAnnotation1);

		for (MapAnnotation mapAnnotation : mapAnnotations) {
			addMarker(mapAnnotation);
		}

	}

	protected void addMarkerFromLongTap(MotionEvent motionEvent) {
		MapViewPosition mapPosition = mapView.getMapPosition();
		GeoPoint geoPoint = mapPosition.getMapCenter();

		float x = motionEvent.getX();
		float y = motionEvent.getY();

		double pixelX = MercatorProjection.longitudeToPixelX(
				geoPoint.getLongitude(), mapPosition.getZoomLevel());
		double pixelY = MercatorProjection.latitudeToPixelY(
				geoPoint.getLatitude(), mapPosition.getZoomLevel());

		pixelX -= mapView.getWidth() >> 1;
		pixelY -= mapView.getHeight() >> 1;

		double latitude = MercatorProjection.pixelYToLatitude(pixelY + y,
				mapPosition.getZoomLevel());
		double longitude = MercatorProjection.pixelXToLongitude(pixelX + x,
				mapPosition.getZoomLevel());

		editedMapAnnotation = new MapAnnotation();
		editedMapAnnotation.latitude = latitude;
		editedMapAnnotation.longitude = longitude;
		mapAnnotations.add(editedMapAnnotation);
		editedOverlayItem = addMarker(editedMapAnnotation);

		isCreation = true;

		Intent intent = new Intent();
		intent.setClass(this, MapAnnotationEditActivity.class);
		intent.putExtra(Utils.EXTRA_IS_NEW, true);
		startActivityForResult(intent, Utils.CODE_MAP_ANNOTATION_EDIT);
	}

	private OverlayItem addMarker(MapAnnotation mapAnnotation) {
		OverlayItem overlay = new OverlayItem(new GeoPoint(
				mapAnnotation.latitude, mapAnnotation.longitude),
				mapAnnotation.title, mapAnnotation.description);
		mapAnnotationsOverlay.addItem(overlay);
		return overlay;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Utils.CODE_MAP_ANNOTATION_EDIT:
			if (resultCode == RESULT_OK) {
				editedMapAnnotation.title = data.getExtras().getString(
						Utils.EXTRA_TITLE);
				editedMapAnnotation.description = data.getExtras().getString(
						Utils.EXTRA_DESCRIPTION);
			} else {
				if (isCreation) {
					mapAnnotations.remove(editedMapAnnotation);
					mapAnnotationsOverlay.removeItem(editedOverlayItem);
				}
			}

			editedOverlayItem = null;
			editedMapAnnotation = null;
			isCreation = false;

			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	protected void onMapAnnotationTap(int index) {
		MapAnnotation mapAnnotation = mapAnnotations.get(index);
		if(currentMapAnnotationForBubble == mapAnnotation) {
			// toggle
			clearBubble();
			return;
		}
		
		addBubble(mapAnnotation);
	}
	
	private void addBubble(MapAnnotation mapAnnotation) {
		currentMapAnnotationForBubble = mapAnnotation;

		TextView bubbleView = new TextView(this);
		Utils.setBackground(bubbleView,
				getResources()
						.getDrawable(R.drawable.balloon_overlay_unfocused));
		bubbleView.setGravity(Gravity.CENTER);
		bubbleView.setMaxEms(20);
		bubbleView.setTextSize(15);
		bubbleView.setTextColor(Color.BLACK);
		bubbleView.setText(mapAnnotation.title);
		Bitmap bitmap = Utils.viewToBitmap(this, bubbleView);
		BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);

		GeoPoint gp = new GeoPoint(mapAnnotation.latitude,
				mapAnnotation.longitude);

		if(bubbleTextOverlay != null) {
			mapView.getOverlays().remove(bubbleTextOverlay);
		}
		bubbleTextOverlay = new ArrayItemizedOverlay(bd);
		OverlayItem bubble = new OverlayItem();
		bubble.setPoint(gp);
		bubbleTextOverlay.addItem(bubble);
		mapView.getOverlays().add(bubbleTextOverlay);
	}
	
	private void clearBubble() {
		if(bubbleTextOverlay != null) {
			mapView.getOverlays().remove(bubbleTextOverlay);
			currentMapAnnotationForBubble = null;
		}
	}

	private File createFileFromInputStream(File f, String mapName) {
		try {
			AssetManager am = getAssets();
			InputStream inputStream = am.open(mapName);

			OutputStream outputStream = new FileOutputStream(f);
			byte buffer[] = new byte[1024];
			int length = 0;

			while ((length = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, length);
			}

			outputStream.close();
			inputStream.close();

			return f;
		} catch (IOException e) {
			// Logging exception
		}

		return null;
	}

	private boolean hasNewVersion(File f) {
		try {
			PackageManager pm = getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(
					getApplicationContext().getPackageName(), 0);
			String appFile = appInfo.sourceDir;
			long lastInstalled = new File(appFile).lastModified();
			return (f.lastModified() < lastInstalled);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean isGooglePlayServicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			Log.d(Utils.TAG, "Google Play services is available.");
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {

	}

	@Override
	public void onConnected(Bundle arg0) {
		if (zoomToCurrentPositionOnConnected) {
			zoomToCurrentPositionOnConnected = false;
			zoomToCurrentPosition();
		}
	}

	@Override
	public void onDisconnected() {

	}

	@Override
	public void onLocationChanged(Location location) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		GeoPoint position = new GeoPoint(lat, lon);
		byte zoom = mapView.getMapPosition().getZoomLevel();
		if (zoom < 15) {
			zoom = 15;
		}
		mapView.getController().setCenter(position);
		mapView.getController().setZoom(zoom);

		currentPositionOverlay.clear();
		OverlayItem overlay = new OverlayItem();
		overlay.setPoint(position);
		currentPositionOverlay.addItem(overlay);
	}

}

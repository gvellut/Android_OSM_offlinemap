package com.vellut.tokyoofflinemap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

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

public class MainActivity extends MapActivity implements ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener {
	private static final String TAG = "TOM";
	private static final String PREF_MAP_DATA = "pref_map_data";
	private static final String PREF_SAVED_LOCATIONS = "pref_saved_locations";

	private static final double INITIAL_LAT = 35.682817;
	private static final double INITIAL_LON = 139.74678;
	private static final byte INITIAL_ZOOM = 14;

	private File mapFile;
	private LocationClient locationClient;
	private MapView mapView;
	private boolean zoomToCurrentPositionOnConnected = false;

	private MapData mapData;
	private ArrayList<SavedLocation> savedLocations;
	private ArrayItemizedOverlay currentPositionOverlay;
	private ArrayItemizedOverlay savedPositionsOverlay;

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
		case R.id.action_tokyo_view:
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
				new GeoPoint(INITIAL_LAT, INITIAL_LON));
		mapView.getController().setZoom(INITIAL_ZOOM);
	}

	private void init() {
		Log.d(TAG, getApplicationContext().getPackageName());
		String mapName = "Tokyo.map";
		mapFile = new File(getExternalCacheDir(), mapName);
		if (!mapFile.exists() || hasNewVersion(mapFile)) {
			Log.d(TAG, "Install map");
			createFileFromInputStream(mapFile, mapName);
		}

		restorePreferences();
	}

	public void restorePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		String sMapData = settings.getString(PREF_MAP_DATA, null);
		if (sMapData != null) {
			try {
				Type type = new TypeToken<MapData>() {
				}.getType();
				mapData = (MapData) deserializeFromString(sMapData, type);
			} catch (Exception ex) {
				Log.e(TAG, "Error getting saved MapData", ex);
			}
		} else {
			mapData = new MapData();
		}

		String sSavedLocations = settings.getString(PREF_SAVED_LOCATIONS, null);
		if (sMapData != null) {
			try {
				Type type = new TypeToken<ArrayList<SavedLocation>>() {
				}.getType();
				savedLocations = (ArrayList<SavedLocation>) deserializeFromString(
						sSavedLocations, type);
			} catch (Exception ex) {
				Log.e(TAG, "Error getting saved SavedLocations", ex);
			}
		} else {
			savedLocations = new ArrayList<SavedLocation>();
		}
	}

	public void savePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();

		Type type = new TypeToken<MapData>() {
		}.getType();
		editor.putString(PREF_MAP_DATA, serializeToString(mapData, type));
		
		type = new TypeToken<ArrayList<SavedLocation>>() {
		}.getType();
		editor.putString(PREF_SAVED_LOCATIONS, serializeToString(savedLocations, type));

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
		mapView = new CopyrightMapView(this);
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

		CircleDrawable circle = new CircleDrawable(Color.RED, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				24, getResources().getDisplayMetrics()));
		savedPositionsOverlay = new ArrayItemizedOverlay(circle);
		mapView.getOverlays().add(savedPositionsOverlay);
		
		// FIXME remove test
		OverlayItem oi = new OverlayItem(new GeoPoint(INITIAL_LAT, INITIAL_LON), "he", "hell");
		savedPositionsOverlay.addItem(oi);
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
			Log.d(TAG, "Google Play services is available.");
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

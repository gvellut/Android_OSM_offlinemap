package com.vellut.offlinemap;

import static com.vellut.offlinemap.Utils.d;

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender;
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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
	private boolean zoomToCurrentPositionOnConnected;
	private MarkerFactory markerFactory;

	private MapData mapData;
	private ArrayList<MapAnnotation> mapAnnotations;

	private ArrayItemizedOverlay currentPositionOverlay;
	private ArrayItemizedOverlay mapAnnotationsOverlay;
	private ArrayItemizedOverlay bubbleTextOverlay;

	private OverlayItem overlayItemInContext;
	private MapAnnotation mapAnnotationInContext;
	private boolean isCreation;

	private MapAnnotation currentMapAnnotationForBubble;

	private boolean isFirstTimeRun;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		locationClient = new LocationClient(this, this, this);
		restorePreferences();

		init();
		configureUI();
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
		getMenuInflater().inflate(R.menu.menu_main, menu);
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_context, menu);
		if (mapAnnotationInContext.isBookmarked) {
			menu.getItem(1).setTitle(
					getString(R.string.action_unstar_map_annotation));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_edit_map_annotation:
			editMapAnnotation();
			return true;
		case R.id.action_star_map_annotation:
			starMapAnnotation();
			return true;
		case R.id.action_delete_map_annotation:
			deleteMapAnnotation(true);
			return true;
		default:
			return super.onContextItemSelected(item);
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
		d(getApplicationContext().getPackageName());
		String mapName = Utils.MAP_NAME;
		mapFile = new File(getExternalCacheDir(), mapName);
		if (!mapFile.exists() || hasNewVersion(mapFile)) {
			d("Installing map");
			createFileFromInputStream(mapFile, mapName);
		}

		markerFactory = new MarkerFactory(this);

		restorePreferences();
	}

	@SuppressWarnings("unchecked")
	public void restorePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		String sMapData = settings.getString(Utils.PREF_MAP_DATA, null);
		if (sMapData != null) {
			try {
				mapData = (MapData) deserializeFromString(sMapData,
						MapData.class);
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

		isFirstTimeRun = settings
				.getBoolean(Utils.PREF_IS_FIRST_TIME_RUN, true);
	}

	public void savePreferences() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();

		editor.putString(Utils.PREF_MAP_DATA,
				serializeToString(mapData, MapData.class));

		Type type = new TypeToken<ArrayList<MapAnnotation>>() {
		}.getType();
		editor.putString(Utils.PREF_SAVED_LOCATIONS,
				serializeToString(mapAnnotations, type));

		editor.putBoolean(Utils.PREF_IS_FIRST_TIME_RUN, isFirstTimeRun);

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

	private void configureUI() {
		createMapView();
		registerForContextMenu(mapView);

		if (isFirstTimeRun) {
			isFirstTimeRun = false;

			showWelcomeDialog();

			// Zoom on Tokyo
			zoomToInitialPosition();
		} else {
			if (mapData.mapZoom == 0) {
				// just in case...
				zoomToInitialPosition();
			} else {
				// mapData is valid: Zoom on previous position
				GeoPoint center = new GeoPoint(mapData.mapLatitude,
						mapData.mapLongitude);
				mapView.getController().setCenter(center);
				mapView.getController().setZoom(mapData.mapZoom);
			}
		}

		Drawable circle = markerFactory
				.getNormalMarker(Utils.DEFAULT_MARKER_COLOR);
		mapAnnotationsOverlay = createMapAnnotationsOverlay(circle, false);
		mapView.getOverlays().add(mapAnnotationsOverlay);

		Drawable currentPositionMarker = getResources().getDrawable(
				R.drawable.ic_maps_indicator_current_position_anim1);
		currentPositionOverlay = new ArrayItemizedOverlay(currentPositionMarker);
		mapView.getOverlays().add(currentPositionOverlay);

		// FIXME remove
		 mapAnnotations.clear();
		 // FIXME dummy annotation 
		 MapAnnotation mapAnnotation1 = new
		 MapAnnotation(Utils.INITIAL_LAT, Utils.INITIAL_LON, Color.YELLOW);
		 mapAnnotation1.title = "Annotation 1"; mapAnnotation1.description =
		 "Description for Annot1"; mapAnnotation1.isBookmarked = true;
		 mapAnnotations.add(mapAnnotation1);
		

		for (MapAnnotation mapAnnotation : mapAnnotations) {
			addMarker(mapAnnotation);
		}
	}

	private ArrayItemizedOverlay createMapAnnotationsOverlay(
			Drawable defaultMarker, boolean recenter) {
		return new ArrayItemizedOverlay(defaultMarker, recenter) {
			@Override
			protected boolean onTap(int index) {
				MainActivity.this.showBubbleForMapAnnotationInteractive(index);
				return true;
			}

			@Override
			protected boolean onLongPress(int index) {
				MainActivity.this.showContextMenuForMapAnnotation(index);
				return true;
			}

			@Override
			public boolean onTap(GeoPoint geoPoint, MapView mapView) {
				// this will call the onTap(int) method above if overlay
				// is hit
				return checkItemHit(geoPoint, mapView, EventType.TAP);
			}

			@Override
			public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
				boolean hasHit = checkItemHit(geoPoint, mapView,
						EventType.LONG_PRESS);
				if (!hasHit) {
					addMapAnnotationInteractive(geoPoint);
				}
				// Note: No other layer can handle double tap
				return true;
			}

		};
	}

	private void addMapAnnotationInteractive(GeoPoint geoPoint) {
		mapAnnotationInContext = new MapAnnotation(geoPoint.getLatitude(),
				geoPoint.getLongitude(), Utils.DEFAULT_MARKER_COLOR);
		mapAnnotations.add(mapAnnotationInContext);
		overlayItemInContext = addMarker(mapAnnotationInContext);

		isCreation = true;

		launchMapAnnotationEditActivity();
	}

	private OverlayItem addMarker(MapAnnotation mapAnnotation) {
		OverlayItem overlay = new OverlayItem(new GeoPoint(
				mapAnnotation.latitude, mapAnnotation.longitude),
				mapAnnotation.title, mapAnnotation.description);
		overlay.setMarker(markerFactory.getMarker(mapAnnotation));
		mapAnnotationsOverlay.addItem(overlay);
		return overlay;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Utils.CODE_MAP_ANNOTATION_EDIT_REQUEST:
			if (resultCode == RESULT_OK) {
				mapAnnotationInContext.title = data.getExtras().getString(
						Utils.EXTRA_TITLE);
				mapAnnotationInContext.description = data.getExtras()
						.getString(Utils.EXTRA_DESCRIPTION);
				int color = data.getExtras().getInt(Utils.EXTRA_COLOR);
				if (color != mapAnnotationInContext.color) {
					mapAnnotationInContext.color = color;
					overlayItemInContext.setMarker(markerFactory
							.getMarker(mapAnnotationInContext));
					mapAnnotationsOverlay.requestRedraw();
				}
				updateBubbleForMapAnnotation(mapAnnotationInContext);
			} else {
				if (isCreation) {
					mapAnnotations.remove(mapAnnotationInContext);
					mapAnnotationsOverlay.removeItem(overlayItemInContext);
				}
			}

			overlayItemInContext = null;
			mapAnnotationInContext = null;
			isCreation = false;

			break;

		case Utils.CODE_CONNECTION_FAILURE_RESOLUTION_REQUEST:
			if (resultCode == RESULT_OK) {
				d("PlayServices Resolved");
			} else {
				d("PlayServices Not Resolved");
			}
			break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void showBubbleForMapAnnotationInteractive(int index) {
		MapAnnotation mapAnnotation = mapAnnotations.get(index);
		if (currentMapAnnotationForBubble == mapAnnotation) {
			// toggle
			clearBubble();
			return;
		}

		showBubbleForMapAnnotation(mapAnnotation);
	}

	private void showBubbleForMapAnnotation(MapAnnotation mapAnnotation) {
		currentMapAnnotationForBubble = mapAnnotation;

		View bubbleView = getLayoutInflater().inflate(
				R.layout.bubble_map_annotation, null);
		Utils.setBackground(bubbleView,
				getResources()
						.getDrawable(R.drawable.balloon_overlay_unfocused));
		TextView textViewTitle = (TextView) bubbleView.findViewById(R.id.textViewTitle);
		textViewTitle.setText(mapAnnotation.title);
		TextView textViewDescription = (TextView) bubbleView.findViewById(R.id.textViewDescription);
		textViewDescription.setText(mapAnnotation.description);
		Bitmap bitmap = Utils.viewToBitmap(this, bubbleView);
		BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);

		GeoPoint gp = new GeoPoint(mapAnnotation.latitude,
				mapAnnotation.longitude);

		if (bubbleTextOverlay != null) {
			mapView.getOverlays().remove(bubbleTextOverlay);
		}
		bubbleTextOverlay = new ArrayItemizedOverlay(bd);
		OverlayItem bubble = new OverlayItem();
		bubble.setPoint(gp);
		bubbleTextOverlay.addItem(bubble);
		mapView.getOverlays().add(bubbleTextOverlay);
	}

	private void updateBubbleForMapAnnotation(MapAnnotation mapAnnotation) {
		if (currentMapAnnotationForBubble == mapAnnotation) {
			showBubbleForMapAnnotation(mapAnnotation);
		}
	}

	private void showContextMenuForMapAnnotation(int index) {
		mapAnnotationInContext = mapAnnotations.get(index);
		overlayItemInContext = Utils.getItem(mapAnnotationsOverlay, index);
		isCreation = false;

		runOnUiThread(new Runnable() {
			public void run() {
				openContextMenu(MainActivity.this.mapView);
			}
		});
	}

	private void editMapAnnotation() {
		launchMapAnnotationEditActivity();
	}

	private void deleteMapAnnotation(boolean confirm) {
		if (confirm) {
			showAlertDialog(R.string.confirm_delete_map_annotation, true,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							MainActivity.this.deleteMapAnnotation(false);
						}
					}, null);
		} else {
			mapAnnotations.remove(mapAnnotationInContext);
			mapAnnotationsOverlay.removeItem(overlayItemInContext);

			cleanUpContextInformation();
		}
	}

	private void starMapAnnotation() {
		// toggle
		mapAnnotationInContext.isBookmarked = !mapAnnotationInContext.isBookmarked;
		overlayItemInContext.setMarker(markerFactory
				.getMarker(mapAnnotationInContext));
		mapAnnotationsOverlay.requestRedraw();
	}

	private void cleanUpContextInformation() {
		overlayItemInContext = null;
		mapAnnotationInContext = null;
		isCreation = false;
	}

	private void launchMapAnnotationEditActivity() {
		Intent intent = new Intent();
		intent.setClass(this, MapAnnotationEditActivity.class);
		intent.putExtra(Utils.EXTRA_IS_NEW, isCreation);
		intent.putExtra(Utils.EXTRA_TITLE, mapAnnotationInContext.title);
		intent.putExtra(Utils.EXTRA_DESCRIPTION,
				mapAnnotationInContext.description);
		intent.putExtra(Utils.EXTRA_COLOR, mapAnnotationInContext.color);
		startActivityForResult(intent, Utils.CODE_MAP_ANNOTATION_EDIT_REQUEST);
	}

	private void clearBubble() {
		if (bubbleTextOverlay != null) {
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

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						Utils.CODE_CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			// If no resolution is available, display a dialog to the user with
			// the error.
			showGooglePlayServicesErrorDialog(connectionResult.getErrorCode());
		}
	}

	private boolean isGooglePlayServicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			d("Google Play services is available.");
			return true;
		} else {
			// Google Play services was not available for some reason
			// Display an error dialog
			showGooglePlayServicesErrorDialog(resultCode);
			return false;
		}
	}

	private void showGooglePlayServicesErrorDialog(int errorCode) {
		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, Utils.CODE_CONNECTION_FAILURE_RESOLUTION_REQUEST);
		errorDialog.show();
	}

	private void showAlertDialog(int resourceId, boolean showCancel,
			OnClickListener okListener, OnClickListener cancelListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(resourceId).setPositiveButton(android.R.string.ok,
				okListener);
		if (showCancel) {
			builder.setNegativeButton(android.R.string.cancel, cancelListener);
		}

		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void showWelcomeDialog() {
		showAlertDialog(R.string.welcome, false, null, null);
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

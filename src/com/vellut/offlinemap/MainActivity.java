package com.vellut.offlinemap;

import static com.vellut.offlinemap.Utils.d;
import static com.vellut.offlinemap.Utils.e;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

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
	private File dataFile;
	private LocationClient locationClient;
	private MapView mapView;
	private boolean zoomToCurrentPositionOnConnected;
	private MarkerFactory markerFactory;

	private MapData mapData;
	private ArrayList<MapAnnotation> mapAnnotations;
	private CurrentPosition currentPosition;
	private boolean hasEditedMapAnnotations;

	private ArrayItemizedOverlay currentPositionOverlay;
	private ArrayItemizedOverlay mapAnnotationsOverlay;
	private ArrayItemizedOverlay bubbleTextOverlay;

	private OverlayItem overlayItemInContext;
	private MapAnnotation mapAnnotationInContext;
	private int indexMapAnnotationInContext;
	private boolean isCreation;

	private boolean isEditingOrCreating;
	private boolean isLocationUpdating;

	private MapAnnotation currentMapAnnotationForBubble;

	private CurrentPositionDrawable currentPositionWithAccuracyMarker;

	private boolean isFirstTimeRun;
	private boolean aBoolean;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		d("In OnCreate");

		// fill data from generic.xml
		Utils.fillGenericData(this);

		locationClient = new LocationClient(this, this, this);
		restorePreferences();
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		init();
		configureUI();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}

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
		hasEditedMapAnnotations = false;
	}

	@Override
	protected void onStop() {
		super.onStop();

		locationClient.disconnect();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		MenuItem item = menu.findItem(R.id.action_initial_view);
		String cityName = getString(R.string.city_name);
		item.setTitle(getString(R.string.action_initial_view, cityName));
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
		case R.id.action_export_import:
			showExportImportDialog();
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

	@Override
	public void onContextMenuClosed(Menu menu) {
		super.onContextMenuClosed(menu);
	}

	private void zoomToCurrentPosition() {
		// do not launch if location currently updating
		if (isGooglePlayServicesConnected() && !isLocationUpdating) {
			setProgressBarIndeterminateVisibility(true);

			LocationRequest lr = new LocationRequest();
			lr.setNumUpdates(1).setFastestInterval(1).setInterval(1)
					.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
					.setExpirationDuration(Utils.LOCATION_UPDATE_TIMEOUT);
			locationClient.requestLocationUpdates(lr, this);

			isLocationUpdating = true;

			new CountDownTimer(Utils.LOCATION_UPDATE_TIMEOUT,
					Utils.LOCATION_UPDATE_TIMEOUT) {
				public void onTick(long millisUntilFinished) {
				}

				public void onFinish() {
					// Timeout reached before we could get a fix
					setProgressBarIndeterminateVisibility(false);
					if (isLocationUpdating) {
						isLocationUpdating = false;
						locationClient.removeLocationUpdates(MainActivity.this);

						Location location = locationClient.getLastLocation();
						if (location != null) {
							setCurrentPosition(location);
						} else {
							showAlertDialog(R.string.timeout_location, false,
									null, null);
						}
					}
				}
			}.start();

		} else {
			zoomToCurrentPositionOnConnected = true;
		}
	}

	private void zoomToInitialPosition() {
		mapView.getController().setCenter(
				new GeoPoint(Utils.INITIAL_LAT, Utils.INITIAL_LON));
		mapView.getController().setZoom(Utils.INITIAL_ZOOM);
	}

	private void showExportImportDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(R.array.items_export_import,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int itemIndex) {
						if (itemIndex == 0) {
							// export
							startExportMapAnnotationsToFile();
						} else {
							// import
							startImportMapAnnotationsFromFile();
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel, null);

		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void init() {
		d(getApplicationContext().getPackageName());
		String mapName = Utils.MAP_NAME;
		mapFile = new File(getExternalCacheDir(), mapName);
		if (!mapFile.exists() || hasNewVersion(mapFile)) {
			d("Installing map");
			createFileFromAsset(mapFile, mapName);
		}

		if (Utils.DATA_NAME != null) {
			// Data file to preload
			String datafileName = Utils.DATA_NAME;
			dataFile = new File(getExternalCacheDir(), datafileName);
			if (!dataFile.exists() || hasNewVersion(dataFile)) {
				d("Installing data file");
				createFileFromAsset(dataFile, datafileName);

				if (TextUtils.equals(Utils.UI_MODE, Utils.UI_MODE_STAR_ONLY)) {
					// Index existing mapAnnotations
					Map<String, MapAnnotation> indexPreviousMapAnnotations = new HashMap<String, MapAnnotation>();
					for (MapAnnotation mapAnnotation : mapAnnotations) {
						indexPreviousMapAnnotations.put(mapAnnotation.id, mapAnnotation);
					}

					String dataFileContent = null;
					InputStream is = null;
					try {
						is = new BufferedInputStream(new FileInputStream(
								dataFile));
						dataFileContent = Utils.convertStreamToString(is);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
						}
					}

					if (dataFileContent != null) {
						ArrayList<MapAnnotation> dataFileMapAnnotations = 
								deserializeMapAnnotationsFromString(dataFileContent);

						if (!indexPreviousMapAnnotations.isEmpty()) {
							// We will modify the map annotations below
							// so need to save later on
							hasEditedMapAnnotations = true;
							for(MapAnnotation mapAnnotation : dataFileMapAnnotations) {
								MapAnnotation previousMapAnnotation = indexPreviousMapAnnotations.get(mapAnnotation.id);
								if(previousMapAnnotation != null) {
									// Copy bookmark info
									mapAnnotation.isBookmarked = previousMapAnnotation.isBookmarked;
								}
							}
							mapAnnotations = dataFileMapAnnotations;
						}
					}

				} else {
					// FIXME Implement merging of the modified notes and
					// updated note
				}
			}
		}

		markerFactory = new MarkerFactory(this);

		hasEditedMapAnnotations = false;

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
		if (sSavedLocations != null) {
			mapAnnotations = deserializeMapAnnotationsFromString(sSavedLocations);
		} else {
			mapAnnotations = new ArrayList<MapAnnotation>();
		}

		String sCurrentPosition = settings.getString(
				Utils.PREF_CURRENT_POSITION, null);
		if (sCurrentPosition != null) {
			try {
				currentPosition = (CurrentPosition) deserializeFromString(
						sCurrentPosition, CurrentPosition.class);
			} catch (Exception ex) {
				Log.e(Utils.TAG, "Error getting saved CurrentPosition", ex);
			}
		} else {
			currentPosition = null;
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

		if (hasEditedMapAnnotations) {
			Type type = new TypeToken<ArrayList<MapAnnotation>>() {
			}.getType();
			editor.putString(Utils.PREF_SAVED_LOCATIONS,
					serializeToString(mapAnnotations, type));
		}

		editor.putString(Utils.PREF_CURRENT_POSITION,
				serializeToString(currentPosition, CurrentPosition.class));

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

	@SuppressWarnings("unchecked")
	private ArrayList<MapAnnotation> deserializeMapAnnotationsFromString(
			String json) {
		ArrayList<MapAnnotation> deserMapAnnotations = null;
		try {
			Type type = new TypeToken<ArrayList<MapAnnotation>>() {
			}.getType();
			deserMapAnnotations = (ArrayList<MapAnnotation>) deserializeFromString(
					json, type);
		} catch (Exception ex) {
			Log.e(Utils.TAG, "Error deserializing Map Annotations", ex);
			deserMapAnnotations = new ArrayList<MapAnnotation>();
		}
		return deserMapAnnotations;
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
		currentPositionWithAccuracyMarker = new CurrentPositionDrawable(
				mapView, currentPositionMarker, true, Color.BLUE);
		currentPositionOverlay = new ArrayItemizedOverlay(
				currentPositionWithAccuracyMarker, false);
		mapView.getOverlays().add(currentPositionOverlay);

		for (MapAnnotation mapAnnotation : mapAnnotations) {
			addMarker(mapAnnotation);
		}

		if (currentPosition != null) {
			addCurrentPositionMarker();
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
				if (TextUtils.equals(Utils.UI_MODE, Utils.UI_MODE_FULL)) {
					MainActivity.this.showContextMenuForMapAnnotation(index);
				} else {
					initializeContextForIndex(index);
					starMapAnnotation();
				}
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
		indexMapAnnotationInContext = mapAnnotations.size() - 1; // last

		isCreation = true;
		isEditingOrCreating = true;

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
				hasEditedMapAnnotations = true;
				updateBubbleForMapAnnotation(mapAnnotationInContext);
			} else {
				if (isCreation) {
					mapAnnotations.remove(mapAnnotationInContext);
					mapAnnotationsOverlay.removeItem(overlayItemInContext);
				}
			}

			cleanUpContextInformation();

			break;

		case Utils.CODE_CONNECTION_FAILURE_RESOLUTION_REQUEST:
			if (aBoolean) {
				d("PlayServices Resolved");
			} else {
				d("PlayServices Not Resolved");
			}
			break;

		case Utils.CODE_EXPORT_FILE_EXPLORER_REQUEST:
			if (resultCode == RESULT_OK) {
				String dirPath = data.getExtras().getString(
						Utils.EXTRA_FILE_PATH);
				File file = new File(dirPath);
				if (file.canWrite()) {
					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd-HHmmss");
					String fileName = Utils.OFM_FILE_NAME_FOR_EXPORT + "-"
							+ sdf.format(now) + "." + Utils.OFM_FILE_EXTENSION;
					file = new File(dirPath, fileName);
					Type type = new TypeToken<ArrayList<MapAnnotation>>() {
					}.getType();
					String ofmData = serializeToString(mapAnnotations, type);
					OutputStream os = null;
					try {
						os = new BufferedOutputStream(
								new FileOutputStream(file));
						os.write(ofmData.getBytes("UTF-8"));
						Toast.makeText(this, getString(R.string.export_done),
								Toast.LENGTH_SHORT).show();
					} catch (IOException ex) {
						e("Error exporting", ex);
					} finally {
						if (os != null) {
							try {
								os.close();
							} catch (IOException e) {
							}
						}
					}
				} else {
					showAlertDialog(R.string.cannot_write, false, null, null);
				}
			}
			break;

		case Utils.CODE_IMPORT_FILE_EXPLORER_REQUEST:
			if (resultCode == RESULT_OK) {
				String filePath = data.getExtras().getString(
						Utils.EXTRA_FILE_PATH);
				File file = new File(filePath);

				String sSavedLocations;
				InputStream is = null;
				try {
					is = new BufferedInputStream(new FileInputStream(new File(
							filePath)));
					sSavedLocations = Utils.convertStreamToString(is);
					Type type = new TypeToken<ArrayList<MapAnnotation>>() {
					}.getType();

					mapAnnotations = (ArrayList<MapAnnotation>) deserializeFromString(
							sSavedLocations, type);
					hasEditedMapAnnotations = true;

					clearBubble();
					mapAnnotationsOverlay.clear();
					for (MapAnnotation mapAnnotation : mapAnnotations) {
						addMarker(mapAnnotation);
					}
					mapAnnotationsOverlay.requestRedraw();

					Toast.makeText(this, getString(R.string.import_done),
							Toast.LENGTH_SHORT).show();
				} catch (IOException ex) {
					e("Error exporting", ex);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
						}
					}
				}
			}
			break;

		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void startExportMapAnnotationsToFile() {
		Intent intent = new Intent();
		intent.setClass(this, FileExplorerActivity.class);
		intent.putExtra(Utils.EXTRA_CHOOSE_DIRECTORY_ONLY, true);
		startActivityForResult(intent, Utils.CODE_EXPORT_FILE_EXPLORER_REQUEST);
	}

	private void startImportMapAnnotationsFromFile() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confirm_import)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent();
						intent.setClass(MainActivity.this,
								FileExplorerActivity.class);
						intent.putExtra(Utils.EXTRA_EXTENSION_FILTER,
								Utils.OFM_FILE_EXTENSION);
						startActivityForResult(intent,
								Utils.CODE_IMPORT_FILE_EXPLORER_REQUEST);
					}
				});
		builder.create().show();
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
		TextView textViewTitle = (TextView) bubbleView
				.findViewById(R.id.textViewTitle);
		textViewTitle.setText(mapAnnotation.title);
		TextView textViewDescription = (TextView) bubbleView
				.findViewById(R.id.textViewDescription);
		if (mapAnnotation.description == null
				|| mapAnnotation.description.isEmpty()) {
			((ViewGroup) bubbleView).removeView(textViewDescription);
		} else {
			textViewDescription.setText(mapAnnotation.description);
		}
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
		initializeContextForIndex(index);

		runOnUiThread(new Runnable() {
			public void run() {
				openContextMenu(MainActivity.this.mapView);
			}
		});
	}

	private void editMapAnnotation() {
		isCreation = false;
		isEditingOrCreating = true;

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

			hasEditedMapAnnotations = true;

			cleanUpContextInformation();
		}
	}

	private void starMapAnnotation() {
		// toggle
		mapAnnotationInContext.isBookmarked = !mapAnnotationInContext.isBookmarked;
		overlayItemInContext.setMarker(markerFactory
				.getMarker(mapAnnotationInContext));
		mapAnnotationsOverlay.requestRedraw();

		hasEditedMapAnnotations = true;

		cleanUpContextInformation();
	}

	private void initializeContextForIndex(int index) {
		indexMapAnnotationInContext = index;
		mapAnnotationInContext = mapAnnotations.get(index);
		overlayItemInContext = Utils.getItem(mapAnnotationsOverlay, index);
	}

	private void cleanUpContextInformation() {
		d("CLEANUPCONTEXT");

		overlayItemInContext = null;
		mapAnnotationInContext = null;
		isCreation = false;
		isEditingOrCreating = false;
	}

	@Override
	public void onSaveInstanceState(Bundle outstate) {
		d("SAVEINSTANCESTATE " + isEditingOrCreating);
		if (isEditingOrCreating) {
			outstate.putInt(Utils.EXTRA_EDITING_CONTEXT_INDEX,
					indexMapAnnotationInContext);
			outstate.putBoolean(Utils.EXTRA_EDITING_CONTEXT_IS_CREATION,
					isCreation);
		} // else do not care
	}

	private void restoreState(Bundle savedInstanceState) {
		d("RESTORESTATE");
		if (savedInstanceState.keySet().contains(
				Utils.EXTRA_EDITING_CONTEXT_INDEX)) {
			indexMapAnnotationInContext = savedInstanceState
					.getInt(Utils.EXTRA_EDITING_CONTEXT_INDEX);
			isCreation = savedInstanceState
					.getBoolean(Utils.EXTRA_EDITING_CONTEXT_IS_CREATION);
			isEditingOrCreating = true;

			mapAnnotationInContext = mapAnnotations
					.get(indexMapAnnotationInContext);
			overlayItemInContext = Utils.getItem(mapAnnotationsOverlay,
					indexMapAnnotationInContext);
		}
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

	private File createFileFromAsset(File f, String mapName) {
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
		showAlertDialog(getString(resourceId), showCancel, okListener,
				cancelListener);
	}

	private void showAlertDialog(String message, boolean showCancel,
			OnClickListener okListener, OnClickListener cancelListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setPositiveButton(android.R.string.ok,
				okListener);
		if (showCancel) {
			builder.setNegativeButton(android.R.string.cancel, cancelListener);
		}

		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void showWelcomeDialog() {
		String app_name = getString(R.string.app_name);
		showAlertDialog(getString(R.string.welcome, app_name), false, null,
				null);
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
		setProgressBarIndeterminateVisibility(false);

		if (isLocationUpdating) {
			isLocationUpdating = false;
			setCurrentPosition(location);
		}
	}

	private void setCurrentPosition(Location location) {
		currentPosition = new CurrentPosition();

		currentPosition.latitude = location.getLatitude();
		currentPosition.longitude = location.getLongitude();

		if (location.hasAccuracy()) {
			currentPosition.accuracy = location.getAccuracy();
		} else {
			currentPosition.accuracy = 0;
		}

		showCurrentPosition();
		addCurrentPositionMarker();
	}

	private void showCurrentPosition() {
		GeoPoint position = new GeoPoint(currentPosition.latitude,
				currentPosition.longitude);
		byte zoom = mapView.getMapPosition().getZoomLevel();
		if (zoom < 17) {
			zoom = 17;
		}

		mapView.getController().setCenter(position);
		mapView.getController().setZoom(zoom);
	}

	private void addCurrentPositionMarker() {
		GeoPoint position = new GeoPoint(currentPosition.latitude,
				currentPosition.longitude);

		currentPositionWithAccuracyMarker.setAccuracy(currentPosition.accuracy);

		currentPositionOverlay.clear();
		OverlayItem overlay = new OverlayItem();
		overlay.setPoint(position);
		currentPositionOverlay.addItem(overlay);
	}

}

package com.vellut.offlinemap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;
import com.vellut.offlinemap.tokyo.R;

public class MapAnnotationEditActivity extends Activity {

	private EditText editTextTitle;
	private EditText editTextDescription;
	private Spinner spinnerColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_map_annotation_edit);

		editTextTitle = (EditText) findViewById(R.id.editTextTitle);
		editTextDescription = (EditText) findViewById(R.id.editTextDescription);
		spinnerColor = (Spinner) findViewById(R.id.spinnerColor);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras.getBoolean(Utils.EXTRA_IS_NEW)) {
			// defaults
			setTitle(R.string.map_annotation_create);
		} else {
			setTitle(R.string.map_annotation_edit);
		}

		String title = extras.getString(Utils.EXTRA_TITLE, "");
		String description = extras.getString(Utils.EXTRA_DESCRIPTION, "");
		int color = extras.getInt(Utils.EXTRA_COLOR, Utils.DEFAULT_MARKER_COLOR);
		
		editTextTitle.setText(title);
		editTextDescription.setText(description);
		
		ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(this, 
				R.layout.spinner_color_picker, Utils.MAP_ANNOTATION_COLORS);
		spinnerColor.setAdapter(colorPickerAdapter);
		spinnerColor.setSelection(colorPickerAdapter.getPosition(color));

		this.setFinishOnTouchOutside(false);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		EasyTracker.getInstance(this).activityStop(this);
	}

	public void onOk(View v) {
		String title = editTextTitle.getText().toString();
		if (TextUtils.isEmpty(title)) {
			Utils.showErrorToast(this, getString(R.string.error_empty_title));
			return;
		}
		String description = editTextDescription.getText().toString();
		int color = (Integer) spinnerColor.getSelectedItem();

		Intent data = new Intent();
		data.putExtra(Utils.EXTRA_TITLE, title);
		data.putExtra(Utils.EXTRA_DESCRIPTION, description);
		data.putExtra(Utils.EXTRA_COLOR, color);
		setResult(RESULT_OK, data);
		finish();
	}

	public void onCancel(View v) {
		Intent data = new Intent();
		setResult(RESULT_CANCELED, data);
		finish();
	}

}

package com.vellut.offlinemap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.vellut.offlinemap.tokyo.R;

public class MapAnnotationEditActivity extends Activity {

	private EditText editTextTitle;
	private EditText editTextDescription;
	private boolean isNew;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_map_annotation_edit);

		Intent intent = getIntent();
		if (intent.getExtras().getBoolean(Utils.EXTRA_IS_NEW)) {
			// defaults
			setTitle(R.string.map_annotation_create);
			isNew = true;
		} else {
			setTitle(R.string.map_annotation_edit);
			isNew = false;
		}

		this.setFinishOnTouchOutside(false);

		editTextTitle = (EditText) findViewById(R.id.editTextTitle);
		editTextDescription = (EditText) findViewById(R.id.editTextDescription);
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	public void onOk(View v) {
		String title = editTextTitle.getText().toString();
		if (TextUtils.isEmpty(title)) {
			Utils.showErrorToast(this, getString(R.string.error_empty_title));
			return;
		}
		String description = editTextDescription.getText().toString();

		Intent data = new Intent();
		data.putExtra(Utils.EXTRA_TITLE, title);
		data.putExtra(Utils.EXTRA_DESCRIPTION, description);
		setResult(RESULT_OK, data);
		finish();
	}

	public void onCancel(View v) {
		Intent data = new Intent();
		setResult(RESULT_CANCELED, data);
		finish();
	}

}

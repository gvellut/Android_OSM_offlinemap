package com.vellut.offlinemap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.vellut.offlinemap.kansai.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import static com.vellut.offlinemap.Utils.e;

public class FileExplorerActivity extends Activity {

	// Check if the first level of the directory structure is the one showing
	private File rootDirectory;
	private Boolean firstLevel = true;
	private ArrayList<Item> fileList;
	private File currentFolder;
	private String selectedFile;
	private boolean chooseDirectory;
	private String extensionFilter;

	ArrayAdapter<Item> adapter;
	ListView listViewFiles;
	TextView textViewCurrentFolder;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_file_explorer);

		Bundle configuration = savedInstanceState;
		if(savedInstanceState == null) {
			configuration = getIntent().getExtras();
		}
		initData(configuration);
		configureUI();
	}

	private void initData(Bundle extras) {
		rootDirectory = Environment.getExternalStorageDirectory();

		if (extras != null) {
			currentFolder = new File(extras.getString(Utils.EXTRA_START_PATH,
					rootDirectory.getAbsolutePath()));
			chooseDirectory = extras.getBoolean(Utils.EXTRA_CHOOSE_DIRECTORY_ONLY, false);
			extensionFilter = extras.getString(Utils.EXTRA_EXTENSION_FILTER, null);
		} else {
			currentFolder = rootDirectory;
			chooseDirectory = false;
		}

		firstLevel = currentFolder.getAbsolutePath().equals(rootDirectory.getAbsolutePath());

		fileList = new ArrayList<Item>();
		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(
						fileList.get(position).icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};

		loadFileList();
	}

	private void configureUI() {
		if (!chooseDirectory) {
			ViewGroup buttonBar = (ViewGroup) findViewById(R.id.buttonBar);
			View btnOk = findViewById(R.id.btnOk);
			buttonBar.removeView(btnOk);

			setTitle(R.string.file_explorer_activity_choose_file);
		} else {
			setTitle(R.string.file_explorer_activity_choose_folder);
		}

		textViewCurrentFolder = (TextView) findViewById(R.id.textViewCurrentFolder);
		setCurrentFolderText();

		listViewFiles = (ListView) findViewById(R.id.listViewFiles);

		listViewFiles.setAdapter(adapter);
		listViewFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedFile = fileList.get(position).file;
				File sel = new File(currentFolder, selectedFile);
				if (sel.isDirectory()) {
					// Adds chosen directory to list
					currentFolder = sel;
					firstLevel = false;

					setCurrentFolderText();
					adapter.clear();
					loadFileList();
					adapter.notifyDataSetChanged();

					listViewFiles.setSelectionAfterHeaderView();
				} else if (!firstLevel && position == 0) {
					// 'up' was clicked

					// currentFolder modified to exclude present directory
					String sCurrentFolder = currentFolder.getAbsolutePath();
					currentFolder = new File(sCurrentFolder.substring(0,
							sCurrentFolder.lastIndexOf("/")));
					firstLevel = currentFolder.getAbsolutePath().equals(rootDirectory.getAbsolutePath());

					setCurrentFolderText();
					adapter.clear();
					loadFileList();
					adapter.notifyDataSetChanged();

					listViewFiles.setSelectionAfterHeaderView();
				} else if (!chooseDirectory) {
					// send Intent result
					Intent data = new Intent();
					data.putExtra(Utils.EXTRA_FILE_PATH, sel.getAbsolutePath());
					setResult(RESULT_OK, data);
					FileExplorerActivity.this.finish();
				}
			}
		});
	}

	private void setCurrentFolderText() {
		String relPath = Utils.pathRelativeTo(currentFolder.getAbsolutePath(),
				rootDirectory.getAbsolutePath());
		relPath = "/" + relPath;
		textViewCurrentFolder.setText(getString(R.string.file_explorer_activity_in_folder, relPath));
	}

	@Override
	public void onSaveInstanceState(Bundle outstate) {
		outstate.putString(Utils.EXTRA_START_PATH, currentFolder.getAbsolutePath());
		outstate.putBoolean(Utils.EXTRA_CHOOSE_DIRECTORY_ONLY, chooseDirectory);
		outstate.putString(Utils.EXTRA_EXTENSION_FILTER, extensionFilter);
	}

	private void loadFileList() {
		try {
			currentFolder.mkdirs();
		} catch (SecurityException e) {
			e("unable to write on the sd card ", e);
		}

		if (currentFolder.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String fileName) {
					File sel = new File(dir, fileName);
					String extension = Utils.getExtension(fileName);
					return (!sel.isHidden() &&
							(sel.isDirectory() ||
									(!chooseDirectory &&
											(sel.isFile() &&
													(extensionFilter == null ||
															extensionFilter.equals(extension))))));
				}
			};

			String[] fList = currentFolder.list(filter);
			if (!firstLevel) {
				Item itemUp = new Item(getString(R.string.file_explorer_activity_up));
				itemUp.icon = R.drawable.directory_up;
				fileList.add(itemUp);
			}

			for (int i = 0; i < fList.length; i++) {
				Item item = new Item(fList[i]);
				File sel = new File(currentFolder, fList[i]);
				if (sel.isDirectory()) {
					item.icon = R.drawable.directory_icon;
				} else {
					item.icon = R.drawable.file_icon;
				}
				fileList.add(item);
			}
		}
	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file) {
			this.file = file;
		}

		@Override
		public String toString() {
			return file;
		}
	}

	public void onOk(View v) {
		// only for chooseDirectory == true
		Intent data = new Intent();
		data.putExtra(Utils.EXTRA_FILE_PATH, currentFolder.getAbsolutePath());
		setResult(RESULT_OK, data);
		finish();
	}

	public void onCancel(View v) {
		Intent data = new Intent();
		setResult(RESULT_CANCELED, data);
		finish();
	}
}
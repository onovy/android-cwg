/*
	This file is part of Android-CWG.

	Android-CWG is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Android-CWG is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Android-CWG.  If not, see <http://www.gnu.org/licenses/>.
*/

package cz.nomi.cwg;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ShowActivity extends Activity {
	private DatabaseAdapter db;
	private String jpg;
	private ImageView image;
	private ProgressBar imageProgress;

	private void showImage(final boolean forceDownload) {
		final Handler handler = new Handler();
		image.setVisibility(View.GONE);
		if (jpg == null) {
			imageProgress.setVisibility(View.GONE);
			return;
		} else {
			imageProgress.setVisibility(View.VISIBLE);
		}

		new Thread() {
			@Override
			public void run() {
				try {
					SharedPreferences settings =
						PreferenceManager.getDefaultSharedPreferences(getBaseContext());
					final String imageUrl = settings.getString("image_url",
						getText(R.string.pref_image_url).toString());
					File cache = Storage.getJpgCacheFile(jpg);
					InputStream in;
					URL url = new URL(imageUrl + jpg);
					if (cache == null) {
						// Can't cache
						in = url.openStream();
					} else {
						if (forceDownload || !cache.exists()) {
							// Write to cache
							in = url.openStream();
							OutputStream outCache =
								new BufferedOutputStream(new FileOutputStream(cache));
							byte[] buf = new byte[1024];
							int len;
							while ((len = in.read(buf)) > 0) {
								outCache.write(buf, 0, len);
							}
							in.close();
							outCache.close();
						}
						// Load from cache
						in = new BufferedInputStream(new FileInputStream(cache));
					}

					final Drawable d = Drawable.createFromStream(in, "src");
					handler.post(new Runnable() {
						public void run() {
							imageProgress.setVisibility(View.GONE);
							image.setImageDrawable(d);
							image.setVisibility(View.VISIBLE);
						}
					});
				} catch (final MalformedURLException mue) {
					handler.post(new Runnable() {
						public void run() {
							imageProgress.setVisibility(View.GONE);
							Toast.makeText(ShowActivity.this, mue.getClass().getName() +
								": " + mue.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				} catch (final IOException ioe) {
					handler.post(new Runnable() {
						public void run() {
							imageProgress.setVisibility(View.GONE);
							Toast.makeText(ShowActivity.this, ioe.getClass().getName() +
								": " + ioe.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}.start();
	}

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		setContentView(R.layout.show);

		db = new DatabaseAdapter(this);
		db.open();

		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		long cwgId = extras.getLong("cwgId");
		Cursor cur = db.getCwg(cwgId);

		TextView showTitle = (TextView) findViewById(R.id.show_title);
		TextView showCatalogTitle = (TextView) findViewById(R.id.show_catalog_title);
		TextView showCatalogId = (TextView) findViewById(R.id.show_catalog_id);
		TextView showCount = (TextView) findViewById(R.id.show_count);
		this.image = (ImageView) findViewById(R.id.image);
		this.imageProgress = (ProgressBar) findViewById(R.id.imageProgress);

		showTitle.setText(cur.getString(cur.getColumnIndex("title")));
		showCatalogTitle.setText(cur.getString(cur.getColumnIndex("catalog_title")));
		showCatalogId.setText(cur.getString(cur.getColumnIndex("catalog_id")));
		showCount.setText(cur.getString(cur.getColumnIndex("count")));
		this.jpg = cur.getString(cur.getColumnIndex("jpg"));

		cur.close();

		this.image.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				showImage(true);
			}
		});
		showImage(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		db.close();
	}
}

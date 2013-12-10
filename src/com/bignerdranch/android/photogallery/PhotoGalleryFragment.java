package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

//public class PhotoGalleryFragment extends Fragment {
public class PhotoGalleryFragment extends VisibleFragment {
	//used to for AsyncTask to create background thread to call and test networking code added in manifest
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		//The call to execute will start asyncTask below, which will then fire up its background thread and call
		//doInBackground
		
		//Hook up options menu callbacks
		setHasOptionsMenu(true);
		//new FetchItemsTask().execute();
		updateItems();
		
		//For intentService
		//Intent i = new Intent(getActivity(), PollService.class);
		//getActivity().startService(i);
		//To test
		//PollService.setServiceAlarm(getActivity(), true);
		
		//for thumbnail downloader
		//mThumbnailThread = new ThumbnailDownloader<ImageView>();
		mThumbnailThread = new ThumbnailDownloader(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
				if (isVisible()) {
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}
	
	//method for updateItems above
	public void updateItems() {
		new FetchItemsTask().execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView)v.findViewById(R.id.gridView);
		
		setupAdapter();
		
		//browse url of photo images with implicit intent
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView <?> gridView, View view, int pos, long id) {
				GalleryItem item = mItems.get(pos);
				
				Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
				//Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);
				Intent i = new Intent(getActivity(), PhotoPageActivity.class);
				i.setData(photoPageUri);
				
				startActivity(i);
			}
		});
		
		return v;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
	}
	
	void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;
		
		if (mItems != null) {
			//mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(),
				//	android.R.layout.simple_gallery_item, mItems));
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}
	//onPostExecute is run after doINBackground completes, and not on main thread, so dafe to update UI within it.
	//private class FetchItemsTask extends AsyncTask<Void, Void, Void> {
	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
		@Override
		//protected Void doInBackground(Void...params) {
		protected ArrayList<GalleryItem>doInBackground(Void...params) {
			//String query = "android"; //Just for testing
			Activity activity = getActivity();
			if (activity == null)
				return new ArrayList<GalleryItem>();
			
			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			
			if (query != null) {
				return new FlickrFetchr().search(query);
			} else {
			//we can remove below code to call the new fetchItems() method
			/*try {
				String result = new FlickrFetchr().getUrl("http://www.google.com");
			} catch (IOException ioe) {
				Log.e(TAG, "Failed to fetch URL: ", ioe);
			}*/
			//add this to code
			////new FlickrFetchr().fetchItems();
			//now that we have all this data, we put it into a model object
			////return null;
			return new FlickrFetchr().fetchItems();
		}
		}
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems = items;
			setupAdapter();
		}
	}
	//returns an imageview that displays the placeholder image
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView)convertView
					.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			
			return convertView;
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}
	//Hook up options menu
	@Override
	@TargetApi(11)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
			
			//Get data from our searchable.xml as a searchableinfo
			SearchManager searchManager = (SearchManager)getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
		}
	}
	
	@Override
	@TargetApi(11)
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();
			return true;
		case R.id.menu_item_clear:
			return true;
		case R.id.menu_item_toggle_polling:
			boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
			PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				getActivity().invalidateOptionsMenu();
			
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	//Implementation that checks to see if alarm is on, then changes to menu to show appropriate feedback
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}

}

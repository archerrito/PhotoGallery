package com.bignerdranch.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;
//This code creates a url object from a string
public class FlickrFetchr {
	//These constants define the endpoint, the method name, the API key, and one extra paramter
	//called extras, witha value of urls.
	public static final String TAG = "FlickFetchr";
	
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	//For intentService
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "http://api.flickr.com/services/rest/";
	private static final String API_KEY = "794895d2cbe6cd4db64d33f679e1c745";
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	//For search
	private static final String METHOD_SEARCH = "flickr.photos.search";
	private static final String PARAMS_EXTRAS = "extras";
	//For search
	private static final String PARAM_TEXT = "text";
	
	private static final String XML_PHOTO = "photo";
	
	private static final String EXTRA_SMALL_URL ="url_s";
	
	byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();
			
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	//while getUrlBytes does the heavy lifting, getUrl(string) is what we will use in this chapter
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	//use the constants to write a method that builds an appropriate request url and fetches its contents.
	//public void fetchItems() {
	//parseItems method needs an XMLPullPArser and an arraylist.
	//public ArrayList<GalleryItem> fetchItems() {
	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		
		try {
			/*
			String url = Uri.parse(ENDPOINT).buildUpon()
					.appendQueryParameter("method", METHOD_GET_RECENT)
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAMS_EXTRAS, EXTRA_SMALL_URL)
					.build().toString();
			*/
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
			//added for XMLPArser
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItems(items, parser);
		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
			//here we use uri builder to build the complete url for your flickr api requests.
			//its a convenience class for creating properly escaped parameterized URL's
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse items", xppe);
		}
		return items;
	}
	
	public ArrayList<GalleryItem> fetchItems() {
		//Move code here from above
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAMS_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();
		//
		return downloadGalleryItems(url);
	}
	
	public ArrayList<GalleryItem> search (String query) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAMS_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		return downloadGalleryItems(url);
				
	}
	//use XmlPullParser, an interface you can use to pull parse events off of a stream of XML.
	void parseItems (ArrayList<GalleryItem> items, XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && 
					XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				//change parseItems to read in owner attribute
				String owner = parser.getAttributeValue(null, "owner");
				
				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				//for adding webview to photo images
				item.setOwner(owner);
				items.add(item);
			}
			
			eventType = parser.next();
		}
	}

}

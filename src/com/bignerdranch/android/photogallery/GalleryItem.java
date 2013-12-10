package com.bignerdranch.android.photogallery;

public class GalleryItem {
	private String mCaption;
	private String mId;
	private String mUrl;
	//for implementing photo page in webview
	private String mOwner;
	
	//now that we have model objects, tie to fill them with data we got from Flickr.
	

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		mCaption = caption;
	}

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}
	
	public String getOwner() {
		return mOwner;
	}
	
	public void setOwner(String owner) {
		mOwner = owner;
	}
	
	public String getPhotoPageUrl() {
		return "http://flickr.com/photos/" + mOwner + "/" + mId;
	}
	
	public String toString() {
		return mCaption;
	}

}

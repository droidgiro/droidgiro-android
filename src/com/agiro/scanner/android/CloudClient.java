package com.agiro.scanner.android;

import java.net.URI;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

/**
 * Handles communication with the cloud service for pushing and retrieving
 * information.
 * 
 */
public class CloudClient {

	public static final String TAG = "aGiro.CloudClient";

	private static final String BASE_URL = "http://agiroapp.appspot.com";

	public static HttpResponse makeRequest(String urlPath, List<NameValuePair> params)
			throws Exception {

		DefaultHttpClient client = new DefaultHttpClient();
		URI uri = new URI(BASE_URL + urlPath);
		Log.e(TAG, "" + uri.toString());
		HttpPost post = new HttpPost(uri);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		post.setEntity(entity);
		HttpResponse res = client.execute(post);
		return res;
	}
}

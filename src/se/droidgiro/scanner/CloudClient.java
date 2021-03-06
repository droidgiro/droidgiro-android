package se.droidgiro.scanner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import se.droidgiro.Registration;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Handles communication with the cloud service for pushing and retrieving
 * information.
 * 
 */
public class CloudClient {

	public static final String TAG = "DroidGiro.CloudClient";

	private static final String REGISTER_URL = "http://cloud.droidgiro.se/register";

	private static final String INVOICES_URL = "http://cloud.droidgiro.se/invoices";

	/**
	 * Sends a pair request to the server with the specified pin code.
	 * 
	 * @param pin
	 *            the pin code to use when pairing with other end.
	 * @return the hash code to use in further communication, null if request
	 *         was denied.
	 * 
	 * @throws Exception
	 */
	public static Registration register(String pin) throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		URI uri = new URI(REGISTER_URL);
		Log.e(TAG, "" + uri.toString());
		HttpPost post = new HttpPost(uri);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("pin", pin));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		post.setEntity(entity);
		HttpResponse res = client.execute(post);
		Log.v(TAG, "Post to register returned "
				+ res.getStatusLine().getStatusCode());
		if (res.getStatusLine().getStatusCode() == 200) {
			StringBuilder sb = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					res.getEntity().getContent(), "UTF-8"));
			Log.v(TAG, "Post to register returned " + sb + " with status "
					+ res.getStatusLine().getStatusCode());
			Gson gson = new Gson();
			Registration registration = gson.fromJson(reader,
					Registration.class);
			Log.v(TAG, registration.toString());
			return registration;
		} else if (res.getStatusLine().getStatusCode() == 401) {
			Log.w(TAG, "Unauthorized");
			return new Registration();
		} else {
			Log.e(TAG, "Unknown error");
			return new Registration();
		}
	}

	public static boolean postFields(List<NameValuePair> fields)
			throws Exception {

		DefaultHttpClient client = new DefaultHttpClient();
		URI uri = new URI(INVOICES_URL);
		Log.e(TAG, "" + uri.toString());
		HttpPost post = new HttpPost(uri);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
		post.setEntity(entity);
		HttpResponse res = client.execute(post);
		return (res.getStatusLine().getStatusCode() == 201 ? true : false);
	}
}

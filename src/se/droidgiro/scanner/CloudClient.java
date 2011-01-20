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

import android.util.Log;

/**
 * Handles communication with the cloud service for pushing and retrieving
 * information.
 * 
 */
public class CloudClient {

	public static final String TAG = "DroidGiro.CloudClient";

	private static final String REGISTER_URL = "http://1.latest.agiroapp.appspot.com/register";

	private static final String INVOICES_URL = "http://1.latest.agiroapp.appspot.com/invoices";

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
	public static String register(String pin) throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		URI uri = new URI(REGISTER_URL);
		Log.e(TAG, "" + uri.toString());
		HttpPost post = new HttpPost(uri);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("identifier", pin));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		post.setEntity(entity);
		HttpResponse res = client.execute(post);
		Log.v(TAG, "Post to register returned "
				+ res.getStatusLine().getStatusCode());
		if (res.getStatusLine().getStatusCode() == 200) {
			StringBuilder sb = new StringBuilder();
			String line;

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					res.getEntity().getContent(), "UTF-8"));
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			Log.v(TAG, "Post to register returned " + sb + " with status "
					+ res.getStatusLine().getStatusCode());
			return sb.toString();
		}
		return null;
	}

	public static boolean postFields(String hash, List<NameValuePair> fields)
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

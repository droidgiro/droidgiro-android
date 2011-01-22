/*
 * Copyright (C) Brian Buikema
 * Found at http://blog.brianbuikema.com
 * License unkown.
 */

package se.droidgiro.scanner.resultlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import java.util.List;

public abstract class TextViewListAdapter<T> extends BaseAdapter {
	private LayoutInflater inflater;
	private List<T> dataObjects; // list of data objects
	private int viewId;

	/**
	 * This is the holder that will provide fast access to arbitrary objects and
	 * views. Use a subclass to adapt it for your needs.
	 */
	public static class ViewHolder {
		// back reference to our list object
		public Object data;
	}

	/**
	 * The constructor.
	 * 
	 * @param context
	 *            is the current context
	 * @param viewid
	 *            is the resource id of your list view item
	 * @param dataObjects
	 *            is the list data objects, or null, if you require to indicate
	 *            an empty list
	 */
	public TextViewListAdapter(Context context, int viewid,
			List<T> dataObjectList) {
		this.inflater = LayoutInflater.from(context);
		this.dataObjects = dataObjectList;
		this.viewId = viewid;

		if (dataObjectList == null) {
			this.dataObjects = new ArrayList<T>();
		}
	}

	/**
	 * The number of data objects in the list.
	 */
	public int getCount() {
		return this.dataObjects.size();
	}

	/**
	 * Get the data object.
	 * 
	 * @param position
	 *            (index) to retrieve
	 * 
	 * @return Return the object at indicated position. Note,
	 *         &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160; the holder
	 *         object uses a back reference to its related data
	 *         &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160; object. So, the
	 *         user usually should use {@link ViewHolder#data}
	 *         &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160; for faster
	 *         access.
	 */
	public Object getItem(int position) {
		return this.dataObjects.get(position);
	}

	/**
	 * Position equals id.
	 * 
	 * @return The id of the object
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Make a view to hold each row. This method is instantiated for each list
	 * data object. Using the Holder Pattern, avoids the unnecessary
	 * findViewById(...) calls.
	 * 
	 * @param position
	 *            (index) to retrieve
	 * @param view
	 *            is the view
	 * @param parent
	 *            is the associated ViewGroup
	 * 
	 * @return The view associated with this row
	 */
	public View getView(int position, View view, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unnecessary
		// calls to findViewById(...) on each row.
		ViewHolder holder;

		// When the view is not null, we can reuse it directly, there is no need
		// to re-inflate it. We only inflate a new View when the view supplied
		// by ListView is null.
		if (view == null) {
			view = this.inflater.inflate(this.viewId, null);
			// call the user's implementation
			holder = createHolder(view);
			// we set the holder as tag
			view.setTag(holder);
		} else {
			// get holder back...much faster than inflate
			holder = (ViewHolder) view.getTag();
		}

		// we must update the object's reference
		holder.data = getItem(position);
		// call the user's implementation
		bindHolder(holder);

		return view;
	}

	/**
	 * Creates your custom holder that carries a reference for your particular
	 * view.
	 * 
	 * @param v
	 *            is the view for the new holder object
	 * 
	 * @return The newly created ViewHolder
	 * 
	 */
	protected abstract ViewHolder createHolder(View v);

	/**
	 * Binds the data from user's object (typically an entity) to the holder.
	 * 
	 * @param h
	 *            is the holder that represents the data object
	 */
	protected abstract void bindHolder(ViewHolder h);
}

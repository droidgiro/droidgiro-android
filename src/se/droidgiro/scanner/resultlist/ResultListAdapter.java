package se.droidgiro.scanner.resultlist;

import android.content.Context;
import java.util.List;
import android.graphics.Typeface;
import android.widget.TextView;
import android.view.View;
import se.droidgiro.R;

public class ResultListAdapter extends
		TextViewListAdapter<ResultListHandler.ListItem> {

	private Context context;

	public ResultListAdapter(Context context, int viewid,
			List<ResultListHandler.ListItem> objects) {
		super(context, viewid, objects);
		this.context = context;
	}

	protected void bindHolder(ViewHolder holder) {
		ResultViewHolder resultHolder = (ResultViewHolder) holder;

		ResultListHandler.ListItem listItem = (ResultListHandler.ListItem) resultHolder.data;
		Typeface ocrb = Typeface.createFromAsset(context.getAssets(),
				"fonts/ocrb10.ttf");
		resultHolder.listItemDataView.setTypeface(ocrb);

		resultHolder.listItemDataView.setText(listItem.listItemData);
		resultHolder.listItemTypeView.setText(listItem.listItemType);
	}

	@Override
	protected ViewHolder createHolder(View v) {
		TextView listItemDataView = (TextView) v
				.findViewById(R.id.field_result_data);
		TextView listItemTypeView = (TextView) v
				.findViewById(R.id.field_result_type);
		ViewHolder resultHolder = new ResultViewHolder(listItemDataView,
				listItemTypeView);
		return resultHolder;
	}

	static class ResultViewHolder extends ViewHolder {
		TextView listItemDataView;
		TextView listItemTypeView;

		public ResultViewHolder(TextView listItemDataView,
				TextView listItemTypeView) {
			this.listItemDataView = listItemDataView;
			this.listItemTypeView = listItemTypeView;
		}
	}
}

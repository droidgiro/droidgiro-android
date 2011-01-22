package se.droidgiro.scanner.resultlist;

import se.droidgiro.R;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;

public class ResultListHandler {

	private List<ListItem> listItemList = new ArrayList<ListItem>();
	private ListItem reference;
	private ListItem amount;
	private ListItem account;
	private ListItem sent;
	private Boolean newData = false;

	public ResultListHandler (Context c) {
		reference = new ListItem();
		amount = new ListItem();
		account = new ListItem();
		sent = new ListItem();

		reference.listItemType = c.getString(R.string.reference_field);
		amount.listItemType = c.getString(R.string.amount_field);
		account.listItemType = c.getString(R.string.account_field);
		sent.listItemData = c.getString(R.string.invoice_sent);

		listItemList.add(reference);
		listItemList.add(amount);
		listItemList.add(account);
	}

	public void setReference(String reference) {
		try {
			if (!reference.equals(this.reference.listItemData)) {
				this.reference.listItemData = reference;
				newData = true;
			}
		} catch(NullPointerException e) {}
	}

	public void setAmount(String amount) {
		try {
			if (!amount.equals(this.amount.listItemData)) {
				this.amount.listItemData = amount;
				newData = true;
			}
		} catch(NullPointerException e) {}
	}

	public void setAccount(String account) {
		try {
			if (!account.equals(this.account.listItemData)) {
				this.account.listItemData = account;
				newData = true; 
			}
		} catch(NullPointerException e) {}
	}

	public void setSent(Boolean status) {
		if (status) {
			if (!listItemList.contains(sent)) {
				listItemList.add(sent);
			}
		} else {
			if (listItemList.contains(sent)) {
				listItemList.remove(sent);
			}
		}
	}

	public List<ListItem> getList() {
		return listItemList;
	}

	public void setNewData(Boolean newData) {
		this.newData = newData;
	}

	public Boolean hasNewData() {
		return newData;
	}

	public void clear() {
		reference.listItemData = null;
		amount.listItemData = null;
		account.listItemData = null;
		if (listItemList.contains(sent)) {
			listItemList.remove(sent);
		}
	}

	public class ListItem {
		public String listItemData = null;
	    public String listItemType = null;
	}

}

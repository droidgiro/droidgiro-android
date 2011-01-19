package se.droidgiro.scanner.auth;

import se.droidgiro.scanner.CaptureActivity;
import se.droidgiro.scanner.CloudClient;
import se.droidgiro.scanner.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PairingActivity extends Activity {

	private EditText digit1;
	private EditText digit2;
	private EditText digit3;
	private EditText digit4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.pairing);
		DigitWatcher dw = new DigitWatcher();

		digit1 = (EditText) findViewById(R.id.pairDigit1);
		digit2 = (EditText) findViewById(R.id.pairDigit2);
		digit3 = (EditText) findViewById(R.id.pairDigit3);
		digit4 = (EditText) findViewById(R.id.pairDigit4);
		digit1.requestFocus();

		digit1.addTextChangedListener(dw);
		digit2.addTextChangedListener(dw);
		digit3.addTextChangedListener(dw);
		digit4.addTextChangedListener(dw);
	}

	@Override
	protected void onResume() {
		super.onResume();
		digit1.setText("");
		digit2.setText("");
		digit3.setText("");
		digit4.setText("");
		digit1.requestFocus();
	}

	private class DigitWatcher implements TextWatcher {

		public void afterTextChanged(Editable s) {
			if (digit1.getText().length() == 0)
				digit1.requestFocus();
			else if (digit2.getText().length() == 0)
				digit2.requestFocus();
			else if (digit3.getText().length() == 0)
				digit3.requestFocus();
			else if (digit4.getText().length() == 0)
				digit4.requestFocus();
			else {
				// compose pin
				String pin = digit1.getText().toString()
						+ digit2.getText().toString()
						+ digit3.getText().toString()
						+ digit4.getText().toString();
				register(pin);
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	}

	public void clear() {
		digit4.setText("");
		digit3.setText("");
		digit2.setText("");
		digit1.setText("");
		digit1.requestFocus();
	}

	public void register(String pin) {
		String channel = null;
		try {
			channel = CloudClient.register(pin);
		} catch (Exception e) {
			clear();
		}
		if (channel == null)
			clear();
		else {
			// Open scanner
			Intent intent = new Intent(PairingActivity.this,
					CaptureActivity.class);
			intent.putExtra("identifier", pin);
			intent.putExtra("channel", channel);
			startActivity(intent);

		}

	}

}

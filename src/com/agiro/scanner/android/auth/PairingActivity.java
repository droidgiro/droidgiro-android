package com.agiro.scanner.android.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.agiro.scanner.android.CaptureActivity;
import com.agiro.scanner.android.R;

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

	private class DigitWatcher implements TextWatcher {

		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
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
				String identifier = digit1.getText().toString()
						+ digit2.getText().toString()
						+ digit3.getText().toString()
						+ digit4.getText().toString();
				// Open scanner
				Intent intent = new Intent(PairingActivity.this,
						CaptureActivity.class);
				intent.putExtra("identifier", identifier);
				startActivity(intent);
			}
		}
	}

}

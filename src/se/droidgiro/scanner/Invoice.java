/*
 * Copyright (C) 2011 DroidGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.droidgiro.scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * An invoice object represents all information needed to register a payment for
 * the same. A complete invoice contains:
 * 
 * <ul>
 * <li>A reference number with a valid check digit</li>
 * <li>An amount with two decimals with a valid check digit</li>
 * <li>A BG/PG account number</li>
 * </ul>
 * Optionally, an invoice may also contain the internal document type<br/>
 * 
 * Validation is made inside the corresponding set methods. If validation fails,
 * the fields are not set. The {@code isComplete} method will only return true
 * if all mandatory fields are set.
 * 
 * <br/>
 * <br/>
 * 
 * @author pakerfeldt
 * 
 */
public class Invoice {

	public static final String FIELDS_FOUND = "Invoice.fieldsFound";
	
	public static final int REFERENCE_FIELD = 1;

	public static final int AMOUNT_FIELD = 2;

	public static final int GIRO_ACCOUNT_FIELD = 4;

	public static final int DOCUMENT_TYPE_FIELD = 8;

	private final String TAG = "DroidGiro.Invoice";

	/*
	 * The patterns below have been derived from reading the following
	 * documents:
	 * http://www.bgc.se/upload/Gemensamt/Trycksaker/Manualer/BG6070.pdf
	 * http://www
	 * .plusgirot.se/sitemod/upload/root/www_plusgirot_se/pdfer/allmanbeskrivning
	 * /g445_allman_beskrivning_inbetalningservice.pdf
	 */

	/**
	 * OCR PATTERN
	 * <ul>
	 * <li>Start of string</li>
	 * <li>followed by H# OR</li>
	 * <li>&nbsp;&nbsp;&nbsp;&nbsp;followed by an optional #
	 * <em>(optional because the user might not have included the # left to the OCR
	 * number)</em></li>
	 * <li>followed by 2 to 25 digits <em>(the OCR number)</em></li>
	 * <li>followed by a #</li>
	 * <li><strong>NOT</strong> followed by two digits and #
	 * <em>(because in that case we have read the BG/PG
	 * account number)</em></li>
	 * </ul>
	 * Result: <br/>
	 * <ul>
	 * <li>group 1 - Not used</li>
	 * <li>group 2 - Complete OCR number (including OCR check digit)</li>
	 * <li>group 3 - OCR check digit</li>
	 * </ul>
	 */
	private static final Pattern OCR_PATTERN = Pattern
			.compile("^(H#|#?)(\\d{1,24}(\\d))#(?!\\d{2}#)");

	/**
	 * AMOUNT PATTERN
	 * <ul>
	 * <li>Start of string OR</li>
	 * <li>&nbsp;&nbsp;&nbsp;&nbsp;#</li>
	 * <li>followed by 1 to 8 digits <em>(the
	 * amount) followed by 2 digits (šre, the fractional part of total amount)</em>
	 * </li>
	 * <li>followed by 1 digit <em>(check)</em></li>
	 * <li>followed by a ></li>
	 * </ul>
	 * Result:<br/>
	 * <ul>
	 * <li>group 1 - Not used</li>
	 * <li>group 2 - Amount in whole SEK</li>
	 * <li>group 3 - Amount in šre (fractional part)</li>
	 * <li>group 4 - Amount check digit</li>
	 * </ul>
	 */
	private static final Pattern AMOUNT_PATTERN = Pattern
			.compile("(^|#)(\\d{1,8})(\\d{2})(\\d)>");

	/**
	 * BG/PG NUMBER PATTERN<br/>
	 * <ul>
	 * <li>Start of string OR</li>
	 * <li>&nbsp;&nbsp;&nbsp;&nbsp;></li>
	 * <li>followed by 7 to 8 digits <em>(the
	 * BG/PG account number)</em></li>
	 * <li>followed by a #</li>
	 * <li>followed by 2 digits <em>(the internal
	 * document type)</em></li>
	 * <li>followed by a #</li>
	 * <li>followed by end of string</li>
	 * </ul>
	 * 
	 * Result:<br/>
	 * <ul>
	 * <li>group 1 - Not used</li>
	 * <li>group 2 - BG/PG number</li>
	 * <li>group 3 - Internal document type</li>
	 * </ul>
	 */
	private static final Pattern ACCOUNT_PATTERN = Pattern
			.compile("(^|>)(\\d{7,8})#(\\d{2})#$");

	private String reference;

	private int amount = -1;

	private short amountFractional = -1;

	private String checkDigitAmount;

	private String giroAccount;

	private String internalDocumentType;

	public String getReference() {
		return reference;
	}

	/**
	 * Verifies the reference number with the check digit and sets them if they
	 * match.
	 * 
	 * The Luhn algorithm or modulus-10 algorithm is used for validation.
	 * 
	 * @param reference
	 *            the complete reference number, including the check digit
	 */
	public void setReference(String reference) {
		if (isValidCC(reference)) {
			Log.v(TAG, "Got reference. Check digit valid.");
			this.reference = reference;
		} else
			Log.e(TAG, "Got reference. Check digit invalid.");
	}

	public short getCheckDigitReference() {
		return Short.parseShort(reference.substring(reference.length() - 1));
	}

	public void setAmount(String amount, String amountFractionals,
			String checkDigit) {
		if (isValidCC(amount + amountFractionals + checkDigit)) {
			Log.v(TAG, "Got amount. Check digit valid.");
			this.amount = Integer.parseInt(amount);
			this.amountFractional = Short.parseShort(amountFractionals);
			this.checkDigitAmount = checkDigit;
		} else
			Log.e(TAG, "Got amount. Check digit invalid.");
	}

	public int getAmount() {
		return amount;
	}

	public void setAmountFractional(short amountFractional) {
		this.amountFractional = amountFractional;
	}

	public short getAmountFractional() {
		return amountFractional;
	}

	public String getCompleteAmount() {
		return Integer.toString(amount)
				+ ","
				+ (amountFractional < 10 ? "0" + amountFractional
						: amountFractional);
	}

	public String getCheckDigitAmount() {
		return checkDigitAmount;
	}

	public String getGiroAccount() {
		return giroAccount;
	}

	public void setGiroAccount(String giroAccount) {
		Log.v(TAG, "Got giro account.");
		this.giroAccount = giroAccount;
	}

	public void setInternalDocumentType(String internalDocumentType) {
		this.internalDocumentType = internalDocumentType;
	}

	public String getInternalDocumentType() {
		return internalDocumentType;
	}

	/**
	 * An invoice is considered complete if it contains a reference number,
	 * amount including fractionals, the amount check digit and a giro account.
	 * 
	 * @return true if the invoice is considered complete, otherwise false
	 */
	public boolean isComplete() {
		return reference != null && amount != -1 && amountFractional != -1
				&& checkDigitAmount != null && giroAccount != null;
	}

	/**
	 * Parses the specified input and looks for known fields.
	 * 
	 * @param input
	 *            the {@code String} to parse for invoice fields.
	 * @return the fields found in the specified input.
	 */
	public int parse(String input) {
		int fieldsDecoded = 0;
		/* Look for reference number */
		Matcher m = OCR_PATTERN.matcher(input);
		if (m.find()) {
			setReference(m.group(2));
			fieldsDecoded += REFERENCE_FIELD;
		}

		/* Look for amount */
		m = AMOUNT_PATTERN.matcher(input);
		if (m.find()) {
			setAmount(m.group(2), m.group(3), m.group(4));
			fieldsDecoded += AMOUNT_FIELD;
		}

		/* Look for BG/PG number */
		m = ACCOUNT_PATTERN.matcher(input);
		if (m.find()) {
			setGiroAccount(m.group(2));
			setInternalDocumentType(m.group(3));
			fieldsDecoded += GIRO_ACCOUNT_FIELD + DOCUMENT_TYPE_FIELD;
		}
		return fieldsDecoded;
	}

	private static boolean isValidCC(String num) {

		final int[][] sumTable = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
				{ 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 } };
		int sum = 0, flip = 0;

		for (int i = num.length() - 1; i >= 0; i--)
			sum += sumTable[flip++ & 0x1][Character.digit(num.charAt(i), 10)];
		return sum % 10 == 0;
	}

	public String toString() {
		/*
		 * Sadly, the following toString() method gets messed up when auto
		 * formatting. It returns a String which looks in some way like the
		 * bottom OCR line on a PG/BG invoice.
		 */
		return "#\t"
				+ (reference != null ? reference : "NO REF")
				+ " #\t "
				+ (amount != -1 ? "" + amount : "NO AMOUNT")
				+ " "
				+ (amountFractional != -1 ? (amountFractional < 10 ? "0"
						+ amountFractional : "" + amountFractional) : "XX")
				+ "   " + (checkDigitAmount != null ? checkDigitAmount : "X")
				+ " >\t\t" + (giroAccount != null ? giroAccount : "NO GIRO")
				+ "#"
				+ (internalDocumentType != null ? internalDocumentType : "XX")
				+ "#\t"
				+ (isComplete() ? "Invoice complete" : "Invoice incomplete");
	}

}

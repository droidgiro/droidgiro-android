package com.agiro.scanner.android;

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

	private final String TAG = "aGiro.Invoice";

	/*
	 * The patterns below have been derived from reading the following
	 * documents:
	 * http://www.bgc.se/upload/Gemensamt/Trycksaker/Manualer/BG6070.pdf
	 * http://www
	 * .plusgirot.se/sitemod/upload/root/www_plusgirot_se/pdfer/allmanbeskrivning
	 * /g445_allman_beskrivning_inbetalningservice.pdf
	 */

	/*
	 * OCR PATTERN Start of string followed by H# OR followed by an optional #
	 * (optional because the user might not have included the # left to the OCR
	 * number) followed by 2 to 25 digits (the OCR number) followed by a # NOT
	 * followed by two digits and # (because in that case we have read the BG/PG
	 * account number)
	 * 
	 * Result: group 1 - Not used group 2 - Complete OCR number (including OCR
	 * check digit) group 3 - OCR check digit
	 */
	private static final Pattern OCR_PATTERN = Pattern
			.compile("^(H#|#?)(\\d{1,24}(\\d))#(?!\\d{2}#)");

	/*
	 * AMOUNT PATTERN Start of string OR # followed by 1 to 8 digits (the
	 * amount) followed by 2 digits (šre, the hundredth part of total amount)
	 * followed by 1 digit (check) followed by a >
	 * 
	 * Result: group 1 - Not used group 2 - Amount in whole SEK group 3 - Amount
	 * in šre (fractional part) group 4 - Amount check digit
	 */
	private static final Pattern AMOUNT_PATTERN = Pattern
			.compile("(^|#)(\\d{1,8})(\\d{2})(\\d)>");

	/*
	 * BG/PG NUMBER PATTERN Start of string OR > followed by 7 to 8 digits (the
	 * BG/PG account number) followed by a # followed by 2 digits (the internal
	 * document type) followed by a # followed by end of string
	 * 
	 * Result: group 1 - Not used group 2 - BG/PG number group 3 - Internal
	 * document type
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

	public String getCheckDigitAmount() {
		return checkDigitAmount;
	}

	public String getGiroAccount() {
		return giroAccount;
	}

	public void setGiroAccount(String giroAccount) {
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

	public void decode(String field) {
		/* Look for reference number */
		Matcher m = OCR_PATTERN.matcher(field);
		if (m.find()) {
			setReference(m.group(2));
		}

		/* Look for amount */
		m = AMOUNT_PATTERN.matcher(field);
		if (m.find()) {
			setAmount(m.group(2), m.group(3), m.group(4));
		}

		/* Look for BG/PG number */
		m = ACCOUNT_PATTERN.matcher(field);
		if (m.find()) {
			setGiroAccount(m.group(2));
			setInternalDocumentType(m.group(3));
		}
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
				+ "#\t" + (isComplete() ? "Invoice complete" : "Invoice incomplete");
	}

}

package com.nisovin.magicspells.util;

import java.util.regex.Pattern;

public class RegexUtil {

	public static final Pattern DOUBLE_PATTERN = Pattern.compile("[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*");
	
	public static final Pattern SIMPLE_INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	public static final boolean matches(Pattern pattern, String string) {
		return pattern.matcher(string).matches();
	}
}

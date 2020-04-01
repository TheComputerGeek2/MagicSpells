package com.nisovin.magicjutsus.util;

import java.util.regex.Pattern;

public class RegexUtil {

	public static final Pattern DOUBLE_PATTERN = Pattern.compile("[\\x00-\\x20]*[+-]?(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*");
	
	public static final Pattern SIMPLE_INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	public static final String USERNAME_REGEXP = "[a-zA-Z0-9_]{3,16}";
	public static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEXP);
	
	public static final Pattern BASIC_HEX_PATTERN = Pattern.compile("[0-9A-Fa-f]+");
	public static final Pattern BASIC_DECIMAL_INT_PATTERN = Pattern.compile("[0-9]+");
	
	public static boolean matches(Pattern pattern, String string) {
		return pattern.matcher(string).matches();
	}
	
	public static String[] split(Pattern pattern, String string, int max) {
		return pattern.split(string, max);
	}
	
	public static String replaceAll(Pattern pattern, String string, String replacement) {
		return pattern.matcher(string).replaceAll(replacement);
	}
	
	public static String replaceFirst(Pattern pattern, String string, String replacement) {
		return pattern.matcher(string).replaceFirst(replacement);
	}
	
}

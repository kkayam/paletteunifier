package com.koraykaya.paletteunifier.scan;

import com.koraykaya.paletteunifier.model.TextureRole;

import java.util.regex.Pattern;

/** Guesses which reference (planks, bark, end grain, stripped) a texture path should be matched against. */
public final class RoleClassifier {
	private static final Pattern STRIPPED = Pattern.compile("(^|_|/)stripped(_|/)");
	private static final Pattern BARK = Pattern.compile("(^|_|/)(log|logs|wood|bark|stem|hyphae|trunk|branch)(_|$|/|\\.)");
	private static final Pattern TOP = Pattern.compile("(_top|_end|_top_[0-9]+)(\\.|$)|(^|/)top(_|\\.|$)");

	private RoleClassifier() {}

	public static TextureRole classify(String path) {
		boolean top = TOP.matcher(path).find();
		if (STRIPPED.matcher(path).find()) {
			return top ? TextureRole.STRIPPED_TOP : TextureRole.STRIPPED_SIDE;
		}
		if (BARK.matcher(path).find()) {
			return top ? TextureRole.LOG_TOP : TextureRole.LOG_SIDE;
		}
		return TextureRole.PLANKS;
	}
}

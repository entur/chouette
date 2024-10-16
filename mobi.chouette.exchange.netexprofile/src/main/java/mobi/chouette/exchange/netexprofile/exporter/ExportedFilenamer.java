package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.Line;

import java.text.StringCharacterIterator;
import java.util.regex.Pattern;

public class ExportedFilenamer {

	private static final String UNDERSCORE = "_";
	private static final String DASH = "-";

	/**
	 * Match occurrences of /  .  \  :  <  >  " ' | ? * ; and the space character.
	 */
	private static final Pattern PATTERN_INVALID_CHARACTERS_IN_FILE_NAME = Pattern.compile("[/.\\\\:<>\"'|?*; ]");

	/**
	 * Match occurrences of non-ASCII characters.
	 */

	private static final Pattern PATTERN_NON_ASCII_CHARACTERS = Pattern.compile("[^\\p{ASCII}]");

	public static String createSharedDataFilename(Context context) {
		NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);

		StringBuilder b = new StringBuilder();
		b.append(UNDERSCORE);
		b.append(parameters.getDefaultCodespacePrefix());
		b.append("_shared_data.xml");

		return b.toString();
	}

	public static String createLineFilename(Context context, Line line) {
		NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);

		StringBuilder b = new StringBuilder();
		b.append(parameters.getDefaultCodespacePrefix());
		b.append(UNDERSCORE);
		b.append(line.getObjectId());
		b.append(UNDERSCORE);
		if (line.getNumber() != null) {
			b.append(line.getNumber().replaceAll(UNDERSCORE, DASH));
			b.append(UNDERSCORE);
		}
		if (line.getName() != null) {
			b.append(line.getName());
		} else if (line.getPublishedName() != null) {
			b.append(line.getPublishedName());
		}

		return replaceInvalidFileNameCharacters(removeNonASCIICharacters(replaceNationalCharactersWithASCIIEquivalent(b.toString()))) + ".xml";
	}

	// Convert string to ascii, replacing common non ascii chars with replacements (Å => A etc).
	static String replaceNationalCharactersWithASCIIEquivalent(String s) {
		final StringBuilder sb = new StringBuilder(s.length() * 2);

		final StringCharacterIterator iterator = new StringCharacterIterator(s);

		char ch = iterator.current();

		while (ch != StringCharacterIterator.DONE) {
			if (Character.getNumericValue(ch) > 0) {
				sb.append(ch);
			} else {

				if (Character.toString(ch).equals("Ê")) {
					sb.append("E");
				} else if (Character.toString(ch).equals("È")) {
					sb.append("E");
				} else if (Character.toString(ch).equals("ë")) {
					sb.append("e");
				} else if (Character.toString(ch).equals("é")) {
					sb.append("e");
				} else if (Character.toString(ch).equals("è")) {
					sb.append("e");
				} else if (Character.toString(ch).equals("Â")) {
						sb.append("A");
				} else if (Character.toString(ch).equals("ß")) {
						sb.append("ss");
				} else if (Character.toString(ch).equals("Ç")) {
						sb.append("C");
				} else if (Character.toString(ch).equals("Ö")) {
						sb.append("O");
				} else if (Character.toString(ch).equals("Ó")) {
						sb.append("O");
				} else if (Character.toString(ch).equals("ª")) {
						sb.append("");
				} else if (Character.toString(ch).equals("º")) {
						sb.append("");
				} else if (Character.toString(ch).equals("É")) {
						sb.append("E");
				} else if (Character.toString(ch).equals("Ä")) {
						sb.append("A");
				} else if (Character.toString(ch).equals("Å")) {
						sb.append("A");
				} else if (Character.toString(ch).equals("å")) {
						sb.append("a");
				} else if (Character.toString(ch).equals("ä")) {
						sb.append("a");
				} else if (Character.toString(ch).equals("Ü")) {
						sb.append("U");
				} else if (Character.toString(ch).equals("ö")) {
						sb.append("o");
				} else if (Character.toString(ch).equals("ü")) {
						sb.append("u");
				} else if (Character.toString(ch).equals("á")) {
						sb.append("a");
				} else if (Character.toString(ch).equals("Æ")) {
					sb.append("E");
				} else if (Character.toString(ch).equals("æ")) {
						sb.append("e");
				} else if (Character.toString(ch).equals("Ø")) {
						sb.append("O");
				} else if (Character.toString(ch).equals("ø")) {
						sb.append("o");
				} else {
						sb.append(ch);
				}
			}
			ch = iterator.next();
		}
		return sb.toString();
	}

	static String replaceInvalidFileNameCharacters(String filename) {
		return PATTERN_INVALID_CHARACTERS_IN_FILE_NAME.matcher(filename).replaceAll(DASH);
	}

	static String removeNonASCIICharacters(String filename) {
		return PATTERN_NON_ASCII_CHARACTERS.matcher(filename).replaceAll("");
	}
}

package checks.regex;

import java.util.regex.Pattern;

public class RegexComplexityCheck {

  void noncompliant(String str) {
    str.matches(
      "^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|" + // Noncompliant {{Simplify this regular expression to reduce its complexity from 106 to the 15 allowed.}}
        ("^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|" +
        "^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$"));
  }

  void compliantFromSonar() {
    Pattern.compile("(\\w+)\\s*([<>]?[=]?)\\s*(.*)", Pattern.CASE_INSENSITIVE);
  }

  Pattern compliantFromGuava() {
    String decimal = "(?:\\d++(?:\\.\\d*+)?|\\.\\d++)";
    String completeDec = decimal + "(?:[eE][+-]?\\d++)?[fFdD]?";
    String hex = "(?:\\p{XDigit}++(?:\\.\\p{XDigit}*+)?|\\.\\p{XDigit}++)";
    String completeHex = "0[xX]" + (hex) + "[pP][+-]?\\d++[fFdD]?";
    String fpPattern = "[+-]?(?:NaN|Infinity|" + completeDec + ("|" + completeHex + ")");
    return Pattern.compile((fpPattern));
  }

  Pattern guavaNonCompliantVersion() {
    String fpPattern = "[+-]?(?:NaN|Infinity|(?:\\d++(?:\\.\\d*+)?|\\.\\d++)(?:[eE][+-]?\\d++)?[fFdD]?|" + // Noncompliant {{Simplify this regular expression to reduce its complexity from 58 to the 15 allowed.}}
      "0[xX](?:\\p{XDigit}++(?:\\.\\p{XDigit}*+)?|\\.\\p{XDigit}++)[pP][+-]?\\d++[fFdD]?)";
    return Pattern.compile(fpPattern);
  }

  Pattern guavaNonCompliantVersionWithMultipleParts() {
    String part1 = "NaN|Infinity|(?:\\d++(?:\\.\\d*+)?|" + // Noncompliant {{Simplify this regular expression to reduce its complexity from 30 to the 15 allowed.}}
      "\\.\\d++)(?:[eE][+-]?\\d++)?[fFdD]?";
    String part2 = "0[xX](?:\\p{XDigit}++(?:\\.\\p{XDigit}*+)?|\\.\\p{XDigit}++)[pP][+-]?\\d++[fFdD]?"; // Noncompliant {{Simplify this regular expression to reduce its complexity from 17 to the 15 allowed.}}
    return Pattern.compile("[+-]?(?:" + part1 + "|" + part2 + ")");
  }
}

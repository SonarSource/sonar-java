package checks.regex;

import java.util.regex.Pattern;

public class RegexStackOverflowCheckWithLowStackConsumption {

  Pattern[] patterns = {
    Pattern.compile("(helllllllllllllllllo|wooooooooooooooooooooooorld)*"), // Noncompliant
    Pattern.compile("(.................................................................................................." + // Noncompliant
      "..............................................................|.................................................." +
      "..............................................................................................................)*"),
    Pattern.compile("(hello)(hello|world)*"), // Noncompliant
    Pattern.compile("(hello)(\\1|world)*"), // Noncompliant
    Pattern.compile("(abc|def)*"), // Noncompliant
    Pattern.compile("(ab|cd)*"), // Noncompliant
    Pattern.compile("(...|...)*"), // Noncompliant
    Pattern.compile("(x{42,})*"), // Noncompliant
  };

  // Regex from the .net incident
  private static final Pattern SEQUENCE_POINT = Pattern.compile("\\[(\\d++),\\d++,\\d++,\\d++,(\\d++)]");
  private static final String SEQUENCE_POINTS_GROUP_NAME = "SequencePoints";
  private static final Pattern FILE_COVERAGE = Pattern.compile(
    ".*<script type=\"text/javascript\">\\s*+highlightRanges\\(\\[(?<" + SEQUENCE_POINTS_GROUP_NAME + ">" + SEQUENCE_POINT + "(," + SEQUENCE_POINT + ")*)]\\);\\s*+</script>.*", // Noncompliant
    Pattern.DOTALL);
}

package checks.regex;

import java.util.regex.Pattern;

public class RegexStackOverflowCheckWithMediumStackConsumption {

  Pattern[] patterns = {
    Pattern.compile("(..|..)*"), // Noncompliant
    Pattern.compile("ab(\\1|..)*"), // Noncompliant
    Pattern.compile("(?:(a|b)\\1)*"), // Noncompliant
  };

}

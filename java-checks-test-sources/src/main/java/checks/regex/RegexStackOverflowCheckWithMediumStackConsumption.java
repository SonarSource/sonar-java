package checks.regex;

import java.util.regex.Pattern;

public class RegexStackOverflowCheckWithMediumStackConsumption {

  Pattern[] patterns = {
    Pattern.compile("(ab|cd)*"), // Noncompliant
    Pattern.compile("(..|..)*"), // Noncompliant
    Pattern.compile("(...|...)*"), // Noncompliant
    Pattern.compile("(x{42,})*"), // Noncompliant
  };

}

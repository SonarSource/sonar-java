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

}

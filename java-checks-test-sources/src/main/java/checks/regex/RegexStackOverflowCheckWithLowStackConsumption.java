package checks.regex;

import java.util.regex.Pattern;

public class RegexStackOverflowCheckWithLowStackConsumption {

  Pattern[] patterns = {
    Pattern.compile("(helllllllllllllllllo|wooooooooooooooooooooooorld)*"), // Noncompliant
    Pattern.compile("(.................................................................................................." + // Noncompliant
      "..............................................................|.................................................." +
      "..............................................................................................................)*"),
    Pattern.compile("(hello|world)*"), // Noncompliant
    Pattern.compile("(abc|def)*"), // Noncompliant
  };

}

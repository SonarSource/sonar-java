package checks.regex;

import java.util.regex.Pattern;

public class RegexStackOverflowCheckWithConstantStackConsumption {

  Pattern[] patterns = new Pattern[]{
    Pattern.compile("a*"),
    Pattern.compile("(a)*"),
    Pattern.compile("((?i)a)*"),
    Pattern.compile("[ab]*"),
    Pattern.compile("(ab)\\1*"),
    Pattern.compile("(?s).*"),
    Pattern.compile("(.{42})*"),
    Pattern.compile("[\\s\\S]*"),
    Pattern.compile("(.|\n)?"),
    Pattern.compile("(.|\n)*+"),
    Pattern.compile("(.|\n){1,42}"),
    Pattern.compile("(ax*+)*+"),
  };

}

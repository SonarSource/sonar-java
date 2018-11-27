import java.util.regex.Pattern;

class A {

  private static final String STRCONST = "constant";
  private final String finalField = "";
  private static String staticNotFinal = "";

  private static final Pattern myRegex = Pattern.compile("myRegex"); // Compliant
  private static final Pattern myRegex1 = ((Pattern.compile("myRegex"))); // Compliant
  private static final Pattern myRegex2;
  private final Pattern myRegex3 = Pattern.compile("myRegex"); // Noncompliant
  private static Pattern myRegex4 = Pattern.compile("myRegex"); // Noncompliant
  private static final boolean bool1 = "".matches("myRegex2"); // Compliant
  private static final String str1 = "".replaceFirst("a", "b"); // Compliant
  private static final String str2 = "".replaceAll("a", "b"); // Compliant

  static {
    myRegex2 = ((Pattern.compile("myRegex"))); // Compliant
    Pattern localPattern = Pattern.compile(".*"); // Noncompliant
  }

  void foo(String param) {
    Pattern regex = Pattern.compile("regex1"); // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
    Pattern.compile(STRCONST); // Noncompliant
    Pattern.compile(param);
    Pattern.compile(finalField); // Compliant
    Pattern.compile(staticNotFinal); // Compliant
    Pattern.compile(A.STRCONST); // Noncompliant
    Pattern.compile(this.finalField); // Compliant
    Pattern.compile(A.staticNotFinal); // Compliant
    Pattern regex3 = Pattern.compile(param); // Compliant
    Pattern regex4 = Pattern.compile(param.toString()); // Compliant

    param.matches(param); // Compliant
    param.matches("myRegex2"); // Noncompliant
    param.matches(STRCONST); // Noncompliant
    param.matches(A.STRCONST); // Noncompliant
    param.matches(finalField); // Compliant
    param.matches(staticNotFinal); // Compliant

    param.replaceFirst("a", "b"); // Noncompliant
    param.replaceFirst(STRCONST, "b"); // Noncompliant
    param.replaceFirst(finalField, " "); // Compliant
    param.replaceFirst(staticNotFinal, " "); // Compliant

    param.replaceAll("a", " "); // Noncompliant
    param.replaceAll(STRCONST, "b"); // Noncompliant
    param.replaceAll(finalField, " "); // Compliant
    param.replaceAll(staticNotFinal, " "); // Compliant

    param.split("aa"); // Noncompliant
    param.split(STRCONST); // Noncompliant
    param.split(finalField); // Compliant
    param.split(staticNotFinal); // Compliant

    // RegEx metacharacters
    param.split("$"); // Noncompliant
    param.split("*"); // Noncompliant
    param.split("."); // Noncompliant
    param.split("a"); // Compliant not included in meta characters

    param.split("\2"); // Noncompliant
    param.split("\\"); // Compliant
    param.split("/a"); // Noncompliant
    param.split("\\a"); // Noncompliant

    param.split("\\a"); // Noncompliant
    param.split("\\2"); // Noncompliant
    param.split("\\-"); // Noncompliant
    param.split("\\*"); // Compliant    * is a metacharacter
    param.split("\\."); // Compliant
    param.split("\\|"); // Compliant
    param.split("\\\\"); // Compliant
    param.split("//++"); // Noncompliant
    param.split("\\?q"); // Noncompliant
  }
}

enum E {

  INSTANCE1(Pattern.compile(".*")), // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
  INSTANCE2(COMPILED);

  private final Pattern pattern;
  private static final Pattern COMPILED = Pattern.compile(".*");

  E(Pattern pattern) {
    this.pattern = pattern;
  }
}

// this code does not compile
// but as we are able to parse it, we should not fail during analysis
// (consider SonarLint)
interface I {
  static {
    Pattern.compile(".*"); // Noncompliant
  }
}

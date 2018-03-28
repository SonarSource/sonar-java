import java.util.regex.Pattern;

class A {

  private static final Pattern myRegex = Pattern.compile("myRegex"); // Compliant
  private static final com.google.common.base.Splitter CRITERIA_SPLITTER = com.google.common.base.Splitter.on(Pattern.compile(" and ", Pattern.CASE_INSENSITIVE)); // Compliant

  private final String temp = "";

  public void foo(String str) {

    foo2(Pattern.compile("regex3")); // Noncompliant [[sc=10;ec=35]] {{Refactor this code to use a "static final" Pattern.}}
    Pattern regex = Pattern.compile("regex1"); // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
    Pattern regex2 = Pattern.compile(temp); // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
    java.util.regex.Matcher matcher = regex.matcher("s");
    foo2(Pattern.compile("regex2")); // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
    System.out.println(Pattern.compile("regex2").toString()); // Noncompliant
    // coverage
    foo2(Pattern.compile(null));
    if (str.matches("myRegex2")) { // Noncompliant {{Refactor this code to use a "static final" Pattern.}}
    }
    str.matches(null); // Compliant
  }

  public void foo2(Pattern p) {
    "   ".split(" "); // Compliant
    "   ".replaceAll(" ", "."); // Noncompliant
    "   ".replaceFirst(" ", "."); // Noncompliant

    String temp2 = "";
    temp2.split(" "); // Compliant
    temp2.split("a"); // Compliant
    temp2.replaceAll(" ", "."); // Noncompliant
    temp2.replaceFirst(" ", "."); // Noncompliant

    // cover special cases

    temp2.split("$"); // Noncompliant
    temp2.split("*"); // Noncompliant
    temp2.split(".");  // Noncompliant
    temp2.split("\\"); // Noncompliant
    temp2.split("-"); // Compliant
    String[] address = temp2.split("."); // Noncompliant
    temp2.split("\\!"); // Compliant
    temp2.split("\1"); // Noncompliant
    temp2.split("\\a"); // Noncompliant

    temp2.split("/a"); // Noncompliant

    Pattern regex3 = Pattern.compile(temp2); // Noncompliant
    Pattern regex4 = Pattern.compile(temp2.toString()); // Noncompliant
  }

  public void doingSomething(String stringToMatch) {
    java.util.regex.Matcher matcher = myRegex.matcher("s");
    if (myRegex.matcher(stringToMatch).matches()) { // Compliant
    }

    myRegex.split(" "); // Compliant
    myRegex.matcher(stringToMatch).replaceAll(" "); // Compliant
    myRegex.matcher(stringToMatch).replaceFirst(" "); // Compliant

    new A().foo2(Pattern.compile("regex2")); // Noncompliant
  }

}

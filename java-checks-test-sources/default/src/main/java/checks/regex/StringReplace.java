package checks.regex;

import javax.validation.constraints.Pattern;

public class StringReplace {

  static final String PLANE = "Plane";

  @Pattern(regexp = "foo") // ignored
  String pattern;

  public void foo(String r) {
    String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
    String changed = init.replaceAll("Bob is", "It's"); // Noncompliant [[sc=27;ec=37]] {{Replace this call to "replaceAll()" by a call to the "replace()" method.}}
    changed = init.replaceAll(PLANE, "UFO"); // Noncompliant [[sc=20;ec=30]]
    changed = init.replaceAll("\\.\\.\\.", ";"); // Noncompliant
    changed = init.replaceAll("\\Q...\\E", ";"); // Noncompliant
    changed = init.replaceAll("\\\\", "It's"); // Noncompliant
    changed = init.replaceAll("\\.", "It's"); // Noncompliant
    changed = init.replaceAll("!", "."); // Noncompliant
    changed = init.replaceAll("\n", " "); // Noncompliant
    changed = init.replaceAll("(?i)bird", "bird"); // Compliant
    changed = init.replaceAll("\\w*\\sis", "It's"); // Compliant
    changed = init.replaceAll("\\.{3}", ";"); // Compliant
    changed = init.replaceAll("\\w", "It's"); // Compliant
    changed = init.replaceAll("\\s", "It's"); // Compliant
    changed = init.replaceAll(r, "It's"); // Compliant
    changed = init.replaceAll(".", "It's"); // Compliant
    changed = init.replaceAll("$", "It's"); // Compliant
    changed = init.replaceAll("|", "It's"); // Compliant
    changed = init.replaceAll("(", "It's"); // Compliant
    changed = init.replaceAll("()", "It's"); // Compliant
    changed = init.replaceAll("[", "It's"); // Compliant
    changed = init.replaceAll("[a-z]]", "It's"); // Compliant
    changed = init.replaceAll("{", "It's"); // Compliant
    changed = init.replaceAll("x{3}", "It's"); // Compliant
    changed = init.replaceAll("^", "It's"); // Compliant
    changed = init.replaceAll("?", "It's"); // Compliant
    changed = init.replaceAll("x?", "It's"); // Compliant
    changed = init.replaceAll("*", "It's"); // Compliant
    changed = init.replaceAll("x*", "It's"); // Compliant
    changed = init.replaceAll("+", "It's"); // Compliant
    changed = init.replaceAll("x+", "It's"); // Compliant
    changed = init.replaceAll("\\", "It's"); // Compliant
  }

}

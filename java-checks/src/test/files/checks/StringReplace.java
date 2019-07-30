package test;

public class StringReplace {

  public void foo(String r) {
    String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
    String changed = init.replaceAll("Bob is", "It's"); // Noncompliant [[sc=27;ec=37]] // {{Replace this call to "replaceAll()" by a call to the "replace()" method.}}
    changed = init.replaceAll("\\w*\\sis", "It's"); // Compliant
    changed = init.replaceAll(r, "It's"); // Compliant
    // ".$|()[{^?*+\\"
    changed = init.replaceAll(".", "It's"); // Compliant
    changed = init.replaceAll("$", "It's"); // Compliant
    changed = init.replaceAll("|", "It's"); // Compliant
    changed = init.replaceAll("(", "It's"); // Compliant
    changed = init.replaceAll("[", "It's"); // Compliant
    changed = init.replaceAll("{", "It's"); // Compliant
    changed = init.replaceAll("^", "It's"); // Compliant
    changed = init.replaceAll("?", "It's"); // Compliant
    changed = init.replaceAll("*", "It's"); // Compliant
    changed = init.replaceAll("+", "It's"); // Compliant
    changed = init.replaceAll("\\", "It's"); // Compliant
  }

}

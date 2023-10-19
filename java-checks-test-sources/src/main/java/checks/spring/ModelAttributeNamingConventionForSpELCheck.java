package checks.spring;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

class ModelAttributeNamingConventionForSpELCheck {

  private static final String MY_LEGAL_CONSTANT = "legalName";
  private static final String MY_ILLEGAL_CONSTANT = "a-b";

  public void foo(org.springframework.ui.Model model) {
    model.addAllAttributes(
      Map.of(" m", 42, // Noncompliant
        " a", 22)); // Noncompliant

    model.addAllAttributes(Map.of(MY_ILLEGAL_CONSTANT, 42)); // Noncompliant

    model.addAttribute(File.separator, 42); // Compliant - can not resolve
    model.addAttribute(MY_LEGAL_CONSTANT, 0); // Compliant
    model.addAttribute(MY_ILLEGAL_CONSTANT, 0); // Noncompliant

    model.addAllAttributes(Map.of("m", 42, "a", 22)); // Compliant
    model.addAllAttributes(getMap()); // Compliant

    model.addAllAttributes(Map.of("m", 42, " a", 22)); // Noncompliant

    model.addAllAttributes(Map.ofEntries(Map.entry("m", 42), Map.entry("a", 22))); // Compliant
    model.addAllAttributes(getMap()); // Compliant

    model.addAllAttributes(Map.ofEntries(Map.entry(" m", 42), Map.entry(" a", 22))); // Noncompliant

    model.addAttribute("", 5); // Noncompliant
    model.addAttribute(" a", ""); // Noncompliant [[sc=24;ec=28]] {{Attribute names must begin with a letter (a-z, A-Z), underscore (_), or dollar sign ($) and can be
                                  // followed by letters, digits, underscores, or dollar signs.}}
    model.addAttribute("a-b", ""); // Noncompliant
    model.addAttribute("1c", 42); // Noncompliant

    model.addAttribute("a", 100); // Compliant
    model.addAttribute("b", 42); // Compliant
    model.addAttribute("_c", 7); // Compliant
    model.addAttribute("$d", 8); // Compliant

    model.addAllAttributes(new HashMap<>()); // Compliant - test coverage
  }

  private Map getMap() {
    return Map.of("one", "two");
  }

}

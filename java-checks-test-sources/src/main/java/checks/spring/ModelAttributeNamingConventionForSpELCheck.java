package checks.spring;

import java.util.Map;

class ModelAttributeNamingConventionForSpELCheck {

  public void foo(org.springframework.ui.Model model) {
    model.addAllAttributes(Map.of("m", 42, "a", 22)); // Compliant
    model.addAllAttributes(getMap()); // Compliant

    model.addAllAttributes(Map.of(" m", 42, " a", 22)); // Noncompliant [[sc=35;ec=39]] {{Attribute names must begin with a letter (a-z, A-Z), underscore (_), or dollar sign ($)
                                                        // and can be followed by letters, digits, underscores, or dollar signs.}}

    model.addAttribute("", 5); // Noncompliant
    model.addAttribute(" a", ""); // Noncompliant [[sc=24;ec=28]] {{Attribute names must begin with a letter (a-z, A-Z), underscore (_), or dollar sign ($) and can be
                                  // followed by letters, digits, underscores, or dollar signs.}}
    model.addAttribute("a-b", ""); // Noncompliant
    model.addAttribute("1c", 42); // Noncompliant

    model.addAttribute("a", 100); // Compliant
    model.addAttribute("b", 42); // Compliant
    model.addAttribute("_c", 7); // Compliant
    model.addAttribute("$d", 8); // Compliant
  }

  private Map getMap() {
    return Map.of("one", "two");
  }

}

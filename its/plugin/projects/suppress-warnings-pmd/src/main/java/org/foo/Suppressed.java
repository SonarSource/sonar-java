package org.foo;

@SuppressWarnings("java:S2129") // supressed at class level
public class Suppressed {

  @SuppressWarnings("pmd:StringInstantiation")
  int foo() {
    // raises pmd:StringInstantiation and java:S2129
    String s = new String("orginal");
    return s.length();
  }
}

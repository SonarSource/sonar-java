package org.foo;

public class NotSuppressed {

  int bar() {
    // raises pmd:StringInstantiation and java:S2129
    String s = new String("orginal");
    return s.length();
  }
}

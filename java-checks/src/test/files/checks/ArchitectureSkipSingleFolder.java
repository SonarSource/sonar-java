package org.something.util.foo;

public class ArchitectureConstraint {
  String s; // Noncompliant {{org.something.util.foo.ArchitectureConstraint must not use java.lang.String}}
}

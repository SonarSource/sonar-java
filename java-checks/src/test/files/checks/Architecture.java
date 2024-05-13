package org.sonar.java.checks.targets;

import java.util.regex.Pattern;
import java.io.File;

public class ArchitectureConstraint {
  int a = 1;
  Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint must not use java.util.regex.Pattern}}
//^^^^^^^
  public ArchitectureConstraint() {
    Pattern.compile("*.java");
    Pattern.compile("*");
    new Object() {
      Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint$1 must not use java.util.regex.Pattern}}
    };
    File file = new File("a"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint must not use java.io.File}}
    String separator = File.separator;
  }

  class A {
    Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint$A must not use java.util.regex.Pattern}}
    class AA {
      Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint$A$AA must not use java.util.regex.Pattern}}
      Object obj = new java.lang.Object() {
        Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraint$A$AA$1 must not use java.util.regex.Pattern}}
      };
    }
  }

}

enum ArchitectureConstraintEnum {
  A;
  File file = new File("a"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraintEnum must not use java.io.File}}
  ArchitectureConstraintEnum() {
    Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraintEnum must not use java.util.regex.Pattern}}
  }
}

interface ArchitectureConstraintInterface {
  Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraintInterface must not use java.util.regex.Pattern}}
}

@interface ArchitectureConstraintAnnotation {
  Pattern pattern = Pattern.compile("*.java"); // Noncompliant {{org.sonar.java.checks.targets.ArchitectureConstraintAnnotation must not use java.util.regex.Pattern}}
}

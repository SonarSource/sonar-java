package org.sonar.java.checks.targets;

import java.util.regex.Pattern;
import java.io.File;

public class ArchitectureConstraint {
  Pattern pattern = Pattern.compile("*.java");
  public ArchitectureConstraint() {
    Pattern.compile("*.java");
    Pattern.compile("*");
    File file = new File("a");
    String separator = File.separator;
  }

}

enum ArchitectureConstraintEnum {
  A;
  File file = new File("a");
  ArchitectureConstraintEnum() {
    Pattern.compile("*.java");
  }
}

interface ArchitectureConstraintInterface {
  Pattern pattern = Pattern.compile("*.java");
}

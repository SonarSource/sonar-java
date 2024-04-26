package org.sonar.java.checks.targets;

import java.util.regex.Pattern;
import java.io.File;

public class ArchitectureSelf {

  public ArchitectureSelf() {
    this.foo = "foo";
    method();
  }

  public static void method() {}
}

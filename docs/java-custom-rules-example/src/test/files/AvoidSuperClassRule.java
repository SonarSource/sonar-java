/*
 * Creation : 20 avr. 2015
 */
package org.sonar.samples.java;

import org.slf4j.helpers.NOPMDCAdapter;

/**
 * A class with extends another class outside the JVM but in classpath
 */
public class MyClass extends NOPMDCAdapter { // Noncompliant {{The usage of super class org.slf4j.helpers.NOPMDCAdapter is forbidden}}

  protected MyClass(String name) {
    super();
  }

  /**
   * No issue when there is no super class
   */
  private static class NestedClassWithoutSuperClass { } // Compliant

}

class MyOtherClass extends MyClass { } // Compliant

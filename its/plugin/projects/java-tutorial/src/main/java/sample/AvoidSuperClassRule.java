package sample;

import org.slf4j.Logger;

public abstract class AvoidSuperClassRule implements Logger { // Noncompliant {{The usage of super class org.slf4j.Logger is forbidden}}

  protected AvoidSuperClassRule(String name) {
  }

  /**
   * No issue when there is no super class
   */
  private static class NestedClassWithoutSuperClass { } // Compliant

}

abstract class MyOtherClass extends AvoidSuperClassRule {  // Compliant
  MyOtherClass() {
    super("");
  }
}

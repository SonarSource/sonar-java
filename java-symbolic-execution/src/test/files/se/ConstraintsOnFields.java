package org.foo;

import javax.annotation.Nonnull;

import java.io.IOException;

abstract class A {

  void foo(String a) {
    String b = a;
    if (b == null) {
      b = MY_CONSTANT; // constant can not be null
    }
    // Noncompliant@+1 {{Change this condition so that it does not always evaluate to "false"}}
    if (b == null && testSomething()) { // b can not be null
      foo();
    } else if (MY_CONSTANT.equalsIgnoreCase(b)) {
      bar();
    }

    if (myValue != null) { } // Noncompliant {{Remove this expression which always evaluates to "true"}} [does not compile]
    if (myIntValueFromInteger != null) { } // Noncompliant
    if (myIntegerValueFromInt != null) { } // Noncompliant - Integer, but initializer is int
    if (myNonNullObject == null) { } // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    if (myIntegerValue == null) { } // Compliant

    if(myBool) { // Compliant - Can be true or false as initializer is a method invocation
      if (myBool) { } // Noncompliant
    }

    if (myUnknownBool) { // Compliant - Can be true or false as there is no initializer at declaration site
      if (myUnknownBool) { } // Noncompliant
    }

    if (debug) { } // Compliant - we do not learn any BOOLEAN constraint on final boolean fields (too much noise)
    if (myOtherFixedBool) { } // Compliant - we do not learn any BOOLEAN constraint on final boolean fields (too much noise)

    if (myFixedBool == null) { } // Noncompliant - boolean can not be null

    if (myNullField == null) { } // Noncompliant

    if (MY_OBJECT_CONSTANT == null) { } // Noncompliant
    if (MY_ARRAY_CONSTANT == null) { } // Noncompliant
  }

  static final String MY_CONSTANT = "value";

  static final Object MY_OBJECT_CONSTANT = new Object();
  static final Object[] MY_ARRAY_CONSTANT = new Object[3];
  final Object myNonNullObject = getObject();
  final Object myNullField = null;

  final int myValue = getValue();
  final int myIntValueFromInteger = Integer.valueOf(42);
  final Integer myIntegerValueFromInt = 42;
  final Integer myIntegerValue = Integer.valueOf(42);

  final boolean myUnknownBool;
  final boolean myBool = testSomething();
  final boolean debug = true;
  final boolean myFixedBool = true;
  final boolean myOtherFixedBool = false;

  A() {
    myUnknownBool = true;
  }

  abstract boolean testSomething();
  abstract void foo();
  abstract void bar();
  abstract int getValue();
  @Nonnull
  abstract Object getObject();
}

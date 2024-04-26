package symbolicexecution.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

abstract class InvariantReturnCheckSample {
  private boolean bool;

  private int foo(boolean a) { // Noncompliant {{Refactor this method to not always return the same value.}}
    int b = 12;
    if (a) {
      return b; // flow@issue1 [[order=1]] {{Returned value.}}
    } else if (polop()) {
      return b;  // flow@issue1 [[order=2]] {{Returned value.}}
    }
    return b; // flow@issue1 [[order=3]] {{Returned value.}}
  }

  abstract boolean polop();

  private int foo2(boolean a) {
    int b = 12;
    if (a) {
      return b;
    }
    return b - 1;
  }

  int foo3(boolean a) {
    int b = 12;
    if (a) {
      return b;
    }
    return b;  // false negative : caching of program states because of liveness of params makes the second path unexplored.
  }

  private String foo4(boolean a) { // Noncompliant
    String b = "foo";
    if (a) {
      return b;
    }
    return b;
  }

  void voidMethod() {
    doSomething();
  }

  private int doSomething() {
    System.out.println("");
    return 42;
  }

  private int getConstant2() {
    return 42;
  }

  private int getConstant3(List<String> myList) {
    myList.stream().map(item -> {
      return 0;
    }).collect(Collectors.toList());
    return 0;
  }

  private InvariantReturnCheckSample() {
  }

  String constructComponentName() {
    String base = "";
    int nameCounter = 0;
    synchronized (getClass()) {
      return base + nameCounter++;
    }
  }

  java.util.List<String> f() {
    System.out.println("");
    if (bool) {
      System.out.println("");
    }
    return new ArrayList<String>();
  }

  java.util.Map<String,String> g() {
    java.util.Map<String,String> foo = new java.util.HashMap<String,String>();
    if (bool) {
      return foo;
    }
    return foo;
  }

  private boolean fun(boolean a, boolean b) { // Noncompliant
    if (a) {
      return a;
    }
    if (b) {
      return b;
    }
    return true;
  }

  private boolean fun2(boolean a, boolean b) { // False negative because of constraints on relationship
    if (a) {
      return a;
    }
    if (b) {
      return b;
    }
    return a != b;
  }

  protected boolean isAssignable(final Class<?> dest, final Class<?> source) {

    if (dest.isAssignableFrom(source) ||
      ((dest == Boolean.TYPE) && (source == Boolean.class)) ||
      ((dest == Byte.TYPE) && (source == Byte.class)) ||
      ((dest == Character.TYPE) && (source == Character.class)) ||
      ((dest == Double.TYPE) && (source == Double.class)) ||
      ((dest == Float.TYPE) && (source == Float.class)) ||
      ((dest == Integer.TYPE) && (source == Integer.class)) ||
      ((dest == Long.TYPE) && (source == Long.class)) ||
      ((dest == Short.TYPE) && (source == Short.class))) {
      return (true);
    } else {
      return (false);
    }
  }

  // Example of a method that could raise issue based on returning the same SV AND same constraints on all the returned value.
  int plop(int a, boolean foo) { // Noncompliant
    int b = 0;
    if (a == b) {
      return b;
    }
    int c = b;
    return c;
  }

  java.util.Optional returnEmptyOptional(boolean p1) { // Noncompliant
    if (p1) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.empty();
  }

  private Object fun(Object o) { // Noncompliant
    if (o == null) {
      return o;
    }
    return null;
  }

  private boolean someMethod() {
    try {
      someExceptionalMethod();
    } catch (MyException e) {
      return false;
    }
    return true;
  }

  class MyException extends Exception {
  }

  protected abstract void someExceptionalMethod() throws MyException;

}

class SONARJAVA3155 {
  // java.lang.Void cannot be instaniated, null is the only possible value for this type
  public Void call(boolean a) throws Exception {
    if (a) {
      return null;
    }
    return null;
  }
}

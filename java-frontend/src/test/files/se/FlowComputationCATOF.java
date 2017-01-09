import java.util.*;

class A {

  private Object maybeNull() {
    return new Random().nextBoolean() ? null : new Object();
  }

  private Object getNull() {
    return null;
  }

  // note that 'a' is not tracked properly from binarySV in the if condition
  // that's why we are missing the assignment flow message
  public void catof1() {
    Object a = new Object(); // flow@catof1 {{Constructor call creates 'non-null'}}
    if (a == null) { // Noncompliant [[flows=catof1]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof1 {{Condition is always false}}
      System.out.println();
    }
  }

  public void catof2a() {
    Object foo = maybeNull(); // flow@catof2a {{'maybeNull' returns non-null}}
    foo.getClass();  // Noncompliant
    if (foo == null) {  // Noncompliant [[flows=catof2a]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof2a {{Condition is always false}}
      log(foo.toString());
    } else {
      log(foo.getClass());
    }
  }

  public void catof2b() {
    Object foo = getNull(); // flow@catof2b {{'getNull' returns null}}
    if (foo == null) {  // Noncompliant [[flows=catof2b]] {{Change this condition so that it does not always evaluate to "true"}} flow@catof2b {{Condition is always true}}
      log(foo.toString()); // Noncompliant NPE
    } else {
      log(foo.getClass());
    }
  }

  public void catof3() {
    Object c = null;
    Object foo = null;
    Object b = foo;   // symbol is not tracked properly from binarySV
    if (b == null) { // Noncompliant [[flows=catof3]] flow@catof3 {{Condition is always true}}
      log(foo.toString()); // Noncompliant NPE
    } else {
      log(foo.getClass());
    }
  }

}


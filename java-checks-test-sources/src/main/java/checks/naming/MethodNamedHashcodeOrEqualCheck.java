package checks.naming;

import javax.persistence.Version;

import org.eclipse.jdt.annotation.NonNull;

class AA {
  int hashcode() { // Noncompliant {{Either override Object.hashCode(), or totally rename the method to prevent any confusion.}}
    return 0;
  }

  int hashcode(int a, int b) { // Noncompliant
    return a + b;
  }

  void equal() { // Noncompliant [[sc=8;ec=13]] {{Either override Object.equals(Object obj), or totally rename the method to prevent any confusion.}}
  }

  int equal(Object obj) { // Noncompliant
    return 0;
  }

  public boolean equals(Object obj) {
    return false;
  }

  void tostring() { // Noncompliant [[sc=8;ec=16]] {{Either override Object.toString(), or totally rename the method to prevent any confusion.}}
  }

  public String toString() { // Compliant
    return "";
  }
}

class C implements I {
  public void hashcode() { // Compliant - method is overriding and we can not do anything about its name here
  }

  @Override
  public int foo() {
    return 0;
  }

  @Override
  public int equal() { // Compliant - method is overriding and we can not do anything about its name here
    return 0;
  }
}

class D implements I2 {
  public <T> int hashcode() { // Compliant
    return 0;
  }

  @Override
  public String tostring() { // Compliant
    return null;
  }
}

interface I {
  void hashcode(); // Noncompliant

  int foo();
  
  int equal(); // Noncompliant
}

interface I2 {
  @Version
  @NonNull
  <T> int hashcode(); // Noncompliant
  String tostring(); // Noncompliant
}

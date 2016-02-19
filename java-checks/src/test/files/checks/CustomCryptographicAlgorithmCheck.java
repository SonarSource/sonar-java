import java.security.MessageDigest;
import java.util.Observable;

class A { // Compliant
  void foo() {
    MessageDigest md = new MessageDigest() { // Noncompliant [[sc=28;ec=41]] {{Use a standard algorithm instead of creating a custom one.}}

    };
  }
}

abstract class B extends MessageDigest { // Noncompliant [[sc=16;ec=17]] {{Use a standard algorithm instead of creating a custom one.}}
  protected B(String algorithm) {
    super(algorithm);
  }
}

abstract class C extends java.security.MessageDigest { // Noncompliant
  protected C(String algorithm) {
    super(algorithm);
  }
}

abstract class D extends B { // Noncompliant
  protected D(String algorithm) {
    super(algorithm);
  }
}

class E extends Observable { // Compliant
}
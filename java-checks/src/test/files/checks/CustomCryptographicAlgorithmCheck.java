import java.security.MessageDigest;
import java.util.Observable;

class A { // Compliant
}

class B extends MessageDigest { // Noncompliant

  protected B(String algorithm) {
    super(algorithm);
  }

  @Override
  protected void engineUpdate(byte input) {
  }

  @Override
  protected void engineUpdate(byte[] input, int offset, int len) {
  }

  @Override
  protected byte[] engineDigest() {
    return null;
  }

  @Override
  protected void engineReset() {
  }
}

class C extends java.security.MessageDigest { // Noncompliant

  protected C(String algorithm) {
    super(algorithm);
  }

  @Override
  protected void engineUpdate(byte input) {
  }

  @Override
  protected void engineUpdate(byte[] input, int offset, int len) {
  }

  @Override
  protected byte[] engineDigest() {
    return null;
  }

  @Override
  protected void engineReset() {
  }
}

class D extends Observable { // Compliant
}
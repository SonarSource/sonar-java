import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import javax.crypto.spec.IvParameterSpec;

class A {

  SecureRandom random = new SecureRandom();
  private byte[] bytes2 = "111".getBytes();
  {
    random.nextBytes(bytes2);
  }
  IvParameterSpec iv = new IvParameterSpec(bytes2); // Compliant FN, will be fixed with SE engine

  void foo1()
    throws Exception {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
  }

  void foo2() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant [[sc=26;ec=52]] {{Use a dynamically-generated, random IV.}}
  }

  void foo3() {
    IvParameterSpec iv = new IvParameterSpec(bytes2); // Noncompliant FN, will be fixed with SE engine
  }

  void foo4() throws UnsupportedEncodingException {
    A a = new A();
    a.bytes2 = "111".getBytes("UTF-8");
    random.nextBytes(a.bytes2);
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Compliant
  }

  void foo5() throws UnsupportedEncodingException {
    A a = new A();
    a.bytes2 = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(a.bytes2); // Noncompliant
  }

  void foo6() throws UnsupportedEncodingException {
    IvParameterSpec iv = new IvParameterSpec("111".getBytes("UTF-8")); // Noncompliant
  }

  void foo7(byte[] bytes) {
    IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
  }

  void foo8() throws UnsupportedEncodingException {
    if (true) {
      byte[] bytes = "111".getBytes("UTF-8");
      random.nextBytes(bytes);
      IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
    }
  }

  void foo9() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    if (true) {
      IvParameterSpec iv = new IvParameterSpec(bytes); // Compliant
    }
  }

  void foo10() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    if (true) {
      IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
    }
  }

  void foo11() throws UnsupportedEncodingException {
    if (true) {
      byte[] bytes = "111".getBytes("UTF-8");
      IvParameterSpec iv = new IvParameterSpec(bytes); // Noncompliant
    }
  }

  void foo12() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    IvParameterSpec iv = new IvParameterSpec(null); // Noncompliant Coverage reasons
  }

  void foo13() throws UnsupportedEncodingException {
    byte[] bytes = "111".getBytes("UTF-8");
    random.nextBytes(bytes);
    byte[] bytes2 = "111222".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes2); // Noncompliant Coverage reasons
  }
}
interface I {
  Runnable r = () -> {
    byte[] bytes = "111".getBytes("UTF-8");
    IvParameterSpec iv = new IvParameterSpec(bytes);
  };
}

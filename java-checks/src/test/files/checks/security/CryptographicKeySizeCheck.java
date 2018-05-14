import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;

class A {

  public KeyGenerator keyG;
  public KeyPairGenerator keyPairG = KeyPairGenerator.getInstance("RSA");;
  private static final KeyGenerator keyG2 = KeyGenerator.getInstance("Blowfish");

  public void foo1() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64);  // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo2() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    int x = 64;
    keyGen.init(x); // FN
  }

  public void foo3() throws NoSuchAlgorithmException {
    String algorithm = "Blowfish";
    KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
    keyGen.init(64);  // FN
  }

  public void foo4() throws NoSuchAlgorithmException {
    KeyGenerator keyGen;
    keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64);  // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo5() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("Blowfish");
    keyG.init(64);  // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo6() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("Blowfish");
    keyG.init(128);  // Compliant
  }

  public void foo7() {
    keyG2.init(64);  // FN
  }

  public void foo() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    KeyGenerator keyGen2 = createGen();

    keyGen.init(64);  // Noncompliant

    keyGen2.init(96);  // FN

    KeyGenerator keyGen3 = KeyGenerator.getInstance("Blowfish");
    keyGen3.init(64);  // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo8() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("AES");
    keyG.init(64);  // Compliant
  }

  private KeyGenerator createGen() {
    return KeyGenerator.getInstance("Blowfish");
  }

}

class B {
  public KeyPairGenerator keyG;

  public void foo() {
    A a = new A();
    a.keyG = KeyGenerator.getInstance("Blowfish");
    a.keyG.init(64);    // Noncompliant
  }

  public void foo1() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024);;  // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void foo2() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    int x = 64;
    keyGen.initialize(x); // FN
  }

  public void foo3() throws NoSuchAlgorithmException {
    String algorithm = "RSA";
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
    keyGen.initialize(64);  // FN
  }

  public void foo4() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen;
    keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024);  // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void foo5() throws NoSuchAlgorithmException {
    keyG = KeyPairGenerator.getInstance("RSA");
    keyG.initialize(64);  // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void foo6() throws NoSuchAlgorithmException {
    keyG = KeyPairGenerator.getInstance("RSA");
    keyG.initialize(2048);  // Compliant
  }

  public void foo7() {
    A a = new A();
    a.keyPairG.initialize(1028);    // FN
  }

  public void foo8() {
    A a = new A();
    a.keyPairG = KeyPairGenerator.getInstance("RSA");
    a.keyPairG.initialize(1028);    // Noncompliant
  }

  public void foo9() throws NoSuchAlgorithmException {
    keyG = KeyPairGenerator.getInstance("DSA");
    keyG.initialize(64);  // Compliant
  }

}

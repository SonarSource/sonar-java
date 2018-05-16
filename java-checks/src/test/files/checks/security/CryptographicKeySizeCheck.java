import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

class A {

  public KeyGenerator keyG;
  public KeyPairGenerator keyPairG;
  private KeyGenerator keyG2;

  public A() throws NoSuchAlgorithmException {
    keyG2 = KeyGenerator.getInstance("Blowfish");
    keyPairG = KeyPairGenerator.getInstance("RSA");
  }

  public void foo1() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Noncompliant [[sc=5;ec=20]] {{Use a key length of at least 128 bits.}}
  }

  public void foo2() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    int x = 64;
    keyGen.init(x); // FN requires following variable's values with SE-based engine
  }

  public void foo3() throws NoSuchAlgorithmException {
    String algorithm = "Blowfish";
    KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
    keyGen.init(64); // FN requires following variable's values with SE-based engine
    keyGen.getAlgorithm();
  }

  public void foo4() throws NoSuchAlgorithmException {
    KeyGenerator keyGen;
    keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo5() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("Blowfish");
    keyG.init(64); // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo6() {
    keyG2.init(64); // FN requires following variable's values with SE-based engine
  }

  public void foo() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    KeyGenerator keyGen2 = createGen();

    keyGen.init(64); // Noncompliant

    keyGen2.init(96); // FN requires following variable's values with SE-based engine

    KeyGenerator keyGen3 = KeyGenerator.getInstance("Blowfish");
    keyGen3.init(64); // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void foo7() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("AES");
    keyG.init(64); // Compliant
  }

  private KeyGenerator createGen() throws NoSuchAlgorithmException {
    return KeyGenerator.getInstance("Blowfish");
  }

  public void foo8() {
    Runnable task2 = () -> {
      KeyGenerator keyGen;
      try {
        keyGen = KeyGenerator.getInstance("Blowfish");
        keyGen.init(64); // Noncompliant
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
    };
  }

  public void foo9() throws NoSuchAlgorithmException {
    new Runnable() {
      @Override
      public void run() {
        KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
        keyGen.init(64); // Noncompliant
      }
    };
  }

  public void foo10() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    KeyGenerator keyGen2 = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Compliant
  }
}

class B {

  public void foo() throws NoSuchAlgorithmException {
    A a = new A();
    a.keyG = KeyGenerator.getInstance("Blowfish");
    a.keyG.init(64); // Noncompliant
  }

  public void foo1() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void foo2() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    int x = 64;
    keyGen.initialize(x); // FN requires following variable's values with SE-based engine
  }

  public void foo3() throws NoSuchAlgorithmException {
    String algorithm = "RSA";
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
    keyGen.initialize(64); // FN requires following variable's values with SE-based engine
  }

  public void foo4() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen;
    keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void foo5() throws NoSuchAlgorithmException {
    KeyPairGenerator keyG;
    keyG = KeyPairGenerator.getInstance("RSA");
    keyG.initialize(2048); // Compliant
  }

  public void foo6() throws NoSuchAlgorithmException {
    A a = new A();
    a.keyPairG.initialize(1024); // FN requires following variable's values with SE-based engine
  }

  public void foo7() throws NoSuchAlgorithmException {
    A a = new A();
    a.keyPairG = KeyPairGenerator.getInstance("RSA");
    a.keyPairG.initialize(1024); // Noncompliant
  }

  public void foo8() throws NoSuchAlgorithmException {
    KeyPairGenerator keyG;
    keyG = KeyPairGenerator.getInstance("DSA");
    keyG.initialize(64); // Compliant
  }

  public void foo9() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance("DSA");
    keyGen.initialize(1024); // FN requires following variable's values with SE-based engine
  }
}

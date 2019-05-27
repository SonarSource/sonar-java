import B.Blowfish;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

class Blowfish {

  KeyGenerator keyG;
  private static final Integer CONSTANT_INT = 64;

  public Blowfish() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("Blowfish");
  }

  public void key_variable() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Noncompliant [[sc=5;ec=20]] {{Use a key length of at least 128 bits.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(128); // Compliant
  }

  public void identifier_parameter() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    int x = 64;
    keyGen.init(x); // FN requires following variable's values with SE-based engine
  }

  public void identifier_parameter2() throws NoSuchAlgorithmException {
    String algorithm = "Blowfish";
    KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
    keyGen.init(64); // Noncompliant
  }

  public void key_assignment() throws NoSuchAlgorithmException {
    KeyGenerator keyGen;
    keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Noncompliant {{Use a key length of at least 128 bits.}}
  }

  public void key_assignment_compliant() throws NoSuchAlgorithmException {
    KeyGenerator keyGen;
    keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(128); // Compliant
  }

  public void false_negative() {
    keyG.init(64); // FN requires following variable's values with SE-based engine
  }

  public void different_algorithm() throws NoSuchAlgorithmException {
    keyG = KeyGenerator.getInstance("AES");
    keyG.init(64); // Compliant
  }

  private KeyGenerator createGen() throws NoSuchAlgorithmException {
    return KeyGenerator.getInstance("Blowfish");
  }

  public void false_negative2() throws NoSuchAlgorithmException {
    keyG = createGen();
    keyG.init(64); // FN requires following variable's values with SE-based engine
  }

  public void lambda_expr() {
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

  public void anonymous_class() {
    new Runnable() {
      @Override
      public void run() {
        KeyGenerator keyGen;
        try {
          keyGen = KeyGenerator.getInstance("Blowfish");
          keyGen.init(64); // Noncompliant
        } catch (NoSuchAlgorithmException e) {
          e.printStackTrace();
        }
      }
    };
  }

  public void false_positive() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    KeyGenerator keyGen2 = KeyGenerator.getInstance("Blowfish");
    keyGen.init(64); // Noncompliant FP
  }

  public void constant_key_value() {
    KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
    keyGen.init(CONSTANT_INT); // FN requires following variable's values with SE-based engine
  }

  public void big_key_value() {
    KeyGenerator keyG;
    keyG = KeyGenerator.getInstance("Blowfish");
    keyG.init(256); // Compliant
  }
}

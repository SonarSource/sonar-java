import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

class RSA {

  KeyPairGenerator keyPairG;
  private static final Integer CONSTANT_INT = 1024;

  RSA() throws NoSuchAlgorithmException{
   keyPairG = KeyPairGenerator.getInstance("RSA");
  }
  public void key_variable() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant [[sc=5;ec=28]]  {{Use a key length of at least 2048 bits.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048); // Compliant
  }

  public void identifier_parameter() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    int x = 1024;
    keyGen.initialize(x); // FN requires following variable's values with SE-based engine
  }

  public void identifier_parameter2() throws NoSuchAlgorithmException {
    String algorithm = "RSA";
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
    keyGen.initialize(1024); // FN requires following variable's values with SE-based engine
  }

  public void key_assignment() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen;
    keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant {{Use a key length of at least 2048 bits.}}
  }

  public void key_assignment_compliant() throws NoSuchAlgorithmException {
    KeyPairGenerator keyG;
    keyG = KeyPairGenerator.getInstance("RSA");
    keyG.initialize(2048); // Compliant
  }

  public void false_negative() throws NoSuchAlgorithmException {
    RSA a = new RSA();
    a.keyPairG.initialize(1024); // FN requires following variable's values with SE-based engine
  }

  public void false_negative2() throws NoSuchAlgorithmException {
    keyPairG = KeyPairGenerator.getInstance("DSA");
    KeyPairGenerator keyPairG2 = KeyPairGenerator.getInstance("RSA");
    keyPairG.initialize(1024); // Noncompliant FP requires following variable's values with SE-based engine
  }

  public void different_algorithm() throws NoSuchAlgorithmException {
    KeyPairGenerator keyG;
    keyG = KeyPairGenerator.getInstance("DSA");
    keyG.initialize(1024); // Compliant
  }

  public void constant_key_value() {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(CONSTANT_INT); // FN requires following variable's values with SE-based engine
  }

  public void big_key_value() {
    KeyPairGenerator keyG;
    keyG = KeyPairGenerator.getInstance("RSA");
    keyG.initialize(3000); // Compliant
  }
}

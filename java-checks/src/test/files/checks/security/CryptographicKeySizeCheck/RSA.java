import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

class RSA {

  public void key_variable() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant [[sc=5;ec=28]]  {{Use a key length of at least 2048 bits.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048); // Compliant
  }

  public void report_twice() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant
    keyGen.initialize(1023); // Noncompliant
  }
}
interface I {
  Runnable r = () -> {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
  };
}

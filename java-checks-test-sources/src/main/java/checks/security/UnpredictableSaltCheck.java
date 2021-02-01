package checks.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.PBEKeySpec;

public class UnpredictableSaltCheck {

  public void method(byte[] salt, String passwordToHash) throws NoSuchAlgorithmException {

    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(salt); // Compliant

  }

  public void method2(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(); // Noncompliant {{Add an unpredictable salt value to this hash.}}
  }

  public void method22222(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(); // Compliant
    
    md.getAlgorithm();
    
    new MyMessageDigest("").update(passwordToHash.getBytes());
  }

  public void method3(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant
  }

  public void method4(char[] chars, byte[] salt) throws NoSuchAlgorithmException {
    PBEKeySpec spec = new PBEKeySpec(chars); // Noncompliant
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
  }

  public void method5(char[] chars, String str) throws NoSuchAlgorithmException {
    byte[] salt = "notrandom".getBytes();
    PBEKeySpec spec = new PBEKeySpec(chars, salt, 1); // Noncompliant {{Make this salt unpredictable.}}
    
    byte[] salt1 = this.secureSalt();
    PBEKeySpec spec2 = new PBEKeySpec(chars, salt1, 2); // Compliant

    byte[] salt1111 = secureSalt();
    PBEKeySpec spec22222 = new PBEKeySpec(chars, salt1111, 2); // Compliant

    byte[] salt2 = this.secureSalt();
    PBEKeySpec spec3 = new PBEKeySpec(chars, secureSalt(), 2); // Compliant

    byte[] salt3 = new byte[]{};
    PBEKeySpec spec4 = new PBEKeySpec(chars, salt3, 2); // Compliant

    PBEKeySpec spec5 = new PBEKeySpec(chars, "notrandom".getBytes(), 2); // Noncompliant {{Make this salt unpredictable.}}
    PBEKeySpec spec5555 = new PBEKeySpec(chars, ("notrandom".getBytes()), 2); // Noncompliant {{Make this salt unpredictable.}}
    
    PBEKeySpec spec6 = new PBEKeySpec(chars, new MyMessageDigest("").engineDigest(), 2); // Compliant
    PBEKeySpec spec666 = new PBEKeySpec(chars, str.getBytes(), 2); // Compliant
  }

  private byte[] secureSalt() {
    return new byte[0];
  }
}

class MyMessageDigest extends MessageDigest {

  /**
   * Creates a message digest with the specified algorithm name.
   *
   * @param algorithm the standard name of the digest algorithm.
   *                  See the MessageDigest section in the <a href=
   *                  "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
   *                  Java Security Standard Algorithm Names Specification</a>
   *                  for information about standard algorithm names.
   */
  protected MyMessageDigest(String algorithm) {
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
    return new byte[0];
  }

  @Override
  protected void engineReset() {

  }
  
  public void test(String passwordToHash) {
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = digest(); // Compliant

  }
}

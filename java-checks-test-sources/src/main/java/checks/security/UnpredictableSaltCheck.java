package checks.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.PBEKeySpec;

public class UnpredictableSaltCheck {

  public void testDigestWithParamAndOneUpdate(byte[] salt, String passwordToHash) throws NoSuchAlgorithmException {

    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(salt); // Compliant

  }

  public void testDigestWithOneUpdateAndWithoutParams(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(); // Noncompliant [[sc=29;ec=40]] {{Add an unpredictable salt value to this hash.}}
  }

  public void testDigestWithMultipleUpdates(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(); // Compliant
    
    md.getAlgorithm();
    
    new MyMessageDigest("").update(passwordToHash.getBytes());
  }

  public void testDigestWithoutUpdate(String passwordToHash) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant
  }

  public void testPBESpec(char[] chars, byte[] salt) throws NoSuchAlgorithmException {
    PBEKeySpec spec = new PBEKeySpec(chars); // Noncompliant [[sc=23;ec=44]]
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
  }

  public void testNonRandomPBESpec(char[] chars, String str) throws NoSuchAlgorithmException {
    byte[] salt = "notrandom".getBytes(); // secondary location
    PBEKeySpec spec = new PBEKeySpec(chars, salt, 1); // Noncompliant [[sc=23;ec=53;secondary=-1]] {{Make this salt unpredictable.}}
    
    final byte[] finalSalt = "notrandom".getBytes(); //secondary location
    PBEKeySpec specFinal = new PBEKeySpec(chars, finalSalt, 1); // Noncompliant [[sc=28;ec=63;secondary=-1]] {{Make this salt unpredictable.}}
    
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
  
  public void testUpdateMethodInsideDigest(String passwordToHash) {
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = digest(); // Compliant

  }
}

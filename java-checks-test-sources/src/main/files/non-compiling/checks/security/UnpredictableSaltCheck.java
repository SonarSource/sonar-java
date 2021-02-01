package checks.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.PBEKeySpec;

public class UnpredictableSaltCheck {
  
  private static byte[] sSalt = new byte[]; 

  public void method(byte[] salt, String passwordToHash) throws NoSuchAlgorithmException {
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));
    byte[] hashedPassword = md.digest(salt); // Compliant

  }
  
  public void method4(char[] chars) throws NoSuchAlgorithmException {
    PBEKeySpec spec = new PBEKeySpec(chars); // Noncompliant
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
    PBEKeySpec spec2 = new PBEKeySpec(chars, salt(), 1); // Compliant 
    PBEKeySpec spec2 = new PBEKeySpec(chars, sSalt, 1); // Compliant 
  }

  public void method4(char[] chars) throws NoSuchAlgorithmException {
    var salt = new byte[]{};
    PBEKeySpec spec = new PBEKeySpec(chars); // Noncompliant
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
    PBEKeySpec spec2 = new PBEKeySpec(chars, salt(), 1); // Compliant 
  }
}

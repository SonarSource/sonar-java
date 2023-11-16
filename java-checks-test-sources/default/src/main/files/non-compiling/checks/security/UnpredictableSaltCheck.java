package checks.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class UnpredictableSaltCheck {
  
  private static byte[] sSalt = new byte[]; 

  public void checkPBEKeySpecWithUnknownSalt(char[] chars) throws NoSuchAlgorithmException {
    PBEKeySpec spec = new PBEKeySpec(chars);
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
    PBEKeySpec spec2 = new PBEKeySpec(chars, salt(), 1); // Compliant 
  }

  public void checkPBEKeySpec(char[] chars) throws NoSuchAlgorithmException {
    var salt = new byte[]{};
    PBEKeySpec spec = new PBEKeySpec(chars);
    PBEKeySpec spec1 = new PBEKeySpec(chars, salt, 1); // Compliant 
    PBEKeySpec spec2 = new PBEKeySpec(chars, salt(), 1); // Compliant 
    PBEKeySpec spec3 = new PBEKeySpec(chars, sSalt, 1); // Compliant 

    new PBEParameterSpec("notrandom".getBytes(), 10000);  // Noncompliant
    new PBEParameterSpec(sSalt, 10000);  // Compliant
  }

}

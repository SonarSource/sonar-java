package checks.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

  @RequestMapping(value = "/s2053/sha/noncompliant/utilrandom", method = {RequestMethod.GET})
  public String noncompliantsha2053utilrandom() throws NoSuchAlgorithmException {

    String passwordToHash = "password";

    MessageDigest md = MessageDigest.getInstance("SHA-512");

    Random random = new Random(); // secondary location
    byte[] randSalt = new byte[16];
    random.nextBytes(randSalt); // secondary location

    md.update(randSalt); // secondary location

    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant [[secondary=46,48,50]]
    System.out.println("hashedPassword = "+hashedPassword.toString());

    byte[] randSalt2 = new byte[16];
    rand.nextBytes(randSalt2); // 

    MessageDigest md2 = MessageDigest.getInstance("SHA-512");
    md2.update(randSalt2); // 

    byte[] hashedPassword2 = md2.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Compliant
    System.out.println("hashedPassword = "+hashedPassword2.toString());

    return "thymeleaf/welcome";
  }
}

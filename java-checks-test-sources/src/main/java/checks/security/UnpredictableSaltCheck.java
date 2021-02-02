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
    PBEKeySpec spec = new PBEKeySpec(chars, salt, 1); // Noncompliant [[secondary=54]] {{Make this salt unpredictable.}}
    
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

@Controller
class noncompliant2053 {

  public byte[] getNonRandomSalt() {
    return "notrandom".getBytes();
  }

  // http://localhost:8080/s2053/sha/noncompliant/nosalt

  @RequestMapping(value = "/s2053/sha/noncompliant/nosalt", method = {RequestMethod.GET})
  public String noncompliantsha2053nosalt() throws NoSuchAlgorithmException {

    String passwordToHash = "password";

    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(passwordToHash.getBytes(StandardCharsets.UTF_8));

    byte[] hashedPassword = md.digest(); // Noncompliant
    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }


  // http://localhost:8080/s2053/sha/noncompliant/constantsalt

  @RequestMapping(value = "/s2053/sha/noncompliant/constantsalt", method = {RequestMethod.GET})
  public String noncompliantsha2053constantsalt() throws NoSuchAlgorithmException {

    String passwordToHash = "password";

    MessageDigest md = MessageDigest.getInstance("SHA-512");

    byte[] salt = "notsecure".getBytes(); // secondary location
    md.update(salt); // secondary location

    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant [[secondary=159,158]]
    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }


  // http://localhost:8080/s2053/sha/noncompliant/utilrandom

  @RequestMapping(value = "/s2053/sha/noncompliant/utilrandom", method = {RequestMethod.GET})
  public String noncompliantsha2053utilrandom(Random rand) throws NoSuchAlgorithmException {

    String passwordToHash = "password";

    MessageDigest md = MessageDigest.getInstance("SHA-512");

    Random random = new Random(); // secondary location
    byte[] randSalt = new byte[16];
    random.nextBytes(randSalt); // secondary location

    md.update(randSalt); // secondary location

    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant [[secondary=181,179,177]]
    System.out.println("hashedPassword = "+hashedPassword.toString());

    byte[] randSalt2 = new byte[16];
    rand.nextBytes(randSalt2); // secondary location

    MessageDigest md2 = MessageDigest.getInstance("SHA-512");
    md2.update(randSalt2); // secondary location

    byte[] hashedPassword2 = md2.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Noncompliant [[secondary=171,187,190]]
    System.out.println("hashedPassword = "+hashedPassword2.toString());

    return "thymeleaf/welcome";
  }


  // http://localhost:8080/s2053/pbekeyspec/constantsalt/noncompliant

  @RequestMapping(value = "/s2053/pbekeyspec/constantsalt/noncompliant", method = {RequestMethod.GET})
  public String noncompliantpbekeyspecconstantsaltsalt2053() throws NoSuchAlgorithmException, InvalidKeySpecException {

    String passwordToHash = "password";
    int iterations = 1000;
    char[] chars = passwordToHash.toCharArray();
    byte[] salt = this.getNonRandomSalt();

    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8); // FN : we don't have interprocedural analysis
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    byte[] hashedPassword = skf.generateSecret(spec).getEncoded();

    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }

  // http://localhost:8080/s2053/pbekeyspec/nosalt/noncompliant

  @RequestMapping(value = "/s2053/pbekeyspec/nosalt/noncompliant", method = {RequestMethod.GET})
  public String noncompliantpbekeyspecnosalt2053() throws NoSuchAlgorithmException, InvalidKeySpecException {

    String passwordToHash = "password";
    char[] chars = passwordToHash.toCharArray();

    // will fail at the run time, this algorithm (pbkdf2) needs a salt to work
    PBEKeySpec spec = new PBEKeySpec(chars); // Noncompliant

    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    byte[] hashedPassword = skf.generateSecret(spec).getEncoded();

    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }
}

@Controller
class compliant2053 {

  public byte[] getSaltFromSha1PRNG() throws NoSuchAlgorithmException {
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    byte[] salt = new byte[16];
    sr.nextBytes(salt);
    return salt;
  }

  // http://localhost:8080/s2053/sha/compliant

  @RequestMapping(value = "/s2053/sha/compliant", method = {RequestMethod.GET})
  public String compliantsha2053() throws NoSuchAlgorithmException {

    String passwordToHash = "password";

    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);

    MessageDigest md = MessageDigest.getInstance("SHA-512");
    md.update(salt);

    byte[] hashedPassword = md.digest(passwordToHash.getBytes(StandardCharsets.UTF_8)); // Compliant
    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }
  
  // http://localhost:8080/s2053/pbekeyspec/compliant

  @RequestMapping(value = "/s2053/pbekeyspec/compliant", method = {RequestMethod.GET})
  public String compliantpbekeyspec2053() throws NoSuchAlgorithmException, InvalidKeySpecException {

    String passwordToHash = "password";
    int iterations = 1000;
    char[] chars = passwordToHash.toCharArray();
    byte[] salt = this.getSaltFromSha1PRNG();

    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8); // Compliant
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    byte[] hashedPassword = skf.generateSecret(spec).getEncoded();

    System.out.println("hashedPassword = "+hashedPassword.toString());

    return "thymeleaf/welcome";
  }
}

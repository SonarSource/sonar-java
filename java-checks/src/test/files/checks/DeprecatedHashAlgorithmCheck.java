import java.security.MessageDigest;
import org.apache.commons.codec.digest.DigestUtils;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

class A {
  void myMethod(String algorithm) {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD5"); // Noncompliant
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant
    md = MessageDigest.getInstance("SHA-256");
    md = org.apache.commons.codec.digest.DigestUtils.getDigest("MD5"); // Noncompliant
    md = DigestUtils.getDigest("SHA-1"); // Noncompliant
    md = DigestUtils.getDigest("SHA-256");
    md = DigestUtils.getMd5Digest(); // Noncompliant
    md = DigestUtils.getShaDigest(); // Noncompliant
    md = DigestUtils.getSha1Digest(); // Noncompliant
    md = DigestUtils.getSha256Digest();
    DigestUtils.md5(""); // Noncompliant
    DigestUtils.md5Hex(""); // Noncompliant
    DigestUtils.sha1(""); // Noncompliant
    DigestUtils.sha1Hex(""); // Noncompliant
    DigestUtils.sha(""); // Noncompliant
    DigestUtils.shaHex(""); // Noncompliant
    DigestUtils.sha256("");
    DigestUtils.sha256Hex("");
    md = MessageDigest.getInstance(algorithm);
    md = DigestUtils.getDigest(algorithm);
    md5Hex("");
  }
  
}
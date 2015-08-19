import java.security.MessageDigest;
import java.security.Provider;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

class A {
  void myMethod(String algorithm, Provider provider, Properties props) {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD5"); // Noncompliant {{Use a stronger encryption algorithm than MD5.}}
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant {{Use a stronger encryption algorithm than SHA-1.}}
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
    md5Hex(""); // Noncompliant
    com.google.common.hash.Hashing.md5(); // Noncompliant
    com.google.common.hash.Hashing.sha1(); // Noncompliant
    com.google.common.hash.Hashing.sha256();
    md = MessageDigest.getInstance("MD5", provider); // Noncompliant
    md = MessageDigest.getInstance("SHA1", "provider"); // Noncompliant
    md = MessageDigest.getInstance("sha-1", "provider"); // Noncompliant
    
    String myAlgo = props.getProperty("myCoolAlgo", "SHA1");
    
    md = MessageDigest.getInstance(myAlgo, provider); // Noncompliant
    md = MessageDigest.getInstance(getAlgo(), provider);
    md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo", "SHA-1")); // Noncompliant
    md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo"));
  }
  
  private String getAlgo() {
    return null;
  }
  
}

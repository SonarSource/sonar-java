import java.security.MessageDigest;
import java.security.Provider;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

class A {
  void myMethod(String algorithm, Provider provider, Properties props) {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD2"); // Noncompliant [[sc=24;ec=35]] {{Use a stronger hashing algorithm than MD2.}}
    md = MessageDigest.getInstance("MD4"); // Noncompliant {{Use a stronger hashing algorithm than MD4.}}
    md = MessageDigest.getInstance("MD6"); // Noncompliant {{Use a stronger hashing algorithm than MD6.}}
    md = MessageDigest.getInstance("MD5"); // Noncompliant [[sc=24;ec=35]] {{Use a stronger hashing algorithm than MD5.}}
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
    md = MessageDigest.getInstance("HAVAL-128"); // Noncompliant {{Use a stronger hashing algorithm than HAVAL-128.}}
    md = MessageDigest.getInstance("HMAC-MD5"); // Noncompliant {{Use a stronger hashing algorithm than MD5.}}
    md = MessageDigest.getInstance("RIPEMD"); // Noncompliant {{Use a stronger hashing algorithm than RIPEMD.}}
    md = MessageDigest.getInstance("RIPEMD-128"); // Noncompliant {{Use a stronger hashing algorithm than RIPEMD.}}
    md = MessageDigest.getInstance("RIPEMD160"); // Noncompliant {{Use a stronger hashing algorithm than RIPEMD.}}
    md = MessageDigest.getInstance("HMACRIPEMD160"); // Noncompliant {{Use a stronger hashing algorithm than RIPEMD.}}
    md = MessageDigest.getInstance("SHA-256");
    md = org.apache.commons.codec.digest.DigestUtils.getDigest("MD2"); // Noncompliant
    md = org.apache.commons.codec.digest.DigestUtils.getDigest("MD5"); // Noncompliant
    md = DigestUtils.getDigest("SHA-1"); // Noncompliant
    md = DigestUtils.getDigest("SHA-256");
    md = DigestUtils.getMd5Digest(); // Noncompliant
    md = DigestUtils.getShaDigest(); // Noncompliant
    md = DigestUtils.getSha1Digest(); // Noncompliant
    md = DigestUtils.getSha256Digest();
    DigestUtils.md2(""); // Noncompliant
    DigestUtils.md2Hex(""); // Noncompliant
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

class B extends java.io.File {
  void myMethod() {
    MessageDigest md = null;
    md = MessageDigest.getInstance(separator);
  }
}

class CryptoAPIs {

  void mac() {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5"); // Noncompliant {{Use a stronger hashing algorithm than MD5.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA1"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA256");
  }

  void signature() {
    java.security.Signature signature = java.security.Signature.getInstance("SHA1withDSA"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
    signature = java.security.Signature.getInstance("SHA1withRSA"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
    signature = java.security.Signature.getInstance("MD2withRSA"); // Noncompliant {{Use a stronger hashing algorithm than MD2.}}
    signature = java.security.Signature.getInstance("MD5withRSA"); // Noncompliant {{Use a stronger hashing algorithm than MD5.}}
    signature = java.security.Signature.getInstance("SHA256withRSA"); // Compliant
  }

  void keys() {
    javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA1"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
    keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA256");
    keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");

    javax.crypto.KeyPairGenerator keyPair = java.security.KeyPairGenerator.getInstance("HmacSHA1"); // Noncompliant {{Use a stronger hashing algorithm than SHA-1.}}
  }

  void dsa() {
    java.security.AlgorithmParameters.getInstance("DSA"); // Noncompliant {{Use a stronger hashing algorithm than DSA.}}
    java.security.AlgorithmParameters.getInstance("DiffieHellman");
    java.security.AlgorithmParameterGenerator.getInstance("DSA"); // Noncompliant {{Use a stronger hashing algorithm than DSA.}}
    java.security.AlgorithmParameterGenerator.getInstance("DiffieHellman");
    java.security.KeyPairGenerator.getInstance("DSA"); // Noncompliant {{Use a stronger hashing algorithm than DSA.}}
    java.security.KeyPairGenerator.getInstance("DiffieHellman");
    java.security.KeyFactory.getInstance("DSA"); // Noncompliant {{Use a stronger hashing algorithm than DSA.}}
    java.security.KeyFactory.getInstance("DiffieHellman");
  }
}

class DeprecatedSpring {
  void foo() {
    new ShaPasswordEncoder(); // Noncompliant {{Don't rely on ShaPasswordEncoder because it is deprecated.}}
    new ShaPasswordEncoder(512); // Noncompliant {{Don't rely on ShaPasswordEncoder because it is deprecated.}}
    new Md5PasswordEncoder(); // Noncompliant {{Use a stronger hashing algorithm than MD5.}}
    new LdapShaPasswordEncoder(); // Noncompliant {{Don't rely on LdapShaPasswordEncoder because it is deprecated and use a stronger hashing algorithm.}}
    new LdapShaPasswordEncoder(org.springframework.security.crypto.keygen.KeyGenerators.secureRandom()); // Noncompliant
    new Md4PasswordEncoder(); // Noncompliant {{Don't rely on Md4PasswordEncoder because it is deprecated and use a stronger hashing algorithm.}}
    new MessageDigestPasswordEncoder("algo"); // Noncompliant {{Don't rely on MessageDigestPasswordEncoder because it is deprecated and use a stronger hashing algorithm.}}
    NoOpPasswordEncoder.getInstance(); // Noncompliant {{Use a stronger hashing algorithm than this fake one.}}
    new StandardPasswordEncoder(); // Noncompliant {{Use a stronger hashing algorithm.}}
    new StandardPasswordEncoder("foo"); // Noncompliant {{Use a stronger hashing algorithm.}}
    new BCryptPasswordEncoder();
  }
}

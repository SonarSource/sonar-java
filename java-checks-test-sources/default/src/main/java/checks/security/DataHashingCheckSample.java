package checks.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

class HashMethodsCheck {

  private static final String ALGORITHM = "MD2";

  void myMethod(String algorithm, Provider provider, Properties props) throws NoSuchAlgorithmException, NoSuchProviderException {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD2"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
//                     ^^^^^^^^^^^
    md = MessageDigest.getInstance("MD4"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("MD6"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("MD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
//                     ^^^^^^^^^^^
    md = MessageDigest.getInstance("HAVAL-128"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("HMAC-MD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD-128"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD160"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("HMACRIPEMD160"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-0"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-224"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-256"); // Compliant
    md = MessageDigest.getInstance("SHA-384"); // Compliant
    md = MessageDigest.getInstance("SHA-512"); // Compliant
    md = DigestUtils.getDigest("MD2"); // Noncompliant
    md = DigestUtils.getDigest("MD5"); // Noncompliant
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

    md = MessageDigest.getInstance(ALGORITHM); // Noncompliant
  }

  private String getAlgo() {
    return null;
  }

}

class ExtendedFile extends java.io.File {
  public ExtendedFile(@NotNull String pathname) {
    super(pathname);
  }

  void myMethod() throws NoSuchAlgorithmException {
    MessageDigest md = null;
    md = MessageDigest.getInstance(separator);
  }
}

class CryptoAPIs {

  void mac() throws NoSuchAlgorithmException {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA256");
  }

  void signature() throws NoSuchAlgorithmException {
    java.security.Signature signature = java.security.Signature.getInstance("SHA1withDSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("SHA1withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("MD2withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("MD5withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("SHA256withRSA"); // Compliant
  }

  void keys() throws NoSuchAlgorithmException {
    javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA256");
    keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");

    java.security.KeyPairGenerator keyPair = java.security.KeyPairGenerator.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
  }

  void dsa() throws NoSuchAlgorithmException {
    java.security.AlgorithmParameters.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.AlgorithmParameters.getInstance("DiffieHellman");
    java.security.AlgorithmParameterGenerator.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.AlgorithmParameterGenerator.getInstance("DiffieHellman");
    java.security.KeyPairGenerator.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.KeyPairGenerator.getInstance("DiffieHellman");
    java.security.KeyFactory.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.KeyFactory.getInstance("DiffieHellman");
  }
}

class DeprecatedSpring {
  void foo() {
    new ShaPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new ShaPasswordEncoder(512); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new Md5PasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new LdapShaPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new LdapShaPasswordEncoder(org.springframework.security.crypto.keygen.KeyGenerators.secureRandom()); // Noncompliant
    new Md4PasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new MessageDigestPasswordEncoder("algo"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    NoOpPasswordEncoder.getInstance(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new StandardPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new StandardPasswordEncoder("foo"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new BCryptPasswordEncoder();
  }
}

class SpringDigestUtils {

  void digestUtils() throws IOException {
    org.springframework.util.DigestUtils.appendMd5DigestAsHex(new byte[10], new StringBuilder()); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    org.springframework.util.DigestUtils.appendMd5DigestAsHex(new FileInputStream(""), new StringBuilder()); // Noncompliant
    org.springframework.util.DigestUtils.md5Digest(new byte[10]); // Noncompliant
    org.springframework.util.DigestUtils.md5Digest(new FileInputStream("")); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    org.springframework.util.DigestUtils.md5DigestAsHex(new byte[10]); // Noncompliant
    org.springframework.util.DigestUtils.md5DigestAsHex(new FileInputStream("")); // Noncompliant
  }

}

package checks.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashMethodsCheckWS {

  private static final String ALGORITHM = "MD2";

  void myMethod(String algorithm, Provider provider, Properties props) throws NoSuchAlgorithmException, NoSuchProviderException {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD2"); // Noncompliant

    md = MessageDigest.getInstance("MD4"); // Noncompliant
    md = MessageDigest.getInstance("MD6"); // Noncompliant
    md = MessageDigest.getInstance("MD5"); // Noncompliant

    md = MessageDigest.getInstance("HAVAL-128"); // Noncompliant
    md = MessageDigest.getInstance("HMAC-MD5"); // Noncompliant
    md = MessageDigest.getInstance("RIPEMD"); // Noncompliant
    md = MessageDigest.getInstance("RIPEMD-128"); // Noncompliant
    md = MessageDigest.getInstance("RIPEMD160"); // Noncompliant
    md = MessageDigest.getInstance("HMACRIPEMD160"); // Noncompliant
    md = MessageDigest.getInstance("SHA"); // Noncompliant
    md = MessageDigest.getInstance("SHA-0"); // Noncompliant
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant
    md = MessageDigest.getInstance("SHA-224"); // Noncompliant
    md = MessageDigest.getInstance("SHA-256"); // Compliant
    md = MessageDigest.getInstance("SHA-384"); // Compliant
    md = MessageDigest.getInstance("SHA-512"); // Compliant
    md = DigestUtils.getDigest("SHA-256");
    md = DigestUtils.getSha256Digest();
    DigestUtils.sha256("");
    DigestUtils.sha256Hex("");
    md = MessageDigest.getInstance(algorithm);
    md = DigestUtils.getDigest(algorithm);
    com.google.common.hash.Hashing.sha256();
    md = MessageDigest.getInstance("MD5", provider); // Noncompliant
    md = MessageDigest.getInstance("SHA1", "provider"); // Noncompliant
    md = MessageDigest.getInstance("sha-1", "provider"); // Noncompliant

    String myAlgo = props.getProperty("myCoolAlgo", "SHA1");

    md = MessageDigest.getInstance(myAlgo, provider); // Noncompliant
    md = MessageDigest.getInstance(getAlgo(), provider);
    md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo"));

    md = MessageDigest.getInstance(ALGORITHM); // Noncompliant
  }

  private String getAlgo() {
    return null;
  }

}

class ExtendedFileWS extends java.io.File {
  public ExtendedFileWS(@NotNull String pathname) {
    super(pathname);
  }

  void myMethod() throws NoSuchAlgorithmException {
    MessageDigest md = null;
    md = MessageDigest.getInstance(separator);
  }
}

class CryptoAPIsWS {

  void mac() throws NoSuchAlgorithmException {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5"); // Noncompliant
    mac = javax.crypto.Mac.getInstance("HmacSHA1"); // Noncompliant
    mac = javax.crypto.Mac.getInstance("HmacSHA256");
  }

  void signature() throws NoSuchAlgorithmException {
    java.security.Signature signature = java.security.Signature.getInstance("SHA1withDSA"); // Noncompliant
    signature = java.security.Signature.getInstance("SHA1withRSA"); // Noncompliant
    signature = java.security.Signature.getInstance("MD2withRSA"); // Noncompliant
    signature = java.security.Signature.getInstance("MD5withRSA"); // Noncompliant
    signature = java.security.Signature.getInstance("SHA256withRSA"); // Compliant
  }

  void keys() throws NoSuchAlgorithmException {
    javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA1"); // Noncompliant
    keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA256");
    keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");

    java.security.KeyPairGenerator keyPair = java.security.KeyPairGenerator.getInstance("HmacSHA1"); // Noncompliant
  }

  void dsa() throws NoSuchAlgorithmException {
    java.security.AlgorithmParameters.getInstance("DSA"); // Noncompliant
    java.security.AlgorithmParameters.getInstance("DiffieHellman");
    java.security.AlgorithmParameterGenerator.getInstance("DSA"); // Noncompliant
    java.security.AlgorithmParameterGenerator.getInstance("DiffieHellman");
    java.security.KeyPairGenerator.getInstance("DSA"); // Noncompliant
    java.security.KeyPairGenerator.getInstance("DiffieHellman");
    java.security.KeyFactory.getInstance("DSA"); // Noncompliant
    java.security.KeyFactory.getInstance("DiffieHellman");
  }
}

class DeprecatedSpringWS {
  void foo() {
    new BCryptPasswordEncoder();
  }
}

class SpringDigestUtilsWS {

  void digestUtils() throws IOException {
  }

}

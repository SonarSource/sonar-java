package checks.security;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;

class CryptographicKeySizeCheckCustom {
  public void rsa() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048); // Noncompliant {{Use a key length of at least 4096 bits for RSA cipher algorithm.}}
    keyGen.initialize(4096); // Compliant
  }

  public void aes() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(64); // Compliant - threshold lowered to 64
    keyGen.init(128); // Compliant
  }

  public void dh() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
    keyGen.initialize(1024); // Noncompliant {{Use a key length of at least 2048 bits for DH cipher algorithm.}}
    keyGen.initialize(2048); // Compliant
  }

  public void diffieHellman() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
    keyGen.initialize(1024); // Noncompliant {{Use a key length of at least 2048 bits for DiffieHellman cipher algorithm.}}
    keyGen.initialize(2048); // Compliant
  }

  public void dsa() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
    keyGen.initialize(1024, new SecureRandom()); // Noncompliant {{Use a key length of at least 2048 bits for DSA cipher algorithm.}}
    keyGen.initialize(2048, new SecureRandom()); // Compliant
  }

  public void ec() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(192); // Noncompliant {{Use a key length of at least 224 bits for EC cipher algorithm.}}
    keyGen.initialize(224); // Compliant
  }
}
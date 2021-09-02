package checks.security;

import javax.crypto.Cipher;

import static java.io.File.separator;

abstract class EncryptionAlgorithmCheck {

  static final String RSA = "RSA";
  static final String NO_PADDING = "/NONE/NoPadding";
  static final String RSA_NO_PADDING = RSA + NO_PADDING;

  public void foo(java.util.Properties props) {
    /*
    should complain:
    - everytime ECB mode is used whatever the encryption algorithm
       - By default without specifying operation mode ECB is chosen
    - when CBC mode is used with PKCS5Padding or PKCS7Padding
    - when RSA is used without OAEPWithSHA-1AndMGF1Padding or OAEPWITHSHA-256ANDMGF1PADDING padding scheme
    */

    try {
      // First case
      Cipher.getInstance("AES"); // Noncompliant [[sc=26;ec=31]] {{Use secure mode and padding scheme.}}

      Cipher.getInstance("AES/ECB/NoPadding"); // Noncompliant
      Cipher.getInstance("AES" + "/ECB/NoPadding"); // Noncompliant
      Cipher.getInstance("AES/ECB/NoPadding", getProvider()); // Noncompliant
      Cipher.getInstance("AES/ECB/NoPadding", "someProvider"); // Noncompliant

      Cipher.getInstance("Blowfish/ECB/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("DES/ECB/PKCS5Padding"); // Noncompliant

      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant

      // Second case
      Cipher.getInstance("AES/CBC/PKCS5Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("DES/CBC/PKCS5Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("AES/CBC/PKCS7Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("Blowfish/CBC/PKCS7Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("DES/CBC/PKCS7Padding"); // Compliant - CBC considered as safe
      Cipher.getInstance("DES/CBC/NoPadding"); // Compliant - CBC considered as safe
      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
      Cipher.getInstance("Blowfish/GCM/NoPadding"); // Compliant

      // Third case
      Cipher.getInstance("RSA/NONE/NoPadding"); // Noncompliant
      Cipher.getInstance("RSA/GCM/NoPadding"); // Noncompliant
      Cipher.getInstance("RSA/ECB/NoPadding"); // Noncompliant

      // SUN Security Provider (default for openjdk 11 for example) treats ECB as None
      Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"); // Compliant
      Cipher.getInstance("RSA/NONE/OAEPWithSHA-1AndMGF1Padding"); // Compliant
      Cipher.getInstance("RSA/NONE/OAEPWITHSHA-256ANDMGF1PADDING"); // Compliant
      Cipher.getInstance("RSA/None/OAEPWITHSHA-384ANDMGF1PADDING"); // Compliant
      Cipher.getInstance("RSA/None/OAEPWITHSHA-512ANDMGF1PADDING"); // Compliant

      // Other
      Cipher.getInstance(null); // Compliant
      Cipher.getInstance(""); // Noncompliant
      String algo = props.getProperty("myAlgo", "AES/ECB/PKCS5Padding");
      Cipher.getInstance(algo); // Noncompliant [[sc=26;ec=30;secondary=62]]
      String s = "RSA/NONE/NoPadding"; // Compliant
      Cipher.getInstance(s); // Noncompliant [[sc=26;ec=27;secondary=64]]

      String sPlus = "RSA" + "/NONE/NoPadding"; // Compliant
      Cipher.getInstance(sPlus); // Noncompliant [[sc=26;ec=31;secondary=67]]

      Cipher.getInstance(RSA_NO_PADDING); // Noncompliant [[sc=26;ec=40;secondary=11]]

      Cipher.getInstance(separator); // Compliant, can not resolve the declaration, for coverage

      // Case is ignored
      Cipher.getInstance("rsa/None/NoPadding"); // Noncompliant
      Cipher.getInstance("AES/ecb/NoPadding"); // Noncompliant
      Cipher.getInstance("aes/GCM/NoPadding"); // Compliant
      Cipher.getInstance("DES/CBC/NOPADDING"); // Compliant
      Cipher.getInstance("RSA/NONE/OAEPWITHSHA-1AndMGF1Padding"); // Compliant
      String algoUpperCase = props.getProperty("myAlgo", "AES/ECB/PKCS5PADDING");

    } catch (Exception  e) {
    }
  }

  abstract java.security.Provider getProvider();

}

package checks.security;

import javax.crypto.Cipher;

import static java.io.File.separator;

abstract class EncryptionAlgorithmCheckSample {

  static final String RSA = "RSA";
  static final String NO_PADDING = "/NONE/NoPadding";
  static final String RSA_NO_PADDING = RSA + NO_PADDING;
//  ^^^<

  public void foo(java.util.Properties props) {
    /*
    should complain:
    - everytime ECB mode is used
       - By default without specifying operation mode ECB is chosen
    - when CBC mode is used with padding different from "NoPadding"
    - when RSA is used without OAEPWithSHA-1AndMGF1Padding or OAEPWITHSHA-256ANDMGF1PADDING padding scheme
    */

    try {
      // First case
      Cipher.getInstance("AES"); // Noncompliant {{Use a secure padding scheme.}}
//                       ^^^^^

      Cipher.getInstance("AES/ECB/NoPadding"); // Noncompliant {{Use a secure cipher mode.}}
      Cipher.getInstance("AES" + "/ECB/NoPadding"); // Noncompliant
      Cipher.getInstance("AES/ECB/NoPadding", getProvider()); // Noncompliant
      Cipher.getInstance("AES/ECB/NoPadding", "someProvider"); // Noncompliant

      Cipher.getInstance("Blowfish/ECB/PKCS5Padding"); // Noncompliant {{Use a secure cipher mode.}}
      Cipher.getInstance("DES/ECB/PKCS5Padding"); // Noncompliant

      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant

      // Second case
      Cipher.getInstance("AES/CBC/PKCS5Padding"); // Noncompliant {{Use another cipher mode or disable padding.}}
      Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("AES/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("Blowfish/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/NoPadding"); // Compliant
      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
      Cipher.getInstance("Blowfish/GCM/NoPadding"); // Compliant

      // Third case
      Cipher.getInstance("RSA/NONE/NoPadding"); // Noncompliant {{Use a secure padding scheme.}}
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
//  ^^^<
      Cipher.getInstance(algo); // Noncompliant
//                       ^^^^
      String s = "RSA/NONE/NoPadding"; // Compliant
//  ^^^<
      Cipher.getInstance(s); // Noncompliant
//                       ^

      String sPlus = "RSA" + "/NONE/NoPadding"; // Compliant
//  ^^^<
      Cipher.getInstance(sPlus); // Noncompliant
//                       ^^^^^

      Cipher.getInstance(RSA_NO_PADDING); // Noncompliant
//                       ^^^^^^^^^^^^^^

      Cipher.getInstance(separator); // Compliant, can not resolve the declaration, for coverage

      // Case is ignored
      Cipher.getInstance("rsa/None/NoPadding"); // Noncompliant
      Cipher.getInstance("AES/ecb/NoPadding"); // Noncompliant
      Cipher.getInstance("aes/GCM/NoPadding"); // Compliant
      Cipher.getInstance("DES/CBC/NOPADDING"); // Compliant
      Cipher.getInstance("RSA/NONE/OAEPWITHSHA-1AndMGF1Padding"); // Compliant
      String algoUpperCase = props.getProperty("myAlgo", "AES/ECB/PKCS5PADDING");
//  ^^^<
      Cipher.getInstance(algoUpperCase); // Noncompliant
//                       ^^^^^^^^^^^^^

    } catch (Exception  e) {
    }
  }

  abstract java.security.Provider getProvider();

}

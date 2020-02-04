import javax.crypto.Cipher;

abstract class A {

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
      Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"); // Noncompliant

      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant

      // Second case
      Cipher.getInstance("AES/CBC/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/PKCS5Padding"); // Noncompliant
      Cipher.getInstance("AES/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("Blowfish/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/PKCS7Padding"); // Noncompliant
      Cipher.getInstance("DES/CBC/NoPadding"); // Compliant
      Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
      Cipher.getInstance("Blowfish/GCM/NoPadding"); // Compliant

      // Third case
      Cipher.getInstance("RSA/NONE/NoPadding"); // Noncompliant
      Cipher.getInstance("RSA/GCM/NoPadding"); // Noncompliant

      Cipher.getInstance("RSA/NONE/OAEPWithSHA-1AndMGF1Padding"); // Compliant
      Cipher.getInstance("RSA/NONE/OAEPWITHSHA-256ANDMGF1PADDING"); // Compliant

      // Other
      Cipher.getInstance(null); // Compliant
      Cipher.getInstance(""); // Noncompliant
      String algo = props.getProperty("myAlgo", "AES/ECB/PKCS5Padding");
      Cipher.getInstance(algo); // Noncompliant
      String s = "RSA/NONE/NoPadding"; // Compliant
    } catch (Exception  e) {
    }
  }

  abstract java.security.Provider getProvider();

}

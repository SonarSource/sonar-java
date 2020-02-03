import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchProviderException;

abstract class A {

  public void foo(java.util.Properties props) {
    /*
    should complain:
    - everytime ECB mode is used whatever the encryption algorithm
       - By default without specifying operation mode ECB is chosen
    - when CBC mode is used with PKCS5Padding or PKCS7Padding
    - when RSA is used without OAEPWithSHA-1AndMGF1Padding or OAEPWITHSHA-256ANDMGF1PADDING padding scheme
    */

    try
    {
      // First case
      Cipher c0 = Cipher.getInstance("AES"); // Noncompliant [[sc=38;ec=43]] {{Use secure mode and padding scheme.}}

      Cipher c100 = Cipher.getInstance("AES/ECB/NoPadding"); // Noncompliant
      Cipher c101 = Cipher.getInstance("AES" + "/ECB/NoPadding"); // Noncompliant
      Cipher c102 = Cipher.getInstance("AES/ECB/NoPadding", getProvider()); // Noncompliant
      Cipher c103 = Cipher.getInstance("AES/ECB/NoPadding", "someProvider"); // Noncompliant

      Cipher c3 = Cipher.getInstance("Blowfish/ECB/PKCS5Padding"); // Noncompliant
      Cipher c4 = Cipher.getInstance("DES/ECB/PKCS5Padding"); // Noncompliant
      Cipher c41 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding"); // Noncompliant

      Cipher c5 = Cipher.getInstance("AES/GCM/NoPadding"); // Compliant

      // Second case
      Cipher c6 = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Noncompliant
      Cipher c7 = Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); // Noncompliant
      Cipher c8 = Cipher.getInstance("DES/CBC/PKCS5Padding"); // Noncompliant
      Cipher c9 = Cipher.getInstance("AES/CBC/PKCS7Padding"); // Noncompliant
      Cipher c10 = Cipher.getInstance("Blowfish/CBC/PKCS7Padding"); // Noncompliant
      Cipher c11 = Cipher.getInstance("DES/CBC/PKCS7Padding"); // Noncompliant

      Cipher c112 = Cipher.getInstance("DES/CBC/NoPadding"); // Compliant
      Cipher c12 = Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
      Cipher c13 = Cipher.getInstance("Blowfish/GCM/NoPadding"); // Compliant

      // Third case
      Cipher c14 = Cipher.getInstance("RSA/NONE/NoPadding"); // Noncompliant
      Cipher c142 = Cipher.getInstance("RSA/GCM/NoPadding"); // Noncompliant

      Cipher c15 = Cipher.getInstance("RSA/NONE/OAEPWithSHA-1AndMGF1Padding"); // Compliant
      Cipher c16 = Cipher.getInstance("RSA/NONE/OAEPWITHSHA-256ANDMGF1PADDING"); // Compliant

      // Other
      Cipher.getInstance(null);
      Cipher.getInstance(""); // Noncompliant
      String algo = props.getProperty("myAlgo", "AES/ECB/PKCS5Padding");
      Cipher c17 = Cipher.getInstance(algo); // Noncompliant
      String s = "RSA/NONE/NoPadding";
    }
    catch(NoSuchAlgorithmException|NoSuchPaddingException e)
    {}
    catch (NoSuchProviderException e)
    {}
  }

  abstract java.security.Provider getProvider();

}

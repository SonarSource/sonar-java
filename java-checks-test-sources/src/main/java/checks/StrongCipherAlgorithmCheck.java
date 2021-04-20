package checks;

import javax.crypto.Cipher;
import javax.crypto.NullCipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

class MyCipher extends Cipher {
  public MyCipher() {
    super(null, null, "");
  }
}

class StrongCipherAlgorithmCheck {
  private final static String DES = "DES";

  void foo() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException {
    Cipher.getInstance("DESede/ECB/PKCS5Padding"); // Noncompliant [[sc=24;ec=49]] {{Use a strong cipher algorithm.}}
    Cipher.getInstance("DES/ECB/PKCS5Padding");// Noncompliant
    Cipher.getInstance("RC2/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("AES/GCM/NoPadding");//Compliant
    new NullCipher(); // Noncompliant [[sc=9;ec=19]] {{Use a strong cipher algorithm.}}
    new javax.crypto.NullCipher(); // Noncompliant
    new MyCipher();

    // DES
    Cipher.getInstance("DES"); // Noncompliant
    Cipher.getInstance(DES); // Noncompliant
    Cipher.getInstance("DES/ECB"); // Noncompliant
    Cipher.getInstance("DES/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("DES/GCM"); // Noncompliant
    Cipher.getInstance("DES/GCM/NoPadding"); // Noncompliant
    Cipher.getInstance("DES/GCM/PKCS5Padding"); // Noncompliant

    // 3DES
    Cipher.getInstance("DESede"); // Noncompliant
    Cipher.getInstance("DESede/ECB"); // Noncompliant
    Cipher.getInstance("DESede/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("DESede/GCM"); // Noncompliant
    Cipher.getInstance("DESede/GCM/NoPadding"); // Noncompliant
    Cipher.getInstance("DESede/GCM/PKCS5Padding"); // Noncompliant

    // DESedeWrap
    Cipher.getInstance("DESedeWrap"); // Noncompliant
    Cipher.getInstance("DESedeWrap/GCM/PKCS5Padding"); // Noncompliant

    // RC2
    Cipher.getInstance("RC2"); // Noncompliant
    Cipher.getInstance("RC2/ECB"); // Noncompliant
    Cipher.getInstance("RC2/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("RC2/GCM"); // Noncompliant
    Cipher.getInstance("RC2/GCM/NoPadding"); // Noncompliant
    Cipher.getInstance("RC2/GCM/PKCS5Padding"); // Noncompliant

    // ARC2 (alias of RC2, not officially supported)
    Cipher.getInstance("ARC2"); // Noncompliant
    Cipher.getInstance("ARC2/GCM/PKCS5Padding"); // Noncompliant

    // RC4
    Cipher.getInstance("RC4"); // Noncompliant
    Cipher.getInstance("RC4/ECB"); // Noncompliant
    Cipher.getInstance("RC4/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("RC4/GCM"); // Noncompliant
    Cipher.getInstance("RC4/GCM/NoPadding"); // Noncompliant
    Cipher.getInstance("RC4/GCM/PKCS5Padding"); // Noncompliant

    // ARC4
    Cipher.getInstance("ARC4"); // Noncompliant
    Cipher.getInstance("ARC4/GCM/PKCS5Padding"); // Noncompliant

    // ARCFOUR
    Cipher.getInstance("ARCFOUR"); // Noncompliant
    Cipher.getInstance("ARCFOUR/GCM/PKCS5Padding", "IAIK"); // Noncompliant

    // Blowfish
    Cipher.getInstance("Blowfish"); // Noncompliant
    Cipher.getInstance("Blowfish/ECB"); // Noncompliant
    Cipher.getInstance("Blowfish/ECB/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("Blowfish/GCM"); // Noncompliant
    Cipher.getInstance("Blowfish/GCM/NoPadding"); // Noncompliant
    Cipher.getInstance("Blowfish/GCM/PKCS5Padding"); // Noncompliant

    Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
  }

  void usingJavaUtilProperties(java.util.Properties props, String otherAlgo) throws NoSuchAlgorithmException, NoSuchPaddingException {
    String algo = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding");
    Cipher.getInstance(algo); // Noncompliant
    Cipher.getInstance(props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")); // Noncompliant
    Cipher.getInstance(getAlgo()); // Compliant
    Cipher.getInstance("/"); // Compliant

    String algo2 = props.getProperty("myAlgo");
    Cipher.getInstance(algo2); // Compliant

    String algo3 = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding");
    algo3 = "myOtherAlgo";
    Cipher.getInstance(algo3); // Compliant

    String algo4 = getAlgo();
    Cipher.getInstance(algo4); // Compliant

    Cipher.getInstance(otherAlgo); // Compliant

    String algo5 = "myAlgo";
    Cipher.getInstance(algo5); // Compliant

    String algo6 = props.getProperty("myAlgo", getAlgo());
    Cipher.getInstance(algo6); // Compliant
  }

  private String getAlgo() {
    return null;
  }
}

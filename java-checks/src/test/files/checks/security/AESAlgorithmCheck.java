import javax.crypto.Cipher;

abstract class A {

  public void foo(java.util.Properties props) {
    Cipher.getInstance();
    Cipher.getInstance(someMethod());
    Cipher.getInstance("AES/ECB/NoPadding"); // Noncompliant {{Use Galois/Counter Mode (GCM/NoPadding) instead.}}
    Cipher.getInstance("AES/CBC/PKCS5Padding"); // Noncompliant
    Cipher.getInstance("AES/GCM/NoPadding"); // Compliant
    Cipher.getInstance("DES/ECB/NoPadding"); // Compliant, not AES
    Cipher.getInstance("AES/ECB/NoPadding", getProvider()); // Noncompliant
    Cipher.getInstance("AES/ECB/NoPadding", "someProvider"); // Noncompliant
    com.mypackage.Cipher.getInstance("AES/ECB/NoPadding");

    String algo = props.getProperty("myAlgo", "AES/ECB/PKCS5Padding");
    Cipher.getInstance(algo); // Noncompliant

    Cipher.getInstance("AES" + "/ECB/NoPadding"); // Noncompliant
    Cipher.getInstance(null);
    Cipher.getInstance("");
  }

  abstract java.security.Provider getProvider();

}

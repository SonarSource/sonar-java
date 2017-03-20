import javax.crypto.Cipher;

class A {
  void foo(){
    Cipher c;
    c = Cipher.getInstance("DESede/ECB/PKCS5Padding"); // Noncompliant [[sc=28;ec=53]] {{Use the recommended AES (Advanced Encryption Standard) instead.}}
    c = Cipher.getInstance("DES/ECB/PKCS5Padding");// Noncompliant {{Use the recommended AES (Advanced Encryption Standard) instead.}}
    c = Cipher.getInstance("RC2/ECB/PKCS5Padding"); // Noncompliant {{Use the recommended AES (Advanced Encryption Standard) instead.}}
    c = Cipher.getInstance("AES/GCM/NoPadding");//Compliant
  }
  
  void usingJavaUtilProperties(java.util.Properties props, String otherAlgo) {
    String algo = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding");
    c = Cipher.getInstance(algo); // Noncompliant {{Use the recommended AES (Advanced Encryption Standard) instead.}}
    c = Cipher.getInstance(props.getProperty("myAlgo", "DES/ECB/PKCS5Padding")); // Noncompliant {{Use the recommended AES (Advanced Encryption Standard) instead.}}
    c = Cipher.getInstance(getAlgo()); // Compliant
    c = Cipher.getInstance("/"); // Compliant
    
    String algo2 = props.getProperty("myAlgo");
    c = Cipher.getInstance(algo2); // Compliant
    
    String algo3 = props.getProperty("myAlgo", "DES/ECB/PKCS5Padding");
    algo3 = "myOtherAlgo";
    c = Cipher.getInstance(algo3); // Compliant
    
    String algo4 = getAlgo();
    c = Cipher.getInstance(algo4); // Compliant
    
    c = Cipher.getInstance(otherAlgo); // Compliant
    
    String algo5 = "myAlgo";
    c = Cipher.getInstance(algo5); // Compliant
    
    String algo6 = props.getProperty("myAlgo", getAlgo());
    c = Cipher.getInstance(algo6); // Compliant
  }
  
  private String getAlgo() {
    return null;
  }
}

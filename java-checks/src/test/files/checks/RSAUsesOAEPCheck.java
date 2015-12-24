import javax.crypto.Cipher;

class A {
  void foo(){
    Cipher c;
    c = Cipher.getInstance("RSA/NONE/NoPadding"); // Noncompliant [[sc=28;ec=48]] {{Use an RSA algorithm with an OAEP (Optimal Asymmetric Encryption Padding).}}
    c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");//Compliant
    c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");//Compliant
    c = Cipher.getInstance("NotACorrectAlgorithmString");//Compliant
    c = Cipher.getInstance("AES/GCM/NoPadding");//Compliant
    String s;
    s = "RSA/NONE/NoPadding"; // Noncompliant [[sc=9;ec=29]] {{Use an RSA algorithm with an OAEP (Optimal Asymmetric Encryption Padding).}}
    s = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";//Compliant
    s = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";//Compliant
    s = "NotACorrectAlgorithmString";//Compliant
    s = "AES/GCM/NoPadding";//Compliant
  }
}

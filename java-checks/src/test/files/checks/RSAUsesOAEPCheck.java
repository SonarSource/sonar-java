import javax.crypto.Cipher;

class A {
  void foo(){
    Cipher c;
    c = Cipher.getInstance("RSA/NONE/NoPadding"); //NonCompliant
    c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");//Compliant
  }
}
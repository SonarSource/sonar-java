import javax.crypto.Cipher;

class A {
  void foo(){
    Cipher c;
    c = Cipher.getInstance("DESede/ECB/PKCS5Padding"); //NonCompliant
    c = Cipher.getInstance("DES/ECB/PKCS5Padding");//NonCompliant
    c = Cipher.getInstance("AES/GCM/NoPadding");//Compliant
  }
}
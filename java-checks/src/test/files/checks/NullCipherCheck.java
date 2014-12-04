import javax.crypto.Cipher;
import javax.crypto.NullCipher;

class A {
  void method() {
    Cipher c;
    c = new NullCipher();
    c = new javax.crypto.NullCipher();
    c = new MyCipher();
    c = Cipher.getInstance("DES/CBC/PKCS5Padding");
    c = new UnknownClass();
    new Comparable<String>();
  }
}

class MyCipher extends Cipher {
  
}
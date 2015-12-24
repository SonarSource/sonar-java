import javax.crypto.Cipher;
import javax.crypto.NullCipher;

class A {
  void method() {
    Cipher c;
    c = new NullCipher(); // Noncompliant [[sc=13;ec=23]] {{Remove this use of the "NullCipher" class.}}
    c = new javax.crypto.NullCipher(); // Noncompliant
    c = new MyCipher();
    c = Cipher.getInstance("DES/CBC/PKCS5Padding");
    c = new UnknownClass();
    new Comparable<String>();
  }
}

class MyCipher extends Cipher {
  
}

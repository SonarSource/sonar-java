import java.security.KeyPairGenerator;
import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.ECGenParameterSpec;
import java.security.SecureRandom;

class RSA {
  public void key_variable() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant [[sc=5;ec=28]]  {{Use a key length of at least 2048 bits for RSA cipher algorithm.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048); // Compliant
  }

  public void report_twice() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(1024); // Noncompliant
    keyGen.initialize(1023); // Noncompliant
  }
}

interface I {
  Runnable r = () -> {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
  };
}

class DH {
  public void key_variable_DH() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
    keyGen.initialize(1024); // Noncompliant [[sc=5;ec=28]]  {{Use a key length of at least 2048 bits for DH cipher algorithm.}}
  }

  public void key_variable_compliant_DH() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
    keyGen.initialize(2048); // Compliant
  }

  public void key_variable() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
    keyGen.initialize(1024); // Noncompliant [[sc=5;ec=28]]  {{Use a key length of at least 2048 bits for DiffieHellman cipher algorithm.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DiffieHellman");
    keyGen.initialize(2048); // Compliant
  }
}

class DSA {
  public void key_variable_DSA() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
    keyGen.initialize(1024, new SecureRandom()); // Noncompliant [[sc=5;ec=48]]  {{Use a key length of at least 2048 bits for DSA cipher algorithm.}}
  }

  public void key_variable_compliant_DSA() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
    keyGen.initialize(2048, new SecureRandom()); // Compliant
  }
}

class AES {
  public void key_variable() throws NoSuchAlgorithmException {
    KeyGenerator keyGen1 = KeyGenerator.getInstance("AES");
    keyGen1.init(64); // Noncompliant [[sc=5;ec=21]]  {{Use a key length of at least 128 bits for AES cipher algorithm.}}
  }

  public void key_variable_compliant() throws NoSuchAlgorithmException {
    KeyGenerator keyGen2 = KeyGenerator.getInstance("AES");
    keyGen2.init(128); // Compliant
  }
}

class EC {
  public void key_EC() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec ecSpec1 = new ECGenParameterSpec("secp112r1"); // Noncompliant [[sc=34;ec=69]]  {{Use a key length of at least 224 bits for EC cipher algorithm.}}
    keyPairGen.initialize(ecSpec1);
    ECGenParameterSpec ecSpec2 = new ECGenParameterSpec("secp112r2"); // Noncompliant
    keyPairGen.initialize(ecSpec2);
    ECGenParameterSpec ecSpec3 = new ECGenParameterSpec("secp128r1"); // Noncompliant
    keyPairGen.initialize(ecSpec3);
    ECGenParameterSpec ecSpec4 = new ECGenParameterSpec("secp128r2"); // Noncompliant
    keyPairGen.initialize(ecSpec4);
    ECGenParameterSpec ecSpec5 = new ECGenParameterSpec("secp160k1"); // Noncompliant
    keyPairGen.initialize(ecSpec5);
    ECGenParameterSpec ecSpec6 = new ECGenParameterSpec("secp160r1"); // Noncompliant
    keyPairGen.initialize(ecSpec6);
    ECGenParameterSpec ecSpec7 = new ECGenParameterSpec("secp160r2"); // Noncompliant
    keyPairGen.initialize(ecSpec7);
    ECGenParameterSpec ecSpec8 = new ECGenParameterSpec("secp192k1"); // Noncompliant
    keyPairGen.initialize(ecSpec8);
    ECGenParameterSpec ecSpec9 = new ECGenParameterSpec("secp192r1"); // Noncompliant
    keyPairGen.initialize(ecSpec9);
    ECGenParameterSpec ecSpec10 = new ECGenParameterSpec("secp224k1");// compliant
    keyPairGen.initialize(ecSpec10);
    ECGenParameterSpec ecSpec11 = new ECGenParameterSpec("secp224r1");// compliant
    keyPairGen.initialize(ecSpec11);
    ECGenParameterSpec ecSpec12 = new ECGenParameterSpec("secp256k1");// compliant
    keyPairGen.initialize(ecSpec12);
    ECGenParameterSpec ecSpec13 = new ECGenParameterSpec("secp256r1");// compliant
    keyPairGen.initialize(ecSpec13);
    ECGenParameterSpec ecSpec14 = new ECGenParameterSpec("secp384r1");// compliant
    keyPairGen.initialize(ecSpec14);
    ECGenParameterSpec ecSpec15 = new ECGenParameterSpec("secp521r1");// compliant
    keyPairGen.initialize(ecSpec15);
    ECGenParameterSpec ecSpec16 = new ECGenParameterSpec("prime192v2"); // Noncompliant
    keyPairGen.initialize(ecSpec16);
    ECGenParameterSpec ecSpec17 = new ECGenParameterSpec("prime192v3"); // Noncompliant
    keyPairGen.initialize(ecSpec17);
    ECGenParameterSpec ecSpec18 = new ECGenParameterSpec("prime239v1");// compliant
    keyPairGen.initialize(ecSpec18);
    ECGenParameterSpec ecSpec19 = new ECGenParameterSpec("prime239v2");// compliant
    keyPairGen.initialize(ecSpec19);
    ECGenParameterSpec ecSpec20 = new ECGenParameterSpec("prime239v3");// compliant
    keyPairGen.initialize(ecSpec20);
    ECGenParameterSpec ecSpec21 = new ECGenParameterSpec("sect113r1"); // Noncompliant
    keyPairGen.initialize(ecSpec21);
    ECGenParameterSpec ecSpec22 = new ECGenParameterSpec("sect113r2"); // Noncompliant
    keyPairGen.initialize(ecSpec22);
    ECGenParameterSpec ecSpec24 = new ECGenParameterSpec("sect131r1"); // Noncompliant
    keyPairGen.initialize(ecSpec24);
    ECGenParameterSpec ecSpec25 = new ECGenParameterSpec("sect131r2"); // Noncompliant
    keyPairGen.initialize(ecSpec25);
    ECGenParameterSpec ecSpec26 = new ECGenParameterSpec("sect163k1"); // Noncompliant
    keyPairGen.initialize(ecSpec26);
    ECGenParameterSpec ecSpec27 = new ECGenParameterSpec("sect163r1"); // Noncompliant
    keyPairGen.initialize(ecSpec27);
    ECGenParameterSpec ecSpec28 = new ECGenParameterSpec("sect163r2"); // Noncompliant
    keyPairGen.initialize(ecSpec28);
    ECGenParameterSpec ecSpec29 = new ECGenParameterSpec("sect193r1"); // Noncompliant
    keyPairGen.initialize(ecSpec29);
    ECGenParameterSpec ecSpec30 = new ECGenParameterSpec("sect193r2"); // Noncompliant
    keyPairGen.initialize(ecSpec30);
    ECGenParameterSpec ecSpec31 = new ECGenParameterSpec("sect233k1");// compliant
    keyPairGen.initialize(ecSpec31);
    ECGenParameterSpec ecSpec32 = new ECGenParameterSpec("sect233r1");// compliant
    keyPairGen.initialize(ecSpec32);
    ECGenParameterSpec ecSpec33 = new ECGenParameterSpec("sect239k1");// compliant
    keyPairGen.initialize(ecSpec33);
    ECGenParameterSpec ecSpec34 = new ECGenParameterSpec("sect283k1");// compliant
    keyPairGen.initialize(ecSpec34);
    ECGenParameterSpec ecSpec35 = new ECGenParameterSpec("sect283r1");// compliant
    keyPairGen.initialize(ecSpec35);
    ECGenParameterSpec ecSpec36 = new ECGenParameterSpec("sect409k1");// compliant
    keyPairGen.initialize(ecSpec36);
    ECGenParameterSpec ecSpec37 = new ECGenParameterSpec("sect409r1");// compliant
    keyPairGen.initialize(ecSpec37);
    ECGenParameterSpec ecSpec38 = new ECGenParameterSpec("sect571k1");// compliant
    keyPairGen.initialize(ecSpec38);
    ECGenParameterSpec ecSpec39 = new ECGenParameterSpec("sect571r1");// compliant
    keyPairGen.initialize(ecSpec39);
    ECGenParameterSpec ecSpec40 = new ECGenParameterSpec("c2tnb191v1"); // Noncompliant
    keyPairGen.initialize(ecSpec40);
    ECGenParameterSpec ecSpec41 = new ECGenParameterSpec("c2tnb191v2"); // Noncompliant
    keyPairGen.initialize(ecSpec41);
    ECGenParameterSpec ecSpec42 = new ECGenParameterSpec("c2tnb191v3"); // Noncompliant
    keyPairGen.initialize(ecSpec42);
    ECGenParameterSpec ecSpec43 = new ECGenParameterSpec("c2tnb239v1");// compliant
    keyPairGen.initialize(ecSpec43);
    ECGenParameterSpec ecSpec44 = new ECGenParameterSpec("c2tnb239v2");// compliant
    keyPairGen.initialize(ecSpec44);
    ECGenParameterSpec ecSpec45 = new ECGenParameterSpec("c2tnb239v3");// compliant
    keyPairGen.initialize(ecSpec45);
    ECGenParameterSpec ecSpec46 = new ECGenParameterSpec("c2tnb359v1");// compliant
    keyPairGen.initialize(ecSpec46);
    ECGenParameterSpec ecSpec47 = new ECGenParameterSpec("c2tnb431r1");// compliant
    keyPairGen.initialize(ecSpec47);

    ECGenParameterSpec ecSpec48 = new ECGenParameterSpec("some123v123");// compliant, unexpected key
    keyPairGen.initialize(ecSpec48);
    ECGenParameterSpec ecSpec49 = new ECGenParameterSpec("EC");// compliant, unexpected key
    keyPairGen.initialize(ecSpec49);
    ECGenParameterSpec ecSpec50 = new ECGenParameterSpec("primee123v23");// compliant, unexpected key
    keyPairGen.initialize(ecSpec50);
  }
}

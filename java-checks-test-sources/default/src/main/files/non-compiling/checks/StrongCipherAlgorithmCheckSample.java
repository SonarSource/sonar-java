package checks;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

class StrongCipherAlgorithmCheckSample {
  private static final String DES_STRING = "DES";
  private static final String DES_TEXT_BLOCK = """
    DES""";

  void foo() throws NoSuchAlgorithmException, NoSuchPaddingException {
    Cipher.getInstance(DES_STRING); // Noncompliant
    Cipher.getInstance(DES_TEXT_BLOCK); // Noncompliant
    // Noncompliant@+1
    Cipher.getInstance("""
      DES""");
  }
}

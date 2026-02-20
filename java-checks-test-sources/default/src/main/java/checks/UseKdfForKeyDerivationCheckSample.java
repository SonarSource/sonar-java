package checks;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;

class UseKdfForKeyDerivationCheckSample {

  private static final String HKDF_ALGO = "HKDF-SHA256";
  private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";

  private static final String AES = "AES";

  // --- Noncompliant: KeyGenerator with KDF algorithms ---

  void nonCompliantAlgos() throws NoSuchAlgorithmException {
    KeyGenerator.getInstance("HKDF-SHA256"); // Noncompliant {{Use the KDF API instead of KeyGenerator for key derivation.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    KeyGenerator.getInstance("HKDF-SHA384"); // Noncompliant
    KeyGenerator.getInstance("HKDF-SHA512"); // Noncompliant
    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // Noncompliant {{Use the KDF API instead of SecretKeyFactory for key derivation.}}
  }

  void nonCompliantConstantInArgument() throws NoSuchAlgorithmException {
    KeyGenerator.getInstance(HKDF_ALGO); // Noncompliant
    SecretKeyFactory.getInstance(PBKDF2_ALGO); // Noncompliant
  }

  // --- Compliant: non-KDF algorithms ---

  void compliantAlgos() throws NoSuchAlgorithmException {
    KeyGenerator.getInstance("AES"); // Compliant
    KeyGenerator.getInstance("DES"); // Compliant
    SecretKeyFactory.getInstance("SHA-224"); // Compliant
  }

  void compliantConstantInArgument() throws NoSuchAlgorithmException {
    KeyGenerator.getInstance(AES); // Compliant
    SecretKeyFactory.getInstance(AES); // Compliant
  }

  void compliantUnknownArg(String algo) throws NoSuchAlgorithmException {
    KeyGenerator.getInstance(algo); // Compliant - unknown algorithm at compile time
    SecretKeyFactory.getInstance(algo); // Compliant - unknown algorithm at compile time
  }
}

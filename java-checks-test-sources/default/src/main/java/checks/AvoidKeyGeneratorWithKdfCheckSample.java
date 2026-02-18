package checks;

import java.security.SecureRandom;
import javax.crypto.KDF;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;

class AvoidKeyGeneratorWithKdfCheckSample {

  private static final byte[] getSharedSecret() {
    return null;
  }

  void noncompliant() throws Exception {
    byte[] sharedSecret = getSharedSecret();
    KeyGenerator kg = KeyGenerator.getInstance("HKDF-SHA256"); // Noncompliant

    kg.init(new SecureRandom(sharedSecret));
    SecretKey key = kg.generateKey();
  }

  void compliant() throws Exception {
    byte[] ikm = getSharedSecret();
    byte[] info = "AES_256_GCM_Key".getBytes();

    KDF hkdf = KDF.getInstance("HKDF-SHA256");
    HKDFParameterSpec params = HKDFParameterSpec.ofExtract()
      .addIKM(ikm)
      .thenExpand(info, 32);

    SecretKey key = hkdf.deriveKey("AES", params);
  }

}

package checks.security;

import org.h2.security.SHA256;

public class CredentialsShouldNotBeHardcodedCheck {
  void nonCompliant(byte[] message) {
    String inputString = "s3cr37";
    byte[] key = inputString.getBytes();
    SHA256.getHMAC(key, message);  // Noncompliant
  }
}

class HardCodedCredentialsShouldNotBeUsedCheck {

  void test_unknow_identifier() {

    byte[] knownIdentifier = new byte[]{0xC, 0xA};

    org.h2.security.SHA256.getHMAC(knownIdentifier, message); // Noncompliant
    org.h2.security.SHA256.getHMAC(unknownIdentifier, message); // compliant unknown
  }
}

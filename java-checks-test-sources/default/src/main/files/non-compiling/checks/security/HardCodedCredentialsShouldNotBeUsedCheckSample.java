class HardCodedCredentialsShouldNotBeUsedCheckSample {

  void test_unknow_identifier() {

    byte[] knownIdentifier = new byte[]{0xC, 0xA};

    org.h2.security.SHA256.getHMAC(knownIdentifier, message); // Noncompliant
    org.h2.security.SHA256.getHMAC(unknownIdentifier, message); // compliant unknown

    int obj1, obj2;
    obj1 = obj2;
    obj2 = obj1;
    new Pbkdf2PasswordEncoder("", obj1); // compliant, and should not raise a StackOverflowError
  }
}

package checks;

import java.security.SecureRandom;

class S4347 {
  private static final byte[] arr = new byte[]{21,12};
  private static final byte[][] arr2 = new byte[42][42];
  private static byte[] arr3 = new byte[] {21, 12};
  private final byte[] arr4 = new byte[] {21, 12};

  void fun(String param) throws Exception {
    new SecureRandom().setSeed(123456L); // Noncompliant [[sc=32;ec=39]] {{Change this seed value to something unpredictable, or remove the seed.}}

    new SecureRandom("abcdefghijklmnop".getBytes("us-ascii")); // Noncompliant
    new SecureRandom(param.getBytes("us-ascii"));

    new SecureRandom(arr); // Noncompliant
    new SecureRandom(new byte[]{21,12}); // Noncompliant
    new SecureRandom(new byte[12]); // Noncompliant

    new SecureRandom();
    new SecureRandom(getBytes());
    new SecureRandom(arr2[14]);
    new SecureRandom(arr3);
    new SecureRandom(arr4);
  }

  byte[] getBytes() { return new byte[0]; }
}

import java.security.*;

class A {
  private static final byte[] arr = new byte[]{21,12};
  void fun(String param) {
    SecureRandom sr = new SecureRandom();
    sr.setSeed(123456L); // Noncompliant [[sc=16;ec=23]] {{Change this seed value to something unpredictable, or remove the seed.}}
    int v = sr.next(32);

    sr = new SecureRandom("abcdefghijklmnop".getBytes("us-ascii")); // Noncompliant
    sr = new SecureRandom(param.getBytes("us-ascii"));
    v = sr.next(32);

    SecureRandom sr2 = new SecureRandom();
    v = sr2.next(32);
    SecureRandom sr3 = new SecureRandom(arr); // Noncompliant
    sr3 = new SecureRandom(new byte[]{21,12}); // Noncompliant
    sr3 = new SecureRandom(new byte[12]); // Noncompliant
  }
}

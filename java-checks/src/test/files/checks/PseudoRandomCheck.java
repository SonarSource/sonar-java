import java.util.Random;

class A {
  void fun() {
    Random random = new Random(); // Noncompliant {{Use a cryptographically strong random number generator (RNG) like "java.security.SecureRandom" in place of this PRNG}}
    byte[] bytes = new byte[20];
    random.nextBytes(bytes);
    double j = Math.random(); // Noncompliant {{Use a cryptographically strong random number generator (RNG) like "java.security.SecureRandom" in place of this PRNG}}
  }
}
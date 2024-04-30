package checks.tests;

import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RandomizedTestDataCheckSample {

  @Test
  public void randomizedTest() {

    UUID userID = UUID.randomUUID(); // Noncompliant {{Replace randomly generated values with fixed ones.}}
//                ^^^^^^^^^^^^^^^^^
    UUID u1 = UUID.randomUUID();
//            ^^^^^^^^^^^^^^^^^<
    UUID u2 = UUID.randomUUID();
//            ^^^^^^^^^^^^^^^^^<
    UUID u3 = UUID.randomUUID();
//            ^^^^^^^^^^^^^^^^^<
    UUID u4 = UUID.randomUUID();
//            ^^^^^^^^^^^^^^^^^<
    UUID u5 = UUID.randomUUID();
//            ^^^^^^^^^^^^^^^^^<

    int userAge = new Random().nextInt(42); // Noncompliant {{Replace randomly generated values with fixed ones.}}
//                ^^^^^^^^^^^^

    int age1 = new Random().nextInt(42);
//             ^^^^^^^^^^^^<
    int age2 = new Random().nextInt(42);
//             ^^^^^^^^^^^^<
    int age3 = new Random().nextInt(42);
//             ^^^^^^^^^^^^<
    int age4 = new Random().nextInt(42);
//             ^^^^^^^^^^^^<
    int age5 = new Random().nextInt(42);
//             ^^^^^^^^^^^^<
    MyRandom myRandom = new MyRandom(); // Compliant
  }

  @Test
  public void notRandomizedTest() {
    int userAge = 31; // Compliant
    UUID userID = UUID.fromString("00000000-000-0000-0000-000000000001"); //Compliant
  }

  class MyRandom {
  }

  @Test
  public void randomizedTestWithSeed() {
    int userAge = new Random(111111111111L).nextInt(42);
//                ^^^^^^^^^^^^^^^^^^^^^^^^^<
  }

}

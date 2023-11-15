package checks.tests;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class NonRandomizedTestDataCheck {

  @Test
  public void randomizedTest() {
    int userAge = new MyRandom().nextInt(42);
    UUID userID = UUID.fromString("00000000-000-0000-0000-000000000001");

  }

  @Test
  public void secondaryLocations() {
    int age1 = new MyRandom().nextInt(42);
    UUID u1 = UUID.fromString("00000000-000-0000-0000-000000000001");
    int age2 = new MyRandom().nextInt(42);
    UUID u2 = UUID.fromString("00000000-000-0000-0000-000000000002");
    int age3 = new MyRandom().nextInt(42);
    UUID u3 = UUID.fromString("00000000-000-0000-0000-000000000003");
    int age4 = new MyRandom().nextInt(42);
    UUID u4 = UUID.fromString("00000000-000-0000-0000-000000000004");
    int age5 = new MyRandom().nextInt(42);
    UUID u5 = UUID.fromString("00000000-000-0000-0000-000000000005");
  }

  class MyRandom {
    int nextInt(int bound) {
      return 31;
    }
  }

}

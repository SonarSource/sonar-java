import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SampleWithNestedTestClass {

  @Nested
  class Inner {

    @Test
    void does_something() {
    }
  }
}

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

class SampleWithDeeplyNestedTestAnnotation {

  static class Holder {

    @ExtendWith(MockitoExtension.class)
    static class InnerTest {
    }
  }
}

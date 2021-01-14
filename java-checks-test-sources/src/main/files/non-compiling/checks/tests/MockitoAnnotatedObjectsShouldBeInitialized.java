package checks.tests;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

class MockitoAnnotatedObjectsShouldBeInitialized {

  @ExtendWith(MockitoExtension.class)
  public class FooTest {
    @Mock // Compliant
    private Bar bar;
  }

  private class Bar {
  }

}

package checks.tests;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public class MockitoAnnotatedObjectsShouldBeInitializedSuperClass {
  @BeforeEach
  void baseSetUp() {
    MockitoAnnotations.initMocks(this);
  }
}

package checks.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.mockito.Mockito.mock;

public class MockFieldShouldUseMockAnnotationSample {

  interface PaymentService {
    void process();
  }

  interface NotificationService {
    void notify(String msg);
  }

  interface DataSource {
    String getData();
  }

  // ===== JUnit 5 - @ExtendWith(MockitoExtension.class) =====

  @ExtendWith(MockitoExtension.class)
  class JUnit5WithExtendWith {
    private final PaymentService paymentService = mock(PaymentService.class); // Noncompliant {{Use "@Mock" annotation instead of "mock()" for field declaration.}}
    //                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^
    private final NotificationService notificationService = mock(NotificationService.class); // Noncompliant {{Use "@Mock" annotation instead of "mock()" for field declaration.}}
    //                                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    @Mock
    private DataSource dataSource; // Compliant - already uses @Mock

    @Test
    void testProcessOrder() {
      DataSource adHocMock = mock(DataSource.class); // Compliant - local variable, not a field
    }
  }

  @ExtendWith(MockitoExtension.class)
  class JUnit5WithStaticImport {
    private PaymentService service = mock(PaymentService.class); // Noncompliant
  }

  @ExtendWith(MockitoExtension.class)
  class JUnit5WithQualifiedCall {
    private PaymentService service = Mockito.mock(PaymentService.class); // Noncompliant
  }

  @ExtendWith(MockitoExtension.class)
  class JUnit5Compliant {
    @Mock
    private PaymentService service; // Compliant

    private PaymentService nonMock = new PaymentService() { // Compliant - not a mock
      public void process() {}
    };
  }

  // ===== JUnit 4 - @RunWith(MockitoJUnitRunner.class) =====

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4WithMockitoRunner {
    private final PaymentService paymentService = mock(PaymentService.class); // Noncompliant
    private final NotificationService notificationService = mock(NotificationService.class); // Noncompliant

    @Mock
    private DataSource dataSource; // Compliant

    @org.junit.Test
    public void testProcessOrder() {
      DataSource adHocMock = mock(DataSource.class); // Compliant - local variable
    }
  }

  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public class JUnit4WithStrictStubsRunner {
    private PaymentService service = mock(PaymentService.class); // Noncompliant
  }

  @RunWith(MockitoJUnitRunner.Silent.class)
  public class JUnit4WithSilentRunner {
    private PaymentService service = mock(PaymentService.class); // Noncompliant
  }

  // ===== Meta-annotations =====

  @MockitoSettings
  class WithMockitoSettings {
    private PaymentService service = mock(PaymentService.class); // Noncompliant
  }

  // ===== Compliant - no Mockito runner/extension =====

  class NoAnnotation {
    private PaymentService service = mock(PaymentService.class); // Compliant - no Mockito runner
  }

  @RunWith(MockitoJUnitRunner.class)
  public class AnnotatedButNoMockInit {
    @Mock
    private PaymentService service; // Compliant

    @org.junit.Test
    public void test() {
      PaymentService adHoc = mock(PaymentService.class); // Compliant - inside method body
    }
  }

  @ExtendWith(org.junit.jupiter.api.extension.Extension.class)
  class NonMockitoExtension {
    private PaymentService service = mock(PaymentService.class); // Compliant - not a Mockito extension
  }

  // ===== Multiple extensions =====

  @ExtendWith({MockitoExtension.class, org.junit.jupiter.api.extension.Extension.class})
  class WithMultipleExtensions {
    private PaymentService service = mock(PaymentService.class); // Noncompliant
  }
}

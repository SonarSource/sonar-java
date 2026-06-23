package checks.tests;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MockitoStaticImportCheckSample {

  interface MyService {
    int getValue();
    String process(String input);
  }

  @Test
  void noncompliant_mock() {
    MyService service = Mockito.mock(MyService.class); // Noncompliant {{Use a static import for "mock".}}
    //                  ^^^^^^^^^^^^
    verify(service).getValue();
  }

  @Test
  void noncompliant_when() {
    MyService service = mock(MyService.class);
    Mockito.when(service.getValue()).thenReturn(42); // Noncompliant {{Use a static import for "when".}}
  //^^^^^^^^^^^^
    verify(service).getValue();
  }

  @Test
  void noncompliant_spy_on_instance() {
    MyService service = mock(MyService.class);
    MyService spied = Mockito.spy(service); // Noncompliant {{Use a static import for "spy".}}
    verify(service).getValue();
  }

  @Test
  void noncompliant_spy_on_class() {
    MyService spied = Mockito.spy(MyService.class); // Noncompliant {{Use a static import for "spy".}}
    verify(spied).getValue();
  }

  @Test
  void noncompliant_doReturn() {
    MyService spied = spy(MyService.class);
    Mockito.doReturn(42).when(spied).getValue(); // Noncompliant {{Use a static import for "doReturn".}}
    verify(spied).getValue();
  }

  @Test
  void noncompliant_doThrow() {
    MyService spied = spy(MyService.class);
    Mockito.doThrow(new RuntimeException()).when(spied).getValue(); // Noncompliant {{Use a static import for "doThrow".}}
    verify(spied).getValue();
  }

  @Test
  void noncompliant_verify() {
    MyService service = mock(MyService.class);
    service.getValue();
    Mockito.verify(service).getValue(); // Noncompliant {{Use a static import for "verify".}}
  }

  @Test
  void noncompliant_times() {
    MyService service = mock(MyService.class);
    service.getValue();
    service.getValue();
    verify(service, Mockito.times(2)).getValue(); // Noncompliant {{Use a static import for "times".}}
  }

  @Test
  void noncompliant_never() {
    MyService service = mock(MyService.class);
    verify(service, Mockito.never()).getValue(); // Noncompliant {{Use a static import for "never".}}
  }

  @Test
  void noncompliant_any() {
    MyService service = mock(MyService.class);
    when(service.process(Mockito.any())).thenReturn("result"); // Noncompliant {{Use a static import for "any".}}
    verify(service).getValue();
  }

  @Test
  void noncompliant_eq() {
    MyService service = mock(MyService.class);
    when(service.process(Mockito.eq("input"))).thenReturn("result"); // Noncompliant {{Use a static import for "eq".}}
    verify(service).getValue();
  }

  @Test
  void noncompliant_verify_and_times() {
    MyService service = mock(MyService.class);
    service.getValue();
    service.getValue();
    Mockito.verify(service, // Noncompliant {{Use a static import for "verify".}}
      Mockito.times(2)) // Noncompliant {{Use a static import for "times".}}
      .getValue();
  }

  @Test
  void noncompliant_verify_and_never() {
    MyService service = mock(MyService.class);
    Mockito.verify(service, // Noncompliant {{Use a static import for "verify".}}
      Mockito.never()) // Noncompliant {{Use a static import for "never".}}
      .getValue();
  }

  @Test
  void noncompliant_when_and_any() {
    MyService service = mock(MyService.class);
    Mockito.when( // Noncompliant {{Use a static import for "when".}}
      service.process(Mockito.any())) // Noncompliant {{Use a static import for "any".}}
      .thenReturn("result");
    verify(service).getValue();
  }

  @Test
  void noncompliant_mock_inside_when() {
    Mockito.when( // Noncompliant {{Use a static import for "when".}}
      Mockito.mock(MyService.class).getValue()) // Noncompliant {{Use a static import for "mock".}}
      .thenReturn(42);
    verify(Mockito.mock(MyService.class)).getValue(); // Noncompliant {{Use a static import for "mock".}}
  }

  @Test
  void compliant_all_methods() {
    MyService service = mock(MyService.class);
    MyService spied = spy(service);

    when(service.getValue()).thenReturn(42);
    when(service.process(any())).thenReturn("result");
    when(service.process(eq("input"))).thenReturn("result");

    doReturn(42).when(spied).getValue();
    doThrow(new RuntimeException()).when(spied).getValue();

    service.getValue();
    service.getValue();
    verify(service, times(2)).getValue();
    verify(service, never()).process(any());
  }

  @Test
  void noncompliant_inOrder() {
    MyService service = mock(MyService.class);
    Mockito.inOrder(service); // Noncompliant {{Use a static import for "inOrder".}}
    verify(service).getValue();
  }

  @Test
  void noncompliant_reset() {
    MyService service = mock(MyService.class);
    Mockito.reset(service); // Noncompliant {{Use a static import for "reset".}}
    verify(service).getValue();
  }

  @Test
  void noncompliant_doCallRealMethod() {
    MyService spied = spy(MyService.class);
    Mockito.doCallRealMethod().when(spied).getValue(); // Noncompliant {{Use a static import for "doCallRealMethod".}}
    verify(spied).getValue();
  }

  @Test
  void noncompliant_doNothing() {
    MyService spied = spy(MyService.class);
    Mockito.doNothing().when(spied).getValue(); // Noncompliant {{Use a static import for "doNothing".}}
    verify(spied).getValue();
  }

  @Test
  void noncompliant_doAnswer() {
    MyService spied = spy(MyService.class);
    Mockito.doAnswer(invocation -> 42).when(spied).getValue(); // Noncompliant {{Use a static import for "doAnswer".}}
    verify(spied).getValue();
  }

  @Test
  void compliant_extended_methods() {
    MyService service = mock(MyService.class);
    MyService spied = spy(MyService.class);

    inOrder(service);
    reset(service);
    doCallRealMethod().when(spied).getValue();
    doNothing().when(spied).getValue();
    doAnswer(invocation -> 42).when(spied).getValue();
    verify(service).getValue();
  }

  @Test
  void compliant_argument_matchers_prefix() {
    MyService service = mock(MyService.class);
    // Using ArgumentMatchers. prefix rather than Mockito. is not flagged by this rule
    when(service.process(org.mockito.ArgumentMatchers.any())).thenReturn("result");
    when(service.process(org.mockito.ArgumentMatchers.eq("input"))).thenReturn("result");
    verify(service).getValue();
  }
}


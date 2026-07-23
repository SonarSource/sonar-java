package checks.tests;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static checks.tests.MockitoStaticImportCheckNameConflictSample.CustomAssertions.verify;

class MockitoStaticImportCheckNameConflictSample {

  interface MyService {
    int getValue();
  }

  // Simulates a non-Mockito library (e.g. WireMock) that also exposes a verify() method
  static class CustomAssertions {
    public static void verify(Object o) {}
  }

  // static import of a non-Mockito verify() is in scope — Mockito.verify() prefix is required to disambiguate
  @Test
  void compliant_static_import_conflict() {
    MyService service = mock(MyService.class);
    service.getValue();
    Mockito.verify(service).getValue(); // Compliant
  }

  // local method named verify() is in scope — Mockito.verify() prefix is required to disambiguate
  static class WithLocalVerify {
    void verify(Object o) {}

    @Test
    void compliant_local_method_conflict() {
      MyService service = mock(MyService.class);
      service.getValue();
      Mockito.verify(service).getValue(); // Compliant
    }
  }

}

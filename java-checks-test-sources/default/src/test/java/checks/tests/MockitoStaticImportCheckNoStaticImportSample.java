package checks.tests;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MockitoStaticImportCheckNoStaticImportSample {

  interface MyService {
    int getValue();
  }

  @Test
  void compliant_consistent_prefixed_usage() {
    MyService service = Mockito.mock(MyService.class); // Compliant - no static import of Mockito methods in this file
    MyService spied = Mockito.spy(service);

    Mockito.when(service.getValue()).thenReturn(42);
    Mockito.doReturn(42).when(spied).getValue();

    service.getValue();
    Mockito.verify(service, Mockito.times(1)).getValue();
    Mockito.verify(service, Mockito.never()).hashCode();
  }

}

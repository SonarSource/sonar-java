package checks.unused;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;

public class UnusedPrivateMethodWithUknownResolution {

  private void init(@Observes Object object) {} // Compliant
  private void init2(@UnknownAnnotation Object object) {} // Noncompliant

  void sonarJava5012Minimal() {
    Z b = () -> bar();
  }

  class Bar {
    private static void bar() {
    }
  }

  List<ClientHttpRequestInterceptor> sonarJava5012AsReported() {
    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

    User userToAuthenticate = new User();

    interceptors.add((request, body, execution) -> {
      request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + userToAuthenticate.token());
      return execution.execute(request, body);
    });

    return interceptors;
  }

  class User {
    public User() {}
    private String token() { // Compliant
      return "123456";
    }
  }
}

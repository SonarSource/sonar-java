import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class JavaNet11 {
  void foo(HttpRequest request, HttpResponse.BodyHandler<Object> responseBodyHandler, HttpResponse.PushPromiseHandler<Object> pushPromiseHandler) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    client.send(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, responseBodyHandler, pushPromiseHandler); // Noncompliant
  }
}

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

class JavaNet10 {
  void foo(HttpRequest request, HttpResponse.BodyHandler<Object> responseBodyHandler, HttpResponse.MultiSubscriber<?, ?> multiSubscriber) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    client.send(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, multiSubscriber); // Noncompliant
  }
}

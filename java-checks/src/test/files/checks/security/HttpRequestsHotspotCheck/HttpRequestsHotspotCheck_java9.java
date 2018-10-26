import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

class JavaNet9 {
  void foo(HttpRequest request, HttpResponse.BodyHandler<Object> responseBodyHandler, HttpResponse.MultiProcessor<?, ?> multiProcessor) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    client.send(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, responseBodyHandler); // Noncompliant
    client.sendAsync(request, multiProcessor); // Noncompliant
  }
}

package checks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class TryWithResourcesCheck_java_21 {

  void httpClientIsAutoCloseableAsOfJava21(HttpRequest request) throws IOException, InterruptedException {
    HttpClient client = null;
    var o = new Object();
    try { // Noncompliant {{Change this "try" to a try-with-resources.}}
      client = HttpClient.newBuilder().build();
      client.send(request, HttpResponse.BodyHandlers.ofString());
    } finally {
      if (client != null) {
        client.close();
      }
    }

    try { // Noncompliant
      client = HttpClient.newHttpClient();
      client.send(request, HttpResponse.BodyHandlers.ofString());
    } finally {
      client.close();
    }

  }

}

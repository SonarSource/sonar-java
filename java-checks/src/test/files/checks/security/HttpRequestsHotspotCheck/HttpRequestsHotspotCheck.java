import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;

// === Java URL connection ===
abstract class URLConnection {
  void foo() throws Exception {
    URL url = new URL("http://example.com");
    HttpURLConnection con = (HttpURLConnection) url.openConnection(); // Noncompliant [[sc=29;ec=69]] {{Make sure that this http request is sent safely.}}
    con.getContent(); // No issue here as it was raised when cast to HttpURLConnection

    doSomething((HttpURLConnection) url.openConnection()); // Noncompliant
  }

  abstract void doSomething(HttpURLConnection httpUrlConnection);
}

// === apache ===
class ApacheHttpClient {
  void foo(HttpClientConnection con, HttpHost target, org.apache.http.HttpRequest request, HttpContext context,
    ResponseHandler<?> responseHandler, HttpUriRequest uriRequest, HttpEntityEnclosingRequest eeRequest)
    throws Exception {
    HttpClient client = HttpClientBuilder.create().build();
    client.execute(target, request); // Noncompliant [[sc=12;ec=19]] {{Make sure that this http request is sent safely.}}
    client.execute(target, request, context); // Noncompliant
    client.execute(target, request, responseHandler); // Noncompliant
    client.execute(target, request, responseHandler, context); // Noncompliant
    client.execute(uriRequest); // Noncompliant
    client.execute(uriRequest, context); // Noncompliant
    client.execute(uriRequest, responseHandler); // Noncompliant
    client.execute(uriRequest, responseHandler, context); // Noncompliant

    con.sendRequestEntity(eeRequest); // Noncompliant
    con.sendRequestHeader(request); // Noncompliant
  }
}

// === google-http-java-client ===
class GoogleHttpClient {
  void foo(Executor executor) throws Exception {

    HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    HttpRequest request = requestFactory.buildGetRequest(new GenericUrl("http://example.com"));

    request.execute(); // Noncompliant
    request.executeAsync(); // Noncompliant
    request.executeAsync(executor); // Noncompliant
  }
}

package checks;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class BasicAuthCheckSampleWithoutSemantic {
  void foo(String authent, URL url) throws IOException, URISyntaxException {
    String encoding = Base64.getEncoder().encodeToString("login:passwd".getBytes(StandardCharsets.UTF_8));
    org.apache.http.client.methods.HttpPost httppost = new org.apache.http.client.methods.HttpPost(url.toURI());

    httppost.setHeader("Authorization", "Digest " + encoding);
    httppost.setHeader("Authorization", authent);
    httppost.setHeader("FlaFla", "Basic " + encoding);

    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Authorization", "Basic " + encoding); // Noncompliant
    conn.setRequestProperty("Authorization", ("Basic " + encoding + encoding)); // Noncompliant
    conn.addRequestProperty("Authorization", ("Basic " + encoding) + encoding); // Noncompliant
  }
}

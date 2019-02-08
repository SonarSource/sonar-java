class A{
  void foo(String authent) {
    String encoding = Base64Encoder.encode ("login:passwd");
    org.apache.http.client.methods.HttpPost httppost = new org.apache.http.client.methods.HttpPost(url);
    httppost.setHeader("Authorization", "Basic " + encoding+encoding);  // Noncompliant [[sc=41;ec=49]] {{Use a more secure method than basic authentication.}}
    httppost.addHeader("Authorization", "Basic " + encoding);  // Noncompliant
    httppost.setHeader("Authorization", "Digest " + encoding);
    httppost.setHeader("Authorization", authent);
    httppost.setHeader("FlaFla", "Basic " + encoding);
    httppost.setHeader(new org.apache.http.message.BasicHeader("Authorization", "Basic " + encoding)); // Noncompliant

    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Authorization", "Basic " + encoding); // Noncompliant
    conn.setRequestProperty("Authorization", ("Basic " + encoding + encoding)); // Noncompliant
    conn.addRequestProperty("Authorization", ("Basic " + encoding) + encoding); // Noncompliant
  }
}

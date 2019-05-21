package test;

import android.webkit.WebView;

public class AndroidSSLConnection {
  public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) { // Noncompliant {{Make sure that SSL/TLS connections are validated safely here.}}
    // ...
  }

  public void onReceivedSslError(WebView view) { // Noncompliant [[sc=15;ec=33]]
    // ...
  }

  public boolean onReceivedSslError(WebView view) { // Noncompliant
    // ...
  }

  public void onReceivedSslError() { // OK
    // ...
  }

  public void onReceivedSslError(String view) { // OK
    // ...
  }

  public void anotherMethodName(WebView view) { // OK
    // ...
  }
}

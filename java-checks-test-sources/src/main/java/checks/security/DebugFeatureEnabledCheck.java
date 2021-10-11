package checks.security;

import android.webkit.WebView;
import android.webkit.WebViewFactoryProvider;
import java.lang.reflect.InvocationTargetException;

// Tests for printStackTrace.
// Tests for @EnableWebSecurity are in files/non-compiling/checks/security/DebugFeatureEnabledCheck.java
public class DebugFeatureEnabledCheck {
  private void f(Throwable e, MyException e1) {
    e.printStackTrace(); // Noncompliant
    e.printStackTrace(System.out); // Compliant - forcing the stream
    e.getMessage(); // Compliant
    new java.lang.Throwable().printStackTrace(); // Noncompliant
    String s = e1.printStackTrace[0]; // Compliant
    printStackTrace();
  }

  private void androidWebView(WebViewFactoryProvider.Statics statics) {
    WebView.setWebContentsDebuggingEnabled(true); // Noncompliant [[sc=13;ec=43] {{Make sure this debug feature is deactivated before delivering the code in production.}}
    WebView.setWebContentsDebuggingEnabled(false);
    statics.setWebContentsDebuggingEnabled(true); // Noncompliant
    statics.setWebContentsDebuggingEnabled(false);
  }

  void printStackTrace() {
  }

  void fun(MyException e) {
    e.printStackTrace(); // Noncompliant
  }

  void fun(CustomException e) {
    e.printStackTrace(); //Compliant : e is not extending Throwable
    DebugFeatureEnabledCheck.CustomException.printStackTrace(); //compliant : CustomException is not extending Throwable
  }

  void fun(InvocationTargetException ite) {
    ite.getTargetException().printStackTrace(); // Noncompliant
  }

  static class CustomException {
    void printStackTrace(Throwable e) {

    }

    static void printStackTrace() {
    }
  }

  static class MyException extends Throwable {
    public String[] printStackTrace;

    @Override
    public void printStackTrace() {
    }

    void fun() {
      MyException ex = new MyException();
      ex.printStackTrace();
    }
  }
}

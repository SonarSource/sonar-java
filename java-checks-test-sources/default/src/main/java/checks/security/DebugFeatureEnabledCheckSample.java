package checks.security;

import android.webkit.WebView;
import android.webkit.WebViewFactoryProvider;
import java.lang.reflect.InvocationTargetException;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

// Tests for printStackTrace.
// Tests for @EnableWebSecurity are in files/non-compiling/checks/security/DebugFeatureEnabledCheckSample.java
public class DebugFeatureEnabledCheckSample {
  private void f(Throwable e, MyException e1) {
    e.printStackTrace(); // Noncompliant
    e.printStackTrace(System.out); // Compliant - forcing the stream
    e.getMessage(); // Compliant
    new java.lang.Throwable().printStackTrace(); // Noncompliant
    String s = e1.printStackTrace[0]; // Compliant
    printStackTrace();
  }

  private void androidWebView(WebViewFactoryProvider.Statics statics) {
    WebView.setWebContentsDebuggingEnabled(true); // Noncompliant {{Make sure this debug feature is deactivated before delivering the code in production.}}
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
    DebugFeatureEnabledCheckSample.CustomException.printStackTrace(); //compliant : CustomException is not extending Throwable
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

  void foo(WebSecurity web, boolean cond){
    web.debug(true); // Noncompliant {{Make sure this debug feature is deactivated before delivering the code in production.}}
    web.debug(false);
    web.debug(cond);
  }

  public WebSecurityCustomizer debugCustomizer() {
    return (web) -> web.debug(true); // Noncompliant {{Make sure this debug feature is deactivated before delivering the code in production.}}
  }

  public WebSecurityCustomizer nonDebugCustomizer() {
    return (web) -> web.debug(false);
  }

}

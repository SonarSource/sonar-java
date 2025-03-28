package checks.security;

import android.annotation.JavascriptInterface;
import android.webkit.WebView;
import java.util.function.Consumer;

public class WebViewJavaScriptInterfaceCheck {
  private WebView webViewProperty = new WebView();

  public static class JsObject {
    @JavascriptInterface
    @Override
    public String toString() {
      return "injectedObject";
    }
  }

  public static class NotAWebView {
    public void addJavascriptInterface(Object obj, String name) {
    }
  }

  public static class WebViewChild extends WebView {
    public WebViewChild() {
    }
  }

  public static class WebViewGrandChild extends WebViewChild {
    @Override
    public void addJavascriptInterface(Object obj, String name) {
    }
  }

  public void nonCompliantScenarios(WebView webViewParam) {
    webViewParam.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant {{Exposing a Javascript interface can expose sensitive information to attackers. Make sure it is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    new WebView().addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
    webViewProperty.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
    WebView webViewLocal = new WebView();
    webViewLocal.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
    final WebView finalWebViewLocal = new WebView();
    finalWebViewLocal.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant

    JsObject jsObjectLocal = new JsObject();
    webViewParam.addJavascriptInterface(jsObjectLocal, "injectedObject"); // Noncompliant
    final String finalInjectedName = "injectedObject";
    webViewParam.addJavascriptInterface(new JsObject(), finalInjectedName); // Noncompliant
    webViewParam.addJavascriptInterface(jsObjectLocal, finalInjectedName); // Noncompliant

    WebView webViewClosure = new WebView();
    String injectedNameClosure = "injectedObject";
    JsObject jsObjectLocalClosure = new JsObject();
    Runnable closure = () -> webViewClosure.addJavascriptInterface(jsObjectLocalClosure, injectedNameClosure); // Noncompliant

    webViewParam.addJavascriptInterface(true ? new JsObject() : new JsObject(), "injectedObject"); // Noncompliant
    webViewParam.addJavascriptInterface(jsObjectLocal, finalInjectedName.toString()); // Noncompliant
    webViewParam.addJavascriptInterface(new JsObject(), finalInjectedName + "suffix"); // Noncompliant
    if (webViewParam != null) {
      webViewParam.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
    }
    Runnable fieldClosure = () -> {
      webViewParam.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
    };

    WebViewChild derivedFromWebView = new WebViewChild();
    derivedFromWebView.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant

    WebView derivedFromWebViewBaseType = new WebViewChild();
    derivedFromWebViewBaseType.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant

    WebViewGrandChild derivedFromWebViewGrandChild = new WebViewGrandChild();
    derivedFromWebViewGrandChild.addJavascriptInterface(new JsObject(), "injectedObject"); // Noncompliant
  }

  public void compliantScenarios(WebView webView, NotAWebView notAWebView) {
    new WebView(); // Compliant, no method invoked on the WebView
    webView.hashCode(); // Compliant, different method invoked on the WebView
    notAWebView.addJavascriptInterface(new JsObject(), "injectedObject"); // Compliant, not a WebView
    Consumer<NotAWebView> consumer = (NotAWebView view) -> {
      view.addJavascriptInterface(new JsObject(), "injectedObject"); // Compliant, not a WebView
    };
  }
}

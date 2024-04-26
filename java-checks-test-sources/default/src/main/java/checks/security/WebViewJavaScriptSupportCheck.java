package checks.security;

import android.webkit.WebSettings;

class WebViewJavaScriptSupportCheck {
  void foo(WebSettings settings, boolean value) {
    settings.setJavaScriptEnabled(true); // Noncompliant {{Make sure that enabling JavaScript support is safe here.}}
//                                ^^^^
    settings.setJavaScriptEnabled(false); // Compliant
    settings.setJavaScriptEnabled(value); // Compliant
  }
}

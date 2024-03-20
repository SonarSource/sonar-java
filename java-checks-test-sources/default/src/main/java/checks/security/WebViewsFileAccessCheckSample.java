package checks.security;

import android.webkit.WebSettings;

class WebViewsFileAccessCheckSample {
  void foo(WebSettings settings, boolean value) {
    settings.setAllowFileAccess(true); // Noncompliant [[sc=33;ec=37]] {{Make sure that enabling file access is safe here.}}
    settings.setAllowFileAccess(false); // Compliant
    settings.setAllowFileAccess(value); // Compliant

    settings.setAllowContentAccess(true); // Noncompliant [[sc=36;ec=40]] {{Make sure that enabling file access is safe here.}}
    settings.setAllowContentAccess(false); // Compliant
    settings.setAllowContentAccess(value); // Compliant

    settings.setAllowFileAccessFromFileURLs(true); // Noncompliant [[sc=45;ec=49]] {{Make sure that enabling file access is safe here.}}
    settings.setAllowFileAccessFromFileURLs(false); // Compliant
    settings.setAllowFileAccessFromFileURLs(value); // Compliant

    settings.setAllowUniversalAccessFromFileURLs(true); // Noncompliant [[sc=50;ec=54]] {{Make sure that enabling file access is safe here.}}
    settings.setAllowUniversalAccessFromFileURLs(false); // Compliant
    settings.setAllowUniversalAccessFromFileURLs(value); // Compliant
  }

}

package checks.security;

import android.webkit.WebSettings;

class WebViewsFileAccessCheck {
  void foo(WebSettings settings, boolean value) {
    settings.setAllowFileAccess(true); // Noncompliant {{Make sure that enabling file access is safe here.}}
//                              ^^^^
    settings.setAllowFileAccess(false); // Compliant
    settings.setAllowFileAccess(value); // Compliant

    settings.setAllowContentAccess(true); // Noncompliant {{Make sure that enabling file access is safe here.}}
//                                 ^^^^
    settings.setAllowContentAccess(false); // Compliant
    settings.setAllowContentAccess(value); // Compliant

    settings.setAllowFileAccessFromFileURLs(true); // Noncompliant {{Make sure that enabling file access is safe here.}}
//                                          ^^^^
    settings.setAllowFileAccessFromFileURLs(false); // Compliant
    settings.setAllowFileAccessFromFileURLs(value); // Compliant

    settings.setAllowUniversalAccessFromFileURLs(true); // Noncompliant {{Make sure that enabling file access is safe here.}}
//                                               ^^^^
    settings.setAllowUniversalAccessFromFileURLs(false); // Compliant
    settings.setAllowUniversalAccessFromFileURLs(value); // Compliant
  }

}

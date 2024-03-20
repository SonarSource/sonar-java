package checks;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class URLHashCodeAndEqualsCheckSample {
  public void foo(URL url) throws Exception {
    Map<URL, Integer> sites = new HashMap<>(); // Noncompliant [[sc=5;ec=22]] {{Use the URI class instead.}}
    Set<URL> otherSites = new HashSet<>(); // Noncompliant {{Use the URI class instead.}}

    URL homepage = new URL("http://sonarsource.com"); // Compliant
    homepage.equals(url); // Noncompliant [[sc=5;ec=25]] {{Use the URI class instead.}}
    homepage.hashCode(); // Noncompliant {{Use the URI class instead.}}

    homepage.getPath(); // Compliant
    Map<URI, URL> uriToUrl = new HashMap<>(); // Compliant
    Map uriToUrl2 = new HashMap(); // Compliant
    Map uriToUrl3 = new HashMap<>(); // Compliant
  }
}

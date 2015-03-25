import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class A {
  public void foo(URL url) throws Exception {
    Map<URL, Integer> sites = new HashMap<>(); // Noncompliant
    Set<URL> otherSites = new HashSet<>(); // Noncompliant

    URL homepage = new URL("http://sonarsource.com"); // Compliant
    homepage.equals(url); // Noncompliant
    homepage.hashCode(); // Noncompliant
    
    homepage.getPath(); // Compliant
    Map<URI, URL> uriToUrl = new HashMap<>(); // Compliant
    Map uriToUrl2 = new Hashmap(); // Compliant
    Map<> uriToUrl2 = new Hashmap<>(); // Compliant
  }
}
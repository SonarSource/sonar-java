import javax.servlet.http.Cookie;
import java.net.HttpCookie;
class A {
 void foo() {
   Cookie myCookie = new Cookie("name", "val");
   myCookie.setDomain(".com"); // Noncompliant [[sc=23;ec=29]] {{Specify at least a second-level cookie domain.}}
   myCookie.setDomain(".myDomain.com"); // Compliant
 }
 void HttpCookie(String domain) {
   HttpCookie myCookie = new Cookie("name", "val");
   myCookie.setDomain(".com"); // Noncompliant
   myCookie.setDomain(".myDomain.com"); // Compliant
   myCookie.setDomain(domain); // Compliant
 }
}

import javax.servlet.http.Cookie;
import java.net.HttpCookie;
class A {
 void foo() {
   Cookie myCookie = new Cookie("name", "val");
   myCookie.setDomain(".com"); // Noncompliant [[sc=23;ec=29]] {{Do not set cookies for 'com' as it is a public suffix.}}
   myCookie.setDomain(".mydomain.com"); // Compliant

   myCookie.setDomain(".co.uk"); // Noncompliant [[sc=23;ec=31]] {{Do not set cookies for 'co.uk' as it is a public suffix.}}
   myCookie.setDomain(".taiki.hokkaido.jp"); // Noncompliant [[sc=23;ec=43]] {{Do not set cookies for 'taiki.hokkaido.jp' as it is a public suffix.}}
   myCookie.setDomain(".github.io"); // Noncompliant [[sc=23;ec=35]] {{Do not set cookies for 'github.io' as it is a public suffix.}}
   myCookie.setDomain(".javaee.github.io"); // Compliant

   myCookie.setDomain(".unknowntosonar"); // Noncompliant [[sc=23;ec=40]] {{Specify at least a second-level cookie domain.}}
   myCookie.setDomain(".mysite.unknowntosonar"); // Compliant

   myCookie.setDomain(""); // Compliant
 }
 void HttpCookie(String domain) {
   HttpCookie myCookie = new HttpCookie("name", "val");
   myCookie.setDomain(".com"); // Noncompliant
   myCookie.setDomain(".mydomain.com"); // Compliant

   myCookie.setDomain(".co.uk"); // Noncompliant
   myCookie.setDomain(".taiki.hokkaido.jp"); // Noncompliant
   myCookie.setDomain(".github.io"); // Noncompliant
   myCookie.setDomain(".javaee.github.io"); // Compliant

   myCookie.setDomain(".unknowntosonar"); // Noncompliant
   myCookie.setDomain(".mysite.unknowntosonar"); // Compliant

   myCookie.setDomain(domain); // Compliant
 }
}

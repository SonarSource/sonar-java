import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;
import org.apache.commons.lang.StringEscapeUtils;

class A {
  void foo(HttpServletRequest request, HttpSession session, boolean test) {
    String data1 = request.getParameter("");
    session.setAttribute("", data1); // Noncompliant {{Make sure the user is authenticated before this data is stored in the session.}}
    session.setAttribute("", request.getParameter(""));  // Noncompliant
    
    Cookie cookie1 = getCookie();
    String data2 = cookie1.getValue();
    session.setAttribute("", data2); // Noncompliant
    session.setAttribute("", cookie1.getValue()); // Noncompliant
    session.putValue("", data2); // Noncompliant
    request.getSession().putValue("", data2); // Noncompliant
    
    String data3 = request.getParameter("");
    if("data3".equals(data3)) {
      session.setAttribute("", data3); // Compliant, variable have been "used" between its declaration and its store
    }
    
    data3 = request.getProtocol();
    foo(data3);
    session.setAttribute("", data3); // Compliant, variable have been "used" between its declaration and its store
    
    data3 = request.getHeader("");
    session.setAttribute("", data3); // Noncompliant
    
    data3 = request.getHeader(""); session.setAttribute("", data3); // Noncompliant
    
    session.setAttribute("", ""); // Compliant
    session.setAttribute("", foo()); // Compliant
    
    String data4 = "";
    session.setAttribute("", data4); // Compliant
    String data5 = foo();
    session.setAttribute("", data5); // Compliant
    
    String data;
    if (test) {
      data = request.getParameter("");
    } else {
      data = "";
    }
    session.setAttribute("", data); // False Negative - assignment of the then clause is not considered as being the last one (ReassignmentFinder)
    
    String dataNotInitialized;
    session.setAttribute("", dataNotInitialized); // Compliant
    
    String dataUsedAfter = request.getHeader("");
    session.setAttribute("", dataUsedAfter); // Noncompliant 
    foo(dataUsedAfter);
  }
  
  String foo() {
    return "";
  }
  
  String foo(String s) {
    return s;
  }
  
  Cookie getCookie() {
    return null;
  }
  
  void no_effect_operation1(HttpServletRequest request) throws Exception {
    Cookie[] cookies = request.getCookies();
    
    String param = "";
    if (theCookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals("vector")) {
          param = URLDecoder.decode(cookie.getValue(), "UTF-8");
          break;
        }
      }
    }
    
    request.getSession().setAttribute( param, "10340"); // Noncompliant
  }
  
  void no_effect_operation2(HttpServletRequest request) throws Exception {
    Cookie[] theCookies = request.getCookies();
    
    String param = "";
    if (theCookies != null) {
      for (Cookie theCookie : theCookies) {
        if (theCookie.getName().equals("vector")) {
          param = URLDecoder.decode(theCookie.getValue(), "UTF-8");
          break;
        }
      }
    }
    
    String bar = StringEscapeUtils.escapeHtml(param);
    
    request.getSession().putValue( "userid", bar); // Noncompliant
  }
}

class B {

  public void doPost1(HttpServletRequest request) throws Exception {
    String param = "";
    param = URLDecoder.decode(param, "UTF-8");

    request.getSession().putValue("userid", param); // Compliant
  }

  public void doPost2(HttpServletRequest request) throws Exception {
    String param = request.getParameter("");
    param = URLDecoder.decode(param, "UTF-8");

    request.getSession().putValue("userid", param); // FN - param should have been detected as being a non-protected data.
  }
}

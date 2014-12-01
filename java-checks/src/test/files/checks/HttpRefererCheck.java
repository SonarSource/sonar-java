import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String referer = request.getHeader("referer");  // Noncompliant
    String param1 = request.getHeader("param1");  // Noncompliant
    if (isTrustedReferer(referer)) {
      //..
    }
    //...
  }
}
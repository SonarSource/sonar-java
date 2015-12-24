import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String sessionId = request.getRequestedSessionId(); // Noncompliant [[sc=32;ec=53]] {{Remove use of this unsecured "getRequestedSessionId()" method}}
  }
}

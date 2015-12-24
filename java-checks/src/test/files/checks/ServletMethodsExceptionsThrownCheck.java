
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.net.InetAddress;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.ServletException;

class A extends HttpServlet {

  private static boolean var = staticMethod();

  private static boolean staticMethod() {}

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip); // Noncompliant [[sc=36;ec=45]] {{Add a "try/catch" block for "getByName".}}
    try {
      InetAddress addr = InetAddress.getByName(ip);
    } catch (IllegalArgumentException e) {
      throw e; // Noncompliant [[sc=7;ec=15]] {{Add a "try/catch" block.}}
    } catch (Exception e) {
      throw e; // Noncompliant {{Add a "try/catch" block.}}
    }
    staticMethod();
  }
  public void foo(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    throw new IllegalStateException("bla"); // Noncompliant {{Add a "try/catch" block.}}
  }
  public void bar(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    throw new IllegalStateException("bla");
  }
}

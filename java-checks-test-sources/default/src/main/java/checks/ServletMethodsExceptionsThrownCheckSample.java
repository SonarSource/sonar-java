package checks;

import java.io.IOException;
import java.net.InetAddress;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ServletMethodsExceptionsThrownCheckSample extends HttpServlet {

  private static boolean var = staticMethod();

  private static boolean staticMethod() { return true; }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip); // Noncompliant [[sc=36;ec=45]] {{Handle the following exception that could be thrown by "getByName": UnknownHostException.}}
    try {
      addr = InetAddress.getByName(ip);
    } catch (IllegalArgumentException e) {
      throw e; // Noncompliant [[sc=7;ec=15]] {{Handle the "IllegalArgumentException" thrown here in a "try/catch" block.}}
    } catch (Exception e) {
      throw e; // Noncompliant {{Handle the "Exception" thrown here in a "try/catch" block.}}
    }
    staticMethod();
  }

  public void foo(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, NamingException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) {
    throw new IllegalStateException("bla"); // Noncompliant {{Handle the "IllegalStateException" thrown here in a "try/catch" block.}}
  }
  public void bar(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    throw new IllegalStateException("bla");
  }

  protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      foo(request, response); // Noncompliant [[sc=7;ec=10]] {{Handle the following exceptions that could be thrown by "foo": IOException, ServletException.}}
    } catch (NamingException ne) {
      throw new ServletException(ne); // Noncompliant {{Handle the "ServletException" thrown here in a "try/catch" block.}}
    }
  }
}


class JakartaServletMethodsExceptionsThrownCheckSample extends jakarta.servlet.http.HttpServlet {

  private static boolean var = staticMethod();

  private static boolean staticMethod() { return true; }

  public void doGet(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws IOException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip); // Noncompliant [[sc=36;ec=45]] {{Handle the following exception that could be thrown by "getByName": UnknownHostException.}}
    try {
      addr = InetAddress.getByName(ip);
    } catch (IllegalArgumentException e) {
      throw e; // Noncompliant [[sc=7;ec=15]] {{Handle the "IllegalArgumentException" thrown here in a "try/catch" block.}}
    } catch (Exception e) {
      throw e; // Noncompliant {{Handle the "Exception" thrown here in a "try/catch" block.}}
    }
    staticMethod();
  }

  public void foo(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws IOException, jakarta.servlet.ServletException, NamingException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip);
  }

  public void doPost(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
    throw new IllegalStateException("bla"); // Noncompliant {{Handle the "IllegalStateException" thrown here in a "try/catch" block.}}
  }
  public void bar(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws IOException, jakarta.servlet.ServletException {
    throw new IllegalStateException("bla");
  }

  protected void doPut(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws jakarta.servlet.ServletException, IOException {
    try {
      foo(request, response); // Noncompliant [[sc=7;ec=10]] {{Handle the following exceptions that could be thrown by "foo": IOException, ServletException.}}
    } catch (NamingException ne) {
      throw new jakarta.servlet.ServletException(ne); // Noncompliant {{Handle the "ServletException" thrown here in a "try/catch" block.}}
    }
  }
}

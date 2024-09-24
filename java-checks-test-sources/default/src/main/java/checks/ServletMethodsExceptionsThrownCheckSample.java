package checks;

import play.Logger;

import java.io.IOException;
import java.net.InetAddress;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.function.Consumer;
import io.vavr.control.Try;

// http://localhost:9090/securityapp/s1989/noncompliantvavr
@WebServlet(urlPatterns = "/s1989/noncompliantvavr")
class S1989noncompliantvavr extends HttpServlet {

  @Override
  protected final void doGet(HttpServletRequest request, HttpServletResponse response) {

    Try.run(() -> { // Noncompliant
      // let's try to raise an exception
    });

    Try.run(() -> { // Noncompliant
      // let's try to raise an exception
    }).isEmpty();

    Try.runRunnable(() -> { // Noncompliant
      // let's try to raise an exception
    });

    Try.runRunnable(() -> { // Noncompliant
      // let's try to raise an exception
    }).isEmpty();
  }
}

// http://localhost:9090/securityapp/s1989/compliantvavr
@WebServlet(urlPatterns = "/s1989/compliantvavr")
class S1989compliantvavr extends HttpServlet {

  @Override
  protected final void doGet(HttpServletRequest request, HttpServletResponse response) {
    final Consumer<Throwable> logError = x -> Logger.error("hey"+x.getMessage(), x);
    Try.run(() -> { // Compliant
      // let's try to raise an exception
    }).onFailure(logError);

    Try.runRunnable(() -> { // Compliant
      // let's try to raise an exception
    }).onFailure(logError);

    Try.of(() -> { // Compliant
      // let's try to raise an exception
      return null;
    });
  }
}

class ServletMethodsExceptionsThrownCheckSample extends HttpServlet {

  private static boolean var = staticMethod();

  private static boolean staticMethod() { return true; }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String ip = request.getRemoteAddr();
    InetAddress addr = InetAddress.getByName(ip); // Noncompliant {{Handle the following exception that could be thrown by "getByName": UnknownHostException.}}
//                                 ^^^^^^^^^
    try {
      addr = InetAddress.getByName(ip);
    } catch (IllegalArgumentException e) {
      throw e; // Noncompliant {{Handle the "IllegalArgumentException" thrown here in a "try/catch" block.}}
//    ^^^^^^^^
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
      foo(request, response); // Noncompliant {{Handle the following exceptions that could be thrown by "foo": IOException, ServletException.}}
//    ^^^
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
    InetAddress addr = InetAddress.getByName(ip); // Noncompliant {{Handle the following exception that could be thrown by "getByName": UnknownHostException.}}
//                                 ^^^^^^^^^
    try {
      addr = InetAddress.getByName(ip);
    } catch (IllegalArgumentException e) {
      throw e; // Noncompliant {{Handle the "IllegalArgumentException" thrown here in a "try/catch" block.}}
//    ^^^^^^^^
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
      foo(request, response); // Noncompliant {{Handle the following exceptions that could be thrown by "foo": IOException, ServletException.}}
//    ^^^
    } catch (NamingException ne) {
      throw new jakarta.servlet.ServletException(ne); // Noncompliant {{Handle the "ServletException" thrown here in a "try/catch" block.}}
    }
  }
}

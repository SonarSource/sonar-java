package checks;

import io.vavr.control.Try;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.util.function.Consumer;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import play.Logger;

// @formatter:off

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

// @formatter:on

class ShouldNotRaiseIfOuterTryCatchesException extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try (Writer writer = response.getWriter()) {
      try {
      } catch (ArrayIndexOutOfBoundsException e) {
      }

      // This is a regression test:
      // There used to be an FP here because the handling of the inner catch erased the information about the outer one.
      // There is no issue here, because the IOException thrown by the method below is caught by the outer try-catch
      writer.write("Just writing stuff."); // Compliant
    } catch (IOException e) {
    }
  }
}

class ShouldHandleNestedTryCatchConstructs {
  static class ShouldDetectUncaughtExceptionInDoublyNestedTryCatch extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      try {
        try {
          throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}
        } catch (ArrayIndexOutOfBoundsException e) {
        }
      } catch (IllegalArgumentException e) {
      }
    }
  }

  static class ShouldNotRaiseForCaughtExceptionInDoublyNestedTryCatch extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
      try {
        try {
          throwIOException(); // Compliant
        } catch (ArrayIndexOutOfBoundsException e) {
        }
      } catch (IOException e) {
      }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
      try {
        try {
          throwIOException(); // Compliant
        } catch (IOException e) {
        }
      } catch (ArrayIndexOutOfBoundsException e) {
      }
    }
  }

  static class ShouldDetectUncaughtExceptionIfTryCatchIsOnLowerLevel extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      try {
        throwIOException(); // Compliant
      } catch (IOException e) {
      }

      throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
      throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}

      try {
        throwIOException(); // Compliant
      } catch (IOException e) {
      }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try {
        throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}

        try {
          throwIOException(); // Compliant
        } catch (IOException e) {
        }
      } catch (ArrayIndexOutOfBoundsException e) {

      }
    }
  }

  static class ShouldDetectForMixedExceptions extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try {
        try {
          throwIOException(); // Compliant: Caught by inner try-catch
          throwServletException(); // Compliant: Caught by outer try-catch
        } catch (IOException e) {

        }

        throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}
      } catch (ServletException e) {

      }
    }
  }

  static class ShouldDetectAcrossTryWithResources extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try {
        throwServletException();
        try (OutputStream stream = new ByteArrayOutputStream()) {
          stream.write(42); // Noncompliant {{Handle the following exception that could be thrown by "write": IOException.}}
        }
      } catch (ServletException e) {

      }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      try {
        try (OutputStream stream = new ByteArrayOutputStream()) {
          stream.write(42); // Compliant: Caught by outer try-catch
        }
      } catch (IOException e) {

      }
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      try (OutputStream stream = new ByteArrayOutputStream()) {
        try {
          stream.write(42); // Compliant
        } catch (IOException e) {

        }
      }
    }
  }

  private static void throwIOException() throws IOException {
    throw new IOException();
  }

  private static void throwServletException() throws ServletException {
    throw new ServletException();
  }
}

class ShouldHandleMultipleCatchBlocksInSeries extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    try {
      throwIOException(); // Compliant: Caught by second catch block
    } catch (IllegalArgumentException e) {

    } catch (IOException e) {

    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      throwIOException(); // Noncompliant {{Handle the following exception that could be thrown by "throwIOException": IOException.}}
    } catch (IllegalArgumentException e) {

    } catch (IllegalStateException e) {

    }
  }

  private static void throwIOException() throws IOException {
    throw new IOException();
  }
}

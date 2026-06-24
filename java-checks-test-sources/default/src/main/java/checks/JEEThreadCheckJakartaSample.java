package checks;

import jakarta.ejb.MessageDriven;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

class NonJEEClassJakarta {
  public void method() {
    Runnable r = new Runnable() { // Compliant - not in a JEE class
      public void run() {}
    };
    Runnable lambda = () -> {}; // Compliant - not in a JEE class
    synchronized (this) { // Compliant - not in a JEE class
    }
  }

  public synchronized void syncMethod() { // Compliant - not in a JEE class
  }
}

class JEEServletJakarta extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    new Thread(r).start();

    Runnable lambda = () -> {}; // Noncompliant

    synchronized (this) { // Noncompliant
    }
  }

  public synchronized void syncMethod() { // Noncompliant
  }
}

@Stateless
class StatelessEJBJakarta {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@Stateful
class StatefulEJBJakarta {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@Singleton
class SingletonEJBJakarta {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@MessageDriven
class MessageDrivenEJBJakarta {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

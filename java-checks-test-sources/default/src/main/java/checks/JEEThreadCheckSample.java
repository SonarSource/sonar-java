package checks;

import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

class NonJEEClass {
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

class JEEServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Runnable r = new Runnable() { // Noncompliant {{Remove this use of "Runnable".}}
      //             ^^^^^^^^
      public void run() {}
    };
    new Thread(r).start();

    Runnable lambda = () -> {}; // Noncompliant

    synchronized (this) { // Noncompliant {{Remove this use of the "synchronized" keyword.}}
  //^^^^^^^^^^^^
    }
  }

  public synchronized void syncMethod() { // Noncompliant
  }
}

@Stateless
class StatelessEJB {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@Stateful
class StatefulEJB {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@Singleton
class SingletonEJB {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@MessageDriven
class MessageDrivenEJB {
  public void method() {
    Runnable r = new Runnable() { // Noncompliant
      public void run() {}
    };
    synchronized (this) { // Noncompliant
    }
  }
}

@Stateless
class StatelessRunnable implements Runnable { // Noncompliant
  public void run() {}
}

@Stateless
class StatelessWithInner {
  class InnerHelper {
    void method() {
      Runnable r = new Runnable() { // Noncompliant
        public void run() {}
      };
      synchronized (this) { // Noncompliant
      }
    }
  }
}

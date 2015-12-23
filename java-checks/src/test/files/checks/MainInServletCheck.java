import javax.ejb.SessionBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServlet;
import org.apache.struts.action.Action;

public class MyServlet extends HttpServlet {
  MyServlet() {
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
  }

  public static void main(String[] args) { // Noncompliant [[sc=22;ec=26]] {{Remove this unwanted "main" method.}}
  }
}

@javax.ejb.Stateless
public class MyBean {
  public static void main(String[] args) { // Noncompliant
  }
}

public class HelloBean implements SessionBean {
  public static void main(String[] args) { // Noncompliant
  }
}

@Deprecated
public class Main {
  public static void main(String [] args) {
  }
}

class ActionNoncompliant extends Action {
  public static void main(String [] args) { // Noncompliant
  }
}

class ActionOk extends Action { }

import javax.servlet.http.HttpServlet;
import org.apache.struts.action.Action;

class A {
  private String userName;
}

class B extends HttpServlet {
  
  private String userName;
  private static String staticVar;
  private final String finalVar;
  
  public B(String x) {
    String localVar;
    finalVar = x;
  }
}

class C extends Action {
  
  private String userName;
  private static String staticVar; 
  private final String finalVar;
  
  public C(String x) {
    finalVar = x;
  }
}

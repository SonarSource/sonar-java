package checks;

import java.util.function.Function;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.apache.struts.action.Action;

class S2226FP extends HttpServlet {
  private int first; // Compliant
  private long second; // Compliant

  @Override
  public void init() throws ServletException {
    this.first = 42;
    second = this.first * 2;
  }
}

class S2226FP2 extends HttpServlet {
  private int first; // Noncompliant {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}
  private final S2226FP2 inst = null;

  @Override
  public void init() throws ServletException {
    inst.first = 42;
  }
}

abstract class S2226FP3 extends HttpServlet {
  private int first; // Noncompliant {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}

  @Override
  public void init() throws ServletException {
    getInstance().first = 42;
  }

  abstract S2226FP3 getInstance();
}

@interface Inject{}

class HttpServletA {
  private String userName;
}

class HttpServletB extends HttpServlet {
  private String userName; // Noncompliant {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}
//               ^^^^^^^^
  private static String staticVar;
  private final String finalVar;
  private String storageType;
  private static final Function<Integer, Integer> LAMBDA = lambdaParam -> {
    Integer lambdaVar = null;
    return lambdaVar;
  };

  public HttpServletB(String x) {
    String localVar;
    finalVar = x;
  }

  public void init(javax.servlet.ServletConfig config) {
    storageType = StorageType.valueOf(config.getInitParameter("storageType"));
  }

  private static class StorageType {
    public static String valueOf(String storageType) {
      return null;
    }
  }
}

class HttpServletC extends Action {

  private String userName; // Noncompliant
  private static String staticVar; 
  private final String finalVar;

  public HttpServletC(String x) {
    finalVar = x;
  }
}

class HttpServletD extends HttpServlet {

  @javax.inject.Inject private String userName; // compliant annotated with inject;
  @Inject private String userName1; // Noncompliant
  @Resource private String city; // compliant annotated with resource;
  private static String staticVar;
}

public class ServletInstanceFieldCheckSample extends HttpServlet {
  @org.springframework.beans.factory.annotation.Autowired
  private javax.sql.DataSource myDB; // Noncompliant
}

class HttpServletE extends HttpServlet {
  private String userName; // Noncompliant {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}
//               ^^^^^^^^
  private final String finalVar;
  private String storageType; // Compliant, initialized in init() method

  public HttpServletE(String x) {
    String localVar;
    finalVar = x;
  }

  public void init() {
    storageType = StorageType.valueOf(getServletConfig().getInitParameter("storageType"));
  }

  private static class StorageType {
    public static String valueOf(String storageType) {
      return null;
    }
  }
}


package checks;

import java.util.function.Function;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServlet;
import org.apache.struts.action.Action;

class HttpServletAJakarta {
  private String userName;
}

class HttpServletBJakarta extends HttpServlet {
  private String userName; // Noncompliant [[sc=18;ec=26]] {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}
  private static String staticVar;
  private final String finalVar;
  private String storageType;
  private static final Function<Integer, Integer> LAMBDA = lambdaParam -> {
    Integer lambdaVar = null;
    return lambdaVar;
  };

  public HttpServletBJakarta(String x) {
    String localVar;
    finalVar = x;
  }

  public void init(jakarta.servlet.ServletConfig config) {
    storageType = StorageType.valueOf(config.getInitParameter("storageType"));
  }

  private static class StorageType {
    public static String valueOf(String storageType) {
      return null;
    }
  }
}

class HttpServletCJakarta extends Action {

  private String userName; // Noncompliant
  private static String staticVar;
  private final String finalVar;

  public HttpServletCJakarta(String x) {
    finalVar = x;
  }
}

class HttpServletDJakarta extends HttpServlet {

  @jakarta.inject.Inject private String userName; // compliant annotated with inject;
  @Inject private String userName1; // Noncompliant
  @Resource private String city; // compliant annotated with resource;
  private static String staticVar;
}

public class ServletInstanceFieldCheckJakarta extends HttpServlet {
  @org.springframework.beans.factory.annotation.Autowired
  private javax.sql.DataSource myDB; // Noncompliant - filtered by the SpringFilter
}

class HttpServletEJakarta extends HttpServlet {
  private String userName; // Noncompliant [[sc=18;ec=26]] {{Remove this misleading mutable servlet instance field or make it "static" and/or "final"}}
  private final String finalVar;
  private String storageType; // Compliant, initialized in init() method

  public HttpServletEJakarta(String x) {
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

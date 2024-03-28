package checks;

import java.util.function.Function;
import javax.servlet.http.HttpServlet;
import org.apache.struts.action.Action;
import javax.annotation.Resource;

class MyServlet extends HttpServlet {
  @javax.ejb.EJB
  private MyObject myObject;
}

public class ServletInstanceFieldCheckSample extends HttpServlet {
  @javax.annotation.Resource
  private javax.sql.DataSource myDB; // compliant annotated with Resource
}

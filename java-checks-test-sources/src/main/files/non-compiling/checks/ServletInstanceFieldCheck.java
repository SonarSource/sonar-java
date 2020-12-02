package checks;

import java.util.function.Function;
import javax.servlet.http.HttpServlet;
import org.apache.struts.action.Action;
import javax.annotation.Resource;

class MyServlet extends HttpServlet {
  @javax.ejb.EJB
  private MyObject myObject;
}

public class ServletInstanceFieldCheck extends HttpServlet {
  @org.springframework.beans.factory.annotation.Autowired
  private javax.sql.DataSource myDB; // compliant annotated with autowired
}

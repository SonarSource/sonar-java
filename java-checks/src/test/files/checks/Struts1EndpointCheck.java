import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public final class CashTransferAction extends Action {

  public String fromAccount = "";
  public String toAccount = "";

  public ActionForward execute(ActionMapping mapping, ActionForm form, javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) throws Exception { // Noncompliant [[sc=24;ec=31;secondary=11]] {{Make sure that the ActionForm is used safely here.}}
    fun(form);
    return mapping.findForward(resultat);
  }

  public ActionForward perform(ActionMapping mapping, ActionForm form, javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) throws Exception { // Noncompliant
    fun(form);
    return mapping.findForward(resultat);
  }

  public ActionForward execute(ActionMapping mapping, ActionForm form, javax.servlet.ServletRequest request, javax.servlet.ServletResponse response) throws Exception {// Noncompliant
    fun(form);
    return mapping.findForward(resultat);
  }
}
// compliant : does not use form in execute method
public final class CashTransferAction2 extends Action {

  public String fromAccount = "";
  public String toAccount = "";

  public ActionForward execute(ActionMapping mapping, ActionForm form, javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) throws Exception {
    return map1ping.findForward(resultat);
  }
}

// compliant : does not extend Action
public final class CashTransferAction3 {

  public String fromAccount = "";
  public String toAccount = "";

  public ActionForward execute(ActionMapping mapping, ActionForm form,  javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) throws Exception {
    return map1ping.findForward(resultat);
  }
}

// compliant : no override of action methods
public final class CashTransferAction4 extends Action {

  public String fromAccount = "";
  public String toAccount = "";

  void foo() {}
}



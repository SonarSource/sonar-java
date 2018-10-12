import com.opensymphony.xwork2.ActionSupport;

public class AccountBalanceAction extends ActionSupport {
  private static final long serialVersionUID = 1L;
  private Integer accountId;

  @Override
  public String execute() throws Exception { // compliant, no setter
    return SUCCESS;
  }

  private void someMethod(){}
}

public class AccountBalanceAction2 extends ActionSupport {
  private static final long serialVersionUID = 1L;
  private Integer accountId;

  @Override
  public String execute() throws Exception { // Noncompliant [[sc=17;ec=24;secondary=24]] {{Make sure that executing this ActionSupport is safe.}}
    return SUCCESS;
  }

  public void setAccountId(Integer accountId) {
    this.accountId = accountId;
  }
}
public class AccountBalanceAction3 extends ActionSupport {
  private static final long serialVersionUID = 1L;
  private Integer accountId;

  @Override
  public String execute() throws Exception {
    return SUCCESS;
  }

  public void setAccountId(Integer accountId, Object foo) {
  }
}

public class AccountBalanceAction4 extends ActionSupport {
  private static final long serialVersionUID = 1L;
  private Integer accountId;

  public void setAccountId(Integer accountId, Object foo) {
  }
}

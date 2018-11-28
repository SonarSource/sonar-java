import javax.naming.directory.SearchControls;
import java.util.Date;
import java.util.Properties;

class S4434 {

  void callConstructor(int scope, long countLimit, int timeLimit, String[] attributes, boolean returnObject, boolean deref) {
    SearchControls ctrl1 = new SearchControls(scope, countLimit, timeLimit, attributes, true, deref); // Noncompliant {{Disable object deserialization.}}
    SearchControls ctrl2 = new SearchControls(scope, countLimit, timeLimit, attributes, false, deref);
    SearchControls ctrl3 = new SearchControls(scope, countLimit, timeLimit, attributes, returnObject, deref); // Should be noncompliant if we know returnObject is true
  }

  void callSetter(boolean returnObject) {
    SearchControls ctrl = new SearchControls();
    ctrl.setReturningObjFlag(true); // Noncompliant
    ctrl.setReturningObjFlag(false);
    ctrl.setReturningObjFlag(returnObject); // Should be noncompliant if we know returnObject is true
  }
}

class NamedSearchControls extends SearchControls {
  public NamedSearchControls() {
    super();
  }

  public NamedSearchControls(boolean b) {
    super(0, 0, 0, new String[0], true, b); // Noncompliant
  }
}

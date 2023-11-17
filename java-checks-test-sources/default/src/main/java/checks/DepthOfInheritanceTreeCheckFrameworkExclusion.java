package checks;

import javax.swing.*;
import org.eclipse.equinox.log.LogPermission;
import org.springframework.dao.DataAccessException;
import javafx.scene.layout.BorderPane;

public class DepthOfInheritanceTreeCheckFrameworkExclusion {
  class MyJFrame extends JFrame {
  }

  class MyLogPermission extends LogPermission {
    public MyLogPermission(String name, String actions) {
      super(name, actions);
    }
  }

  public class MyBorderPane extends BorderPane { // Compliant, package javafx.scene.** is excluded
    // empty
  }

  class MyDataAccessException extends DataAccessException {
    public MyDataAccessException(String msg) {
      super(msg);
    }
  }

  class OneMoreLevelException extends MyDataAccessException {
    public OneMoreLevelException(String msg) {
      super(msg);
    }
  }

  class OneLevelTooFarException extends OneMoreLevelException { // Noncompliant {{This class has 2 parents which is greater than 1 authorized.}}
    public OneLevelTooFarException(String msg) {
      super(msg);
    }
  }
}

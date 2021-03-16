package checks;

import javax.swing.*;
import org.eclipse.equinox.log.LogPermission;
import org.springframework.dao.DataAccessException;

public class DitKnownFrameworks {
  class MyJFrame extends JFrame {
  }

  class MyLogPermission extends LogPermission {
    public MyLogPermission(String name, String actions) {
      super(name, actions);
    }
  }

  class MyDataAccessException extends DataAccessException {
    public MyDataAccessException(String msg) {
      super(msg);
    }
  }

}

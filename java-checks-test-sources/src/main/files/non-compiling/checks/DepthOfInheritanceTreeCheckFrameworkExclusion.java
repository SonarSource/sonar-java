package checks;

import android.view.View;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.persistit.ui.AdminPanel;

public class DitFrameworkExclusion {
  class MyView extends android.view.View {
  }

  class MyAdminPanel extends com.persistit.ui.AdminPanel {
  }

  class MyDataAccessException extends com.intellij.openapi.ui.SimpleToolWindowPanel {
  }
}

package foo;

import com.sun.awt.AWTUtilities;
import javax.swing.SortOrder;
import sun.awt.CausedFocusEvent;

public class Main {

  public int test() {
    SortOrder order = SortOrder.ASCENDING;
    if (order.equals(SortOrder.DESCENDING)) {
      return 1;
    }

    CausedFocusEvent.Cause cause = CausedFocusEvent.Cause.ACTIVATION; // Encapsulated (= not visible) since JDK 9
    if (cause.equals(CausedFocusEvent.Cause.AUTOMATIC_TRAVERSE)) {
      return 2;
    }

    AWTUtilities.Translucency trans = AWTUtilities.Translucency.PERPIXEL_TRANSLUCENT; // Removed in JDK 11
    if (trans.equals(AWTUtilities.Translucency.PERPIXEL_TRANSPARENT)) {
      return 3;
    }

    return 4;
  }

}

package checks;

import java.io.Serializable;

class TransientFieldInNonSerializableCheck {

  class C extends Unknown {
    transient String x;
    String y;
  }

}

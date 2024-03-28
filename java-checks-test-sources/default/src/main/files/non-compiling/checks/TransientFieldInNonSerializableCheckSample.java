package checks;

import java.io.Serializable;

class TransientFieldInNonSerializableCheckSample {

  class C extends Unknown {
    transient String x;
    String y;
  }

}

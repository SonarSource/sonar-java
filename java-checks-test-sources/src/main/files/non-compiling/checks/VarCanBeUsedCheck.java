package checks;

import java.util.ArrayList;
import java.util.List;

public class VarCanBeUsedCheck {
  
  void f(int[] array) {
    A a = new A();
    Object o = unknown(); // Compliant, unknown method call
  }
  
  
}

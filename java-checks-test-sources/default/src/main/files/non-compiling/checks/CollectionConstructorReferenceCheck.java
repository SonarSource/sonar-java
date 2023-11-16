package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CollectionConstructorReferenceCheck {
  void foo() {
    Function<UnknownType, List<String>> list1 = ArrayList::new;  // Compliant, undetermined ArrayList constructor
  }
}

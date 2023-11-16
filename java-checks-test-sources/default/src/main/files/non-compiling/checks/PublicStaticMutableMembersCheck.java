package checks;

import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublicStaticMutableMembersCheck {
  public static final List UNKNOWN_LIST = unknownMethod("a"); // Compliant
  public static final List noInitializer;
  // we don't know the type of foo
  public static final List unknown = foo();

}

interface I {
  public static MyImmutableCollection<String> immutableList2; //Compliant : immutable collection
}

class MyImmutableCollection<E> extends ImmutableCollection<E> { }

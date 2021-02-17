package checks;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

class StaticMemberAccessParent {
  public static int counter;
}

class StaticMemberAccessChild extends StaticMemberAccessParent {
  public StaticMemberAccessChild() {
    StaticMemberAccessChild.counter++;  // Noncompliant {{Use static access with "checks.StaticMemberAccessParent" for "counter".}}
    StaticMemberAccessParent.counter++; // Compliant

    StaticMemberAccessChild.unknown++;  // Compliant
    StaticMemberAccessParent.unknown++; // Compliant

    StaticMemberAccessChild.unknown(); // Compliant
    StaticMemberAccessParent.unknown(); // Compliant
  }
}

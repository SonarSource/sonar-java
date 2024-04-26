package checks;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

class StaticMemberAccessParent {
  public static int counter;

  static void foo() {
    StaticMemberAccessParent.counter++;
    counter++;
  }
}

class StaticMemberAccessChild extends StaticMemberAccessParent {
  public StaticMemberAccessChild() {
    StaticMemberAccessChild.counter++; // Noncompliant {{Use static access with "checks.StaticMemberAccessParent" for "counter".}}
    StaticMemberAccessParent.counter++; // Compliant

    StaticMemberAccessChild.foo(); // Noncompliant
    StaticMemberAccessParent.foo(); // Compliant
  }
}

class Generic<X> {
  interface E<Y> { }

  static <T> T m() {
    return null;
  }

  void test() {
    Object b = Generic.m(); // Compliant
  }
}

class GuavaFP {
  // method is incorrectly resolved as Set.of, specifically excluded in implementation to avoid
  // see SONARJAVA-3095
  protected static final Set<String> STRING_SET = ImmutableSet.of(
    "javax.management.remote.timeout",
    "javax.management.remote.misc",
    "javax.management.remote.rmi",
    "javax.management.mbeanserver",
    "sun.rmi.loader",
    "sun.rmi.transport.tcp",
    "sun.rmi.transport.misc",
    "sun.rmi.server.call",
    "sun.rmi.dgc");
  
}

package checks.S3252_StaticMemberAccessCheckSample;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import checks.S3252_StaticMemberAccessCheckSample.hide_non_public.StaticMemberAccessCheckSampleHelper.B;
import checks.S3252_StaticMemberAccessCheckSample.hide_non_public.StaticMemberAccessCheckSampleHelper.Bar;

class StaticMemberPackageHidden {
  public void foo(){
    int x = B.CONSTANT; // Compliant A is not accessible so we should not raise an issue
    int y = Bar.CONSTANT; // Noncompliant
  }
}

class StaticMemberAccessParent {
  public static int counter;

  static void foo() {
    StaticMemberAccessParent.counter++;
    counter++;
  }
}

class StaticMemberAccessChild extends StaticMemberAccessParent {
  public StaticMemberAccessChild() {
    StaticMemberAccessChild.counter++; // Noncompliant {{Use static access with "checks.S3252_StaticMemberAccessCheckSample.StaticMemberAccessParent" for "counter".}}
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

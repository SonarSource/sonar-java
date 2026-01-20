package checks;

import java.util.List;
import java.util.ArrayList;

public class WildCardReturnParameterNestedTypeCheck {
  void bar() {
    foo(listOfLists());
  }

  void foo(List<List<? extends A>> listList) {

  }

  List<List<? extends A>> listOfLists() {
    return new ArrayList<>();
  }

  private static class A {
  }

  private static class B extends checks.A {
  }
}



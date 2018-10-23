import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

class ConstructorInMethodRef {

  class Type {
    Type erasure() {
      return null;
    }
  }

  private static List<Set<Type>> erased(List<Set<Type>> typeSets) {
    return typeSets.stream().map(set -> set.stream().map(Type::erasure)
      .collect(Collectors.toCollection(LinkedHashSet::new))).collect(Collectors.toList());
  }

  private static List<Set<Type>> erased2(List<Set<Type>> typeSets) {
    return typeSets.stream().map(set -> set.stream().map(Type::erasure)
      .collect(Collectors.toCollection(() -> new LinkedHashSet<>()))).collect(Collectors.toList());
  }
  public class Bar {
    public void print() {
      java.util.List<String> list = java.util.Arrays.asList("x", "y", "z");
      java.util.List<Foo> foos = list.stream().map(Foo::new).collect(Collectors.toList());
      System.out.println(foos.get(0).foo);
    }

    public class Foo {
      private String foo;
      private Foo(String foo) {
        this.foo = foo;
      }
    }
  }
}

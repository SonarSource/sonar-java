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

}

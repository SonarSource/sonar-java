class VarInitializer {
  java.util.Set<String> mySet = ImmutableSet.of();

  abstract class ImmutableSet<E> implements java.util.Set<E> {
    public static <T> ImmutableSet<T> of() {
      return null;
    }
  }
}



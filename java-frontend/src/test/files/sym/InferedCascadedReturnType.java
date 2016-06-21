class InferedCascadedReturnType<U> {

  java.util.List<U> foo() {
    return sortKeysByValue(Ordering.natural().reverse());
  }

  private static <K, V> java.util.List<K> sortKeysByValue(java.util.Comparator<? super V> valueComparator) {}

  static abstract class Ordering<T> implements java.util.Comparator<T>{
    public static <C extends Comparable> Ordering<C> natural(){}
    public <S extends T> Ordering<S> reverse(){}
  }
}
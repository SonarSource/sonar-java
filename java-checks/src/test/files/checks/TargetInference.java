import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

interface I {}
class A implements I {}

class Test {

  HashSet<A> hashSetA;
  HashSet<I> hashSetI;

  void methodCall(A a) {
    HashSet<A> newHashSet = newHashSet(a, a);

    useSet(newHashSet); // does not compile, however we have semantic with ECJ, type of arg is HashSet<A>
    useSet(newHashSet(a, a)); // compile, type inference has been influenced by target type, type of arg is HashSet<I>
  }

  <E> java.util.HashSet<E> newHashSet(E... elements) {
    return null;
  }

  void useSet(java.util.Set<I> elements) {}
}

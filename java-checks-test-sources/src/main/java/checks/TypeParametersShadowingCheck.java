package checks;

import java.util.List;
import java.util.function.Function;

public class TypeParametersShadowingCheck<T0> {
  class TypeParameterHidesAnotherType<T> {
    public class Inner<T> { // Noncompliant [[sc=24;ec=25;secondary=-1]] {{Rename "T" which hides a type parameter from the outer scope.}}
      //...
    }
    public class Inner2<T0> { // Noncompliant [[secondary=-5]] {{Rename "T0" which hides a type parameter from the outer scope.}}
      //...
    }
    private <T> T method() { // Noncompliant [[sc=14;ec=15;secondary=-7]] {{Rename "T" which hides a type parameter from the outer scope.}}
      return null;
    }
  }

  class NoTypeParameterHiding<T> {
    public class InnerS1<S> { // Compliant
      List<S> listOfS;
    }
    public class InnerS2<S> { // Compliant
      List<S> listOfS;
    }
    private <V> V method() { // Compliant
      return null;
    }
  }

  class MultipleHiding<T, Q> {
    public class Inner<T> { // Noncompliant [[sc=24;ec=25;secondary=-1]] {{Rename "T" which hides a type parameter from the outer scope.}}
      private <T> T method2() { // Noncompliant [[secondary=-2]] {{Rename "T" which hides a type parameter from the outer scope.}}
        return null;
      }
      private <Q> T method() { // Noncompliant [[sc=16;ec=17;secondary=-5]] {{Rename "Q" which hides a type parameter from the outer scope.}}
        return null;
      }
    }
    private <T> T method2() { // Noncompliant [[secondary=-9]]
      return null;
    }
  }

  class TypeExtend<E extends Comparable<E>, Q> {
    public class Inner<E extends Q> { // Noncompliant [[sc=24;ec=25;secondary=-1]] {{Rename "E" which hides a type parameter from the outer scope.}}
    }
    public class Inner2<P extends E> { // Compliant
    }
  }

  class DeeplyNestedIntoAnonymousClass<T> {
    public class Inner<S> { // Compliant
      private <Q> T method() { // Compliant
        new Function<Integer, Integer>() {
          class InnerAnonymousHidingT<T> { // Noncompliant [[secondary=-4]]
            //...
          }
          class InnerAnonymousHidingS<S> { // Noncompliant [[secondary=-6]]
            //...
          }
          class InnerAnonymousHidingQ<Q> { // Noncompliant [[secondary=-8]]
            //...
          }
          @Override
          public Integer apply(Integer o) {
            return null;
          }
        };
        return null;
      }
    }
  }

  // Static members are not subject to scoping problem, since they anyway don't have access to the outer scope
  private static <T0> T0 methodBefore() { // Compliant, static member, T0 is not hiding anything
    return null;
  }
  public static class Inner<T0, P, Q> { // Compliant, static member, T0 is not hiding anything
    private <Q> Q method2() { // Noncompliant [[secondary=-1]]
      return null;
    }
    // Actually hiding the one from the static class
    private <T0> Q method3() { // Noncompliant [[secondary=-5]]
      return null;
    }
    private static <P> P method1() { // Compliant, static member, not hiding anything
      new Function<Integer, Integer>() {
        class InnerAnonymousHidingT<P> { // Noncompliant [[secondary=-2]]
          //...
        }
        @Override
        public Integer apply(Integer o) {
          return null;
        }
      };
      return null;
    }
  }
  private static <T0> T0 methodAfter() { // Compliant, static member
    return null;
  }

  public static class Inner2 {
    // Compliant, not hiding anything
    private <T0> T0 method() {
      return null;
    }
  }

}

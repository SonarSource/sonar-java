package checks;

import java.util.List;
import java.util.function.Function;

public class TypeParametersShadowingCheck {
  class TypeParameterHidesAnotherType<T> {
    public class Inner<T> { // Noncompliant [[sc=24;ec=25;secondary=-1]] {{Rename "T" which hides a type parameter from the outer scope.}}
      //...
    }
    private <T> T method() { // Noncompliant [[sc=14;ec=15;secondary=-4]] {{Rename "T" which hides a type parameter from the outer scope.}}
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
}

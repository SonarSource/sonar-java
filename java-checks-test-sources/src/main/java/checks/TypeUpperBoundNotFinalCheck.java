package checks;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class TypeUpperBoundNotFinalCheck {
  public static <T extends FinalClass> T getMyString() { // Noncompliant [[sc=18;ec=38]] {{The upper bound of a type variable should not be final.}}
    return (T) new FinalClass<>();
  }

  public static class NonExtendableTypeParam<T extends FinalClass> { } // Noncompliant

  public static void boundedWildcard(Collection<? extends FinalClass> c) { } // Noncompliant

  public static class FinalAndNonFinalBounds<T extends FinalClass & Comparable> { } // Noncompliant [[sc=46;ec=79]]

  @Nullable
  public static <T extends FinalClass & Comparable & Serializable> T multipleBounds() { // Noncompliant
    return null;
  }

  public static <T extends FinalClass<T>> void finalParameterizedBound() { } // Noncompliant

  public static <T extends NonFinalClass<? extends FinalClass>> void finalInnerBound() { } // Noncompliant [[sc=42;ec=62]]

  public static final class ImmutableClass<B> {
    public <T extends B> NonFinalClass<B> extendsClass(Map<? extends Class<? extends T>, ? extends T> map) { return null; } // Noncompliant [[sc=60;ec=88]]
  }


  public static class Extendable<T extends Object> { } // Compliant

  public static class MultipleBounds<T extends Object & Comparable> { }

  public static void extendableWildcard(Collection<? extends Object> c) { }

  public static void superTypes(List<Comparable<? super T>> list) { }

  public static <T extends Comparable<T>> void complexBound() { }

  public static <T extends Comparable<? super T>> void extendsAndSuper(List<T> list) { }

  public static void unboundedWildcard(Collection<?> c) { }

  public static <T extends TypeUpperBoundNotFinalCheck.NonFinalClass<T>> void memberSelect() { }

  static class NonFinalClass<T> { }
  final static class FinalClass<T> { }
}

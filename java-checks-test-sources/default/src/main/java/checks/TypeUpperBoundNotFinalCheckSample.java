package checks;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class TypeUpperBoundNotFinalCheckSample {
  public static class NonExtendableTypeParam<T extends FinalClass> { } // Noncompliant {{Replace this type parametrization by the 'final' type `FinalClass`.}}

  public static <T extends FinalClass> void methodTypeParameter() { } // Noncompliant

  public static void nonExtendableWildcard(Collection<? extends FinalClass> c){ } // Noncompliant

  public static <T extends NonFinalClass<? extends FinalClass>> void complexTypeParameter() { } // Noncompliant

  public static class Variables {
    private TwoParams<? extends FinalClass, String> complexVarParams = null; // Noncompliant
    private FinalClass<? extends NonFinalClass> finalVar = null; // Compliant

    public static void methodTypeParameter() {
      Collection<? extends FinalClass> variableDeclInMethod = null; // Noncompliant
      TwoParams<String, ? extends FinalClass> complexVarParams = null; // Noncompliant
    }
  }

  static abstract class AbstractClass {
    public abstract <T extends FinalClass> Set<T> overriddenWithTypeParam(); // Noncompliant
    public abstract Set<String> overriddenWithArgument(Collection<? extends FinalClass> v); // Noncompliant
  }

  public static void boundedWildcard(Collection<? extends FinalClass> c) { } // Noncompliant

  public static class FinalAndNonFinalBounds<T extends FinalClass & Comparable> { } // Noncompliant [[sc=46;ec=79]]

  @Nullable
  public static <T extends FinalClass & Comparable & Serializable> T multipleBounds() { // Noncompliant
    return null;
  }

  public static <T extends FinalClass<T>> void finalParameterizedBound() { } // Noncompliant [[sc=18;ec=41]]

  public static <T extends NonFinalClass<? extends FinalClass>> void finalInnerBound() { } // Noncompliant [[sc=42;ec=62]]

  public static <T extends TwoParams<? extends NonFinalClass, ? extends FinalClass>> void complexFinalInnerBound() { } // Noncompliant [[sc=63;ec=83]]

  public static <T extends NonFinalClass, B extends FinalClass> void multipleTypeParams() { } // Noncompliant [[sc=43;ec=63]]

  public static NonFinalClass<? extends FinalClass<T>> methodReturn() { return null; } // Noncompliant [[sc=31;ec=54]]

  public static final class ImmutableClass<B> {
    public <T extends B> NonFinalClass<B> extendsClass(Map<? extends Class<? extends T>, ? extends T> map) { return null; } // Noncompliant [[sc=60;ec=88]]
  }



  public static class Extendable<T extends Object> { // Compliant
    private FinalClass fc = null;
    private Extendable() { fc = new FinalClass(); }
  }

  public static class OverridingClass extends AbstractClass {
    @Override
    public <T extends FinalClass> Set<T> overriddenWithTypeParam() {return new HashSet<>();} // Compliant because overridden methods signature might not be modifiable by user
    @Override
    public Set<String> overriddenWithArgument(Collection<? extends FinalClass> v) { // Compliant
      TwoParams<? extends FinalClass, String> complexVarParams = null; // Noncompliant
      return new HashSet<>();
    };
  }

  public static class MultipleBounds<T extends Object & Comparable> { }

  public static void extendableWildcard(Collection<? extends Object> c) { }

  public static void superTypes(List<Comparable<? super T>> list) { }

  public static <T extends Comparable<T>> void complexBound() { }

  public static <T extends Comparable<? super T>> void extendsAndSuper(List<T> list) { }

  public static void unboundedWildcard(Collection<?> c) { }

  public static FinalClass returnFinal() { return null; }

  public static FinalClass<? extends NonFinalClass<T>> finalMethodReturn() { return null; }

  public static NonFinalClass<? extends FinalClass[]> arrayBound() { return null; }



  public static class TwoParams<T, B> { }
  static class NonFinalClass<T> { }
  final static class FinalClass<T> { }
}

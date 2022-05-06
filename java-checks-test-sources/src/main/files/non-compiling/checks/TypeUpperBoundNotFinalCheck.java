package checks;


public class TypeUpperBoundNotFinalCheck {
  public static class FinalBound<T extends FinalClass> { } // Noncompliant

  public static class UnknownBound<T extends Unknown> { } // Compliant

  public static class OverridingClass extends Unknown {
    @Override
    public Set<String> overriddenWithArgument(Collection<? extends FinalClass> v) { // Compliant because of @Override
      return new HashSet<>();
    };
    public <T extends FinalClass> Set<T> overriddenWithTypeParam() {return new HashSet<>();} // Compliant because method might be overridding
  }

  final static class FinalClass<T> { }
}

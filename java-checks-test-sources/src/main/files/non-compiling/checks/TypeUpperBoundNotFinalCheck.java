package checks;


public class TypeUpperBoundNotFinalCheck {
  public static class FinalBound<T extends FinalClass> { } // Noncompliant

  public static class UnknownBound<T extends Unknown> { } // Compliant

  final static class FinalClass<T> { }
}

package checks;

class BooleanMethodReturnCheckA {
  @UnknownAnnotation
  public Boolean foo() {
    return null; // Compliant, UnknownAnnotation could be Nullable
  }
}

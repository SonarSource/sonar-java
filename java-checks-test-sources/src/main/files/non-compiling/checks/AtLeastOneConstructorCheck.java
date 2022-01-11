class AtLeastOneConstructorCheck  {
  @EJB
  private MyObject foo; // Compliant, unknown annotation, could be from javax.ejb.
  @Resource
  private MyObject foo2; // Compliant, unknown annotation, could be from javax.ejb.
}

@Unknown
class AtLeastOneConstructorCheckAnnotatedUnknown  {
  private MyObject foo; // Compliant
}

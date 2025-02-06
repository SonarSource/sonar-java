package checks.spring;

import org.springframework.test.annotation.DirtiesContext;

public class DirtyContextShouldUseCorrectControlModeCheckSample {

  @interface MyAnnotation{}

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD, hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE) // Noncompliant {{Replace methodMode with classMode.}}
//                ^^^^^^^^^^
  public static class WrongContextMode {
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS, hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE) // Noncompliant {{Replace classMode with methodMode.}}
  //                ^^^^^^^^^
    public void wrongMode() {}

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void correctMode() {}


    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // Noncompliant
    @MyAnnotation
    public void additionalAnnotation(){}

    @DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
    public void hierarchyMode(){}

  }

  @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
  public static class CorrectContextMode {
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // Noncompliant
    public void wrongMode() {}

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void correctMode() {}


    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // Noncompliant
    @MyAnnotation
    public void additionalAnnotation(){}
  }

  @DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
  public static class HierarchyMode {}

  @MyAnnotation
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Noncompliant
  public class AdditionalAnnotation {}


  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  @interface MyDirtyContext {}

  @MyDirtyContext  // we don't raise an issue, as I don't know if it is valid
  // and the computing time would be too costly compared to the benefit
  public static class MyWrongDirtyContext {}

  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD) // Noncompliant
  record WrongRecordContext() {
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // Noncompliant
    public void wrongMode() {}

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void correctMode() {}
  }

}

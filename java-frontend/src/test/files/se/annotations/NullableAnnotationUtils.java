/**
 * Classes are placed in "android.support.annotation" in order
 * to simulate android support package (not available through maven central)
 */
package android.support.annotation;

@MyAnnotation(Location.ALL)
interface A {
  @MyAnnotation @org.bar.MyOtherAnnotation Object foo();
  Object bar();

  @javax.annotation.Nullable Object nullable1();
  @javax.annotation.CheckForNull Object nullable2();
  @org.jetbrains.annotations.Nullable Object nullable3();
  @edu.umd.cs.findbugs.annotations.Nullable Object nullable4();
  @org.eclipse.jdt.annotation.Nullable Object nullable5();

  @javax.annotation.Nonnull Object nonnull1();
  @javax.validation.constraints.NotNull Object nonnull2();
  @org.jetbrains.annotations.NotNull Object nonnull3();
  @edu.umd.cs.findbugs.annotations.NonNull Object nonnull4();
  @lombok.NonNull Object nonnull5();
  @NonNull Object nonnull6(); // android annotation
  @org.eclipse.jdt.annotation.NonNull Object nonnull7();
}

@interface MyAnnotation {
  public Location[] value() default Location.ALL;
}


enum Location {
  ALL, PARAMETER, RETURN_TYPE;
}

// fake 'android.support.annotation.NonNull' annotation
@interface NonNull { }

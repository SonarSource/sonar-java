/**
 * Classes are placed in "android.support.annotation" in order
 * to simulate android support package (not available through maven central)
 */
package android.support.annotation;

interface A {
  @MyAnnotation @org.bar.MyOtherAnnotation Object foo();
  Object bar();

  @javax.annotation.Nullable Object nullable1();
  @javax.annotation.CheckForNull Object nullable2();
  @org.jetbrains.annotations.Nullable Object nullable3();
  @edu.umd.cs.findbugs.annotations.Nullable Object nullable4();
  @org.eclipse.jdt.annotation.Nullable Object nullable5();
  @javax.annotation.Nonnull(when = javax.annotation.meta.When.UNKNOWN) Object nullable6();
  @javax.annotation.Nonnull(when = javax.annotation.meta.When.MAYBE) Object nullable7();
  @MyNullableAnnotation Object nullable8();
  @MyOtherNullableAnnotation Object nullable9();
  @MayBeNullAnnotation Object nullable10();
  @Nullable Object nullable11(); // android annotation
  @android.support.annotation.Nullable Object nullable12(); // android annotation

  @javax.annotation.Nonnull Object nonnull1();
  @javax.validation.constraints.NotNull Object nonnull2();
  @org.jetbrains.annotations.NotNull Object nonnull3();
  @edu.umd.cs.findbugs.annotations.NonNull Object nonnull4();
  @lombok.NonNull Object nonnull5();
  @NonNull Object nonnull6(); // android annotation
  @org.eclipse.jdt.annotation.NonNull Object nonnull7();
  @javax.annotation.Nonnull(when = javax.annotation.meta.When.ALWAYS) Object nonnull8();
  @MyNonNullAnnotation Object nonnull9();
}

// fake 'android.support.annotation.NonNull' annotation
@interface NonNull { }

// fake 'android.support.annotation.Nullable' annotation
@interface Nullable { }

@javax.annotation.Nullable
@interface MyNullableAnnotation { }

@javax.annotation.Nonnull(when = javax.annotation.meta.When.MAYBE)
@interface MyOtherNullableAnnotation { }

@MyNullableAnnotation
@interface MayBeNullAnnotation { }

@javax.annotation.Nonnull
@interface MyNonNullAnnotation { }

@interface MyAnnotation { }

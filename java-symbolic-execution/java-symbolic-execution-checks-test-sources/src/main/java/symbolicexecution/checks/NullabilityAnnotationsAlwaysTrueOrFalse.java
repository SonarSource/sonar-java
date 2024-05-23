package symbolicexecution.checks;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class NullabilityAnnotationsAlwaysTrueOrFalse {
  boolean field;

  public void nullableAttributes() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null) { // Compliant, getRequestAttributes is annotated with @org.springframework.lang.Nullable
    }
  }

  public void alwaysTrue() {
    Object attributes = springNonNull();
    if (attributes != null) { // Noncompliant
    }
  }

  public void alwaysTrue2() {
    AnnotatedNonNullByDefault c = new AnnotatedNonNullByDefault();
    Object attributes = c.nonNullViaClassAnnotation();
    if (attributes != null) { // Noncompliant
    }
  }

  public void alwaysTrue3() {
    AnnotatedNonNullByDefault c = new AnnotatedNonNullByDefault();
    Object attributes = c.nonNullViaClassAnnotation();
    Object attributes2 = attributes;
    if (attributes2 != null) { // Noncompliant
    }
  }

  public void notAlwaysTrue() {
    AnnotatedNonNullByDefault c = new AnnotatedNonNullByDefault();
    Object attributes = c.nullableViaDirectAnnotation();
    if (attributes != null) { // Compliant
    }
  }

  @org.springframework.lang.NonNull
  public Object springNonNull() {
    return new Object();
  }

  @org.eclipse.jdt.annotation.NonNullByDefault
  class AnnotatedNonNullByDefault {
    public Object nonNullViaClassAnnotation() {
      return new Object();
    }

    @Nullable
    public Object nullableViaDirectAnnotation() {
      return new Object();
    }

  }

  class ClassWithFinalNonNullField {
    final Object o = nonNullCall();

    @Nonnull
    public Object nonNullCall() {
      return new Object();
    }

    public void alwaysTrue4() {
      if (o != null) { // Noncompliant
      }
    }
  }

  void localVariable() {
    @Nonnull
    Object o = getSomething();
    if (o != null) {
    }
  }
  void localVariable2(List<String>items) {
    for (@Nonnull Object o : items) {
      if (o != null) { // Noncompliant
      }
    }
  }

  private Object getSomething() {
    if (field) {
      return null;
    }
    return new Object();
  }

}

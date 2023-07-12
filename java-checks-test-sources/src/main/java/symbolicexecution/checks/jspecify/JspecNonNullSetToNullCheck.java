package symbolicexecution.checks.jspecify;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

public class JspecNonNullSetToNullCheck {
  @NullMarked
  class C {

    @NonNull
    String s;
    String nullField = null; // FN

    String willBeAssignedNull;
    @org.jspecify.annotations.Nullable
    String canBeNull;

    public void method(@org.jspecify.annotations.Nullable String a) {
      s = null; // Noncompliant
      s = a; // Noncompliant
      willBeAssignedNull = null; // Noncompliant
      canBeNull = null; // Compliant
    }

    List<@NonNull String> list;
    List<? extends @NonNull Object> list2;

    public void setLists(@Nullable String s) {
      list = List.of(null); // FN - would require new SE features
      list2 = List.of(s); // FN - would require new SE features
    }

    public String getString() {
      return null; // Noncompliant
    }

    public String getString2() {
      return null; // Noncompliant
    }

  }

  class NoNullableInfo {

    @NullMarked
    public String getString() {
      return null; // Noncompliant
    }

    public String getString2() {
      return null; // Compliant
    }

  }
}

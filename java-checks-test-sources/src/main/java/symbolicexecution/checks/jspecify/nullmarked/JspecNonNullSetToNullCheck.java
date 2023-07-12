package symbolicexecution.checks.jspecify.nullmarked;

import java.util.List;
import javax.annotation.Nullable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

public class JspecNonNullSetToNullCheck {
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
      willBeAssignedNull = null; // FN
      canBeNull = null; // Compliant
    }

    List<@NonNull String> list;
    List<? extends @NonNull Object> list2;

    public void setLists(@Nullable String s) {
      list = List.of(null); // FN - would require new SE features
      list2 = List.of(s); // FN - would require new SE features
    }

    public String getString() {
      return null; // FN
    }

    public String getString2() {
      return null; // FN
    }

  }

  @NullUnmarked
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

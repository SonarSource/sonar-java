import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
class Annotations {

  static final String PATH = "/root";

  enum Mode {
    A, B
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
  @interface Config {
    String value() default "";

    String[] paths() default {};

    Mode mode() default Mode.A;

    int priority() default 1;

    Class<?> type() default Object.class;
  }

  @Config("single")
  int single;

  @Config(value = PATH, paths = {PATH + "/a", "/b"}, mode = Mode.B, priority = 2 + 3, type = String.class)
  void configured(@Config("param") String parameter) {
  }

  @Deprecated(since = "1.0", forRemoval = true)
  void removed() {
  }

  @SafeVarargs
  final void safe(List<String>... lists) {
  }

  @FunctionalInterface
  interface Callback {
    void call();
  }

  @Override
  public String toString() {
    return "annotations";
  }

  @SuppressWarnings("unused")
  private int unused;
}

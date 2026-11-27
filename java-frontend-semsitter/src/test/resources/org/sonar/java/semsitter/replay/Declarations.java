import java.io.Serializable;
import java.util.List;
import java.util.Map;
import static java.util.Collections.emptyList;

public abstract class Declarations<T extends Comparable<T>> implements Serializable, Comparable<Declarations<T>> {

  private static final long serialVersionUID = 1L;
  protected transient volatile int counter;
  List<String> names;
  Map<String, List<T>> index;
  int[] values;
  String[][] grid;
  T current;

  public Declarations() {
  }

  protected Declarations(T first) {
  }

  public abstract void run() throws Exception;

  public final synchronized <R> R convert(T input, Class<R> target) {
    return null;
  }

  static native void nativeCall();

  @Deprecated
  void legacy(String... args) {
  }

  @Override
  public int compareTo(Declarations<T> other) {
    return 0;
  }

  interface Listener {
    void onEvent(String event);

    default void close() {
    }

    static Listener none() {
      return null;
    }
  }

  enum Level {
    LOW, MEDIUM, HIGH;

    Level next() {
      return null;
    }
  }

  record Point(int x, int y) implements Serializable {
    Point {
    }

    static Point origin() {
      return null;
    }
  }

  @interface Marker {
    String value() default "";

    int priority() default 0;
  }

  static class Nested extends Declarations<String> {
    static {
    }

    {
    }

    @Override
    public void run() {
    }
  }

  sealed interface Shape permits Circle, Square {
  }

  record Circle(double radius) implements Shape {
  }

  static final class Square implements Shape {
  }
}

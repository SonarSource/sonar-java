import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

class Initializers {

  static final List<String> NAMES;
  static int counter;
  final int id;
  final List<Runnable> tasks = new ArrayList<>();

  static {
    NAMES = new ArrayList<>();
    NAMES.add("a");
    counter = NAMES.size();
  }

  {
    id = ++counter;
    tasks.add(() -> System.out.println(id));
  }

  Initializers() {
    this(0);
  }

  Initializers(int extra) {
    super();
    tasks.add(new Runnable() {
      private int runs;

      @Override
      public void run() {
        runs += extra;
      }
    });
  }

  Callable<Integer> local(int base) {
    class Adder implements Callable<Integer> {
      private final int delta;

      Adder(int delta) {
        this.delta = delta;
      }

      @Override
      public Integer call() {
        return base + delta + id;
      }
    }
    record Pair(int left, int right) {
      int sum() {
        return left + right;
      }
    }
    enum Kind {
      ONE, TWO
    }
    interface Shape {
      double area();
    }
    Shape square = () -> base * base;
    Pair pair = new Pair(base, Kind.TWO.ordinal());
    return new Adder(pair.sum() + (int) square.area());
  }

  Object anonymousGeneric() {
    return new ArrayList<String>() {
      {
        add("first");
      }

      @Override
      public boolean add(String value) {
        return super.add(value.trim());
      }
    };
  }
}

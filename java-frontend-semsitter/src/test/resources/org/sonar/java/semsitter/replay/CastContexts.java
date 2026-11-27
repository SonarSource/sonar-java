import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

class CastContexts {

  static final int LIMIT;
  Object target;

  static {
    int seed = 3;
    LIMIT = seed * 2;
  }

  {
    String label = "x";
    target = label;
  }

  Object returned(String s) {
    return (Object) s;
  }

  Object returnedParenthesized(String s) {
    return ((Object) s);
  }

  void assigned(String s, char c, byte b, short sh, int i, long l) {
    target = (Object) s;
    target = ((Object) s);
    int fromChar = (int) c;
    char toChar = (char) i;
    long shifted = (long) i << 2;
    int shiftedInt = (int) b << sh;
    long sum = (long) i + l;
    int bytes = (int) b + (int) sh;
    double fraction = (double) i / l;
    Object[] elements = new Object[1];
    String first = ((String[]) elements)[0];
    Object direct = ((Object[]) elements)[0];
    Integer boxed = (int) b;
  }

  void arguments(String s, List<Object> list) {
    list.add((Object) s);
    take((Object) s);
    overloaded((Object) s);
    varargs((Object) s);
    new Holder((Object) s);
    Supplier<Object> supplier = () -> (Object) s;
    Function<String, Object> function = value -> {
      return (Object) value;
    };
    run(() -> (Object) s);
  }

  void take(Object o) {
  }

  void overloaded(Object o) {
  }

  void overloaded(String s) {
  }

  void varargs(Object... values) {
  }

  void run(Supplier<Object> supplier) {
  }

  static class Holder {
    Holder(Object o) {
    }
  }

  void masked() throws IOException {
    try {
      throw new FileNotFoundException();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try (Reader reader = new FileReader("f")) {
      reader.read();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try (Closeable closeable = () -> {
    }) {
      closeable.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      try {
        throw new FileNotFoundException();
      } finally {
        target = null;
      }
    } catch (FileNotFoundException | IllegalStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Reader shared = new FileReader("g");
    try (shared) {
      shared.read();
    }
    try {
      synchronized (this) {
        throw new FileNotFoundException();
      }
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

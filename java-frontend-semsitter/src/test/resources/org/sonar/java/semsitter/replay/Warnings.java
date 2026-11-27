import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.*;

class Warnings {

  int field;

  void casts(Object o, String s, int i, long l, List<String> list) {
    String same = (String) s;
    Object widened = (Object) s;
    long promoted = (long) i;
    int narrowed = (int) l;
    double mixed = (double) i / 2;
    String downcast = (String) o;
    Object fromList = (Object) list.get(0);
    Integer boxed = (Integer) i;
    int unboxed = (int) boxed;
    Runnable lambda = (Runnable) () -> {
    };
    List<String> generic = (List<String>) list;
    Object array = (Object[]) new String[0];
    char letter = (char) 65;
  }

  void assignments(int value) {
    int local = 1;
    local = local;
    this.field = this.field;
    field = field;
    value = value;
    int other = local;
    other = max(other, 2);
  }

  void catches(InputStream in) {
    try {
      in.read();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (RuntimeException e) {
      throw e;
    }
    try {
      in.read();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void effectivelyFinal(List<String> items) throws Exception {
    int notFinal = 0;
    notFinal++;
    int captured = 1;
    String name = "n";
    Callable<String> callable = () -> name + captured;
    for (String item : items) {
      Runnable r = () -> System.out.println(item);
      r.run();
    }
    callable.call();
    Map<String, Integer> counts = new HashMap<>();
    List<Integer> values = new ArrayList<>(counts.values());
    values.add(notFinal);
  }
}

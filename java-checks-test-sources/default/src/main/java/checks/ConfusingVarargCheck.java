package checks;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

class ConfusingVarargCheck {

  public static void main(String[] args) {
    int[] arr = {1, 2, 3};
    Integer[] arrWrapper = {1, 2, 3};
    Object nullObject = null;

    new A(1, null); // Noncompliant {{Cast this argument to 'String' to pass a single element to the vararg method.}}
//           ^^^^
    new A(2, (String) null);
    new A(3, args);
    new A(4);

    vararg(null); // Noncompliant {{Cast this argument to 'Object' to pass a single element to the vararg method.}}
//         ^^^^
    vararg((null)); // Noncompliant
    vararg(arr); // Noncompliant {{Use an array of 'Integer' instead of an array of 'int'.}}
//         ^^^

    vararg((Object) null);
    vararg(arrWrapper);
    vararg(nullObject);
    vararg(1, 2, 3);
    vararg();

    primitiveVararg(null); // Noncompliant {{Remove this argument or pass an empty 'int' array to the vararg method.}}

    primitiveVararg(new int[] {});
    primitiveVararg(arr);
    primitiveVararg();
  }

  static void vararg(Object... s) {
    if (s == null) {
      System.out.println("null");
    } else {
      System.out.println("length: " + s.length);
    }
  }

  static void primitiveVararg(int... s) {
    if (s == null) {
      System.out.println("null");
    } else {
      System.out.println("length: " + s.length);
    }
  }

  private static class A {
    A(int i, String... s) {
      System.out.print(i + ": ");
      if (s == null) {
        System.out.println("null");
      } else {
        System.out.println("length: " + s.length);
      }
    }
  }

  enum MyEnum {
    A("my little pony", new byte[] {0x00, 0x00, 0xFFFFFFFE, 0xFFFFFFFF}), // Compliant
    B("armageddon", new byte[] {0xFFFFFFFE, 0xFFFFFFFF, 0x00, 0x00}, new byte[] {0x00, 0x00, 0xFFFFFFFF, 0xFFFFFFFE});

    MyEnum(String s, byte[]... bees) {}
  }

  static List<byte[]> inference(String s) {
    return Arrays.asList(s.getBytes(StandardCharsets.UTF_8)); // Compliant
  }

  void reflection(int[] ints) throws Exception {
    Method method = ConfusingVarargCheck.class.getMethod("foo", null); // Compliant
    method.invoke(this, null); // Compliant

    ConfusingVarargCheck.class.getConstructors()[0].newInstance(null); // Compliant

    Arrays.asList(ints).contains(42); // wrong usage, but not a FP, it simply creates a List<int[]>
  }
}

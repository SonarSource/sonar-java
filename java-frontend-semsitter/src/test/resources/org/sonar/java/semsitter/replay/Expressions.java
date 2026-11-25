import java.util.ArrayList;
import java.util.List;

class Expressions {

  static final int SIZE = 4;
  static final String NAME = "expr" + SIZE;
  static final long BIG = 1L << 40;
  static final char LETTER = 'a';
  static final double RATIO = 1.5e3;
  static final float SMALL = 2.5f;
  static final boolean FLAG = !false && (SIZE > 2 || SIZE <= 1);
  static final int HEX = 0xFF & 0b1010 | 017 ^ ~3;

  int a = 1;
  int b = a + 2 * 3 - 4 / 2 % 3;
  long c = a + 1L;
  double d = a / 2.0;
  int e = (int) d;
  Object o = "text";
  boolean isString = o instanceof String;
  String s = isString ? (String) o : null;
  int[] array = new int[SIZE];
  int[][] matrix = {{1, 2}, {3}};
  int[] init = new int[] {1, 2, 3};
  int first = init[0];
  int length = init.length;
  List<String> list = new ArrayList<>();
  Class<?> type = String.class;
  Class<?> arrayType = int[].class;
  String concat = "a" + 1 + 'c' + 2.0 + true + null;
  Object self = this;
  int shift = a >> 1 >>> 2;
  int negated = -a + +b;
  char nextLetter = (char) (LETTER + 1);
  byte small = (byte) 200;
  short shortValue = 10;
  Integer boxed = a;
  int unboxed = boxed;
  String text = String.valueOf(a).trim().toUpperCase();
  int size = list.size();
  boolean empty = list.isEmpty() && list.contains("x");
  Object created = new Object() {
    @Override
    public String toString() {
      return "anonymous";
    }
  };
  Inner inner = new Inner();
  Inner.Deep deep = inner.new Deep();
  int innerValue = inner.value;
  String qualified = java.util.Locale.ROOT.toString();

  int assign() {
    int x = 0;
    x += 1;
    x -= 2;
    x *= 3;
    x /= 4;
    x %= 5;
    x <<= 1;
    x >>= 1;
    x >>>= 1;
    x &= 7;
    x |= 8;
    x ^= 9;
    x++;
    x--;
    ++x;
    --x;
    return x;
  }

  String superCall() {
    return super.toString() + Expressions.this.hashCode();
  }

  class Inner {
    int value = SIZE;

    class Deep {
    }
  }
}

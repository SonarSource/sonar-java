import com.inetpsa.tst.HashFunction;
import com.inetpsa.tst.SipHashFunction;
import java.nio.ByteBuffer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Annotation(title = "plop", value = 51)
final class A {
  // All compliant
  int a = 0;
  int b = 1;
  int c = -1;

  int d = 2;
  int e = 42; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
//        ^^

  long aLong = 12L; // Noncompliant {{Assign this magic number 12L to a well-named constant, and use the constant instead.}}
  double aDouble = 12.3d; // Noncompliant {{Assign this magic number 12.3d to a well-named constant, and use the constant instead.}}
  float aFloat = 12.3F; // Noncompliant {{Assign this magic number 12.3F to a well-named constant, and use the constant instead.}}
  String string = "string";
  String strDouble = "123.3d";
  boolean bool = true;
  class A1 {
    long a = 0;
    long b = 1;
    long c = -1;
  }
  class A2 {
    double a = 0.0d;
    double b = 1.0d;
    double c = -1.0d;
  }
  class A3{
    float a = 0.0f;
    float b = 1.0f;
    float c = -1.0f;
  }

  private static final int CONSTANT = 42;

  private static final MyType MY_TYPE = new MyType() {
    int magic = 42; // Compliant, in final type (it is not checkstyle like)
  };

  public enum MyEnum {
    INSTANCE1(100), // Compliant
    INSTANCE2 { // Compliant
      void method() {
        System.out.println(42); // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
      }
    };

    MyEnum(int value) {
    }
  }

  final int myConst = 0; // Compliant because final
  final BigInteger bi = new BigInteger("16a09e667f3bcc908b2fb1366ea957d3e3adec17512775099da2f590b0667322a", 16); // Compliant, class constructor
  final Object[] p1 = new Object[42]; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
  static final Object[] p2 = new Object[42]; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}

  byte[] method() {
    final int foo = 42; // Compliant, because final
    final long[] la = {3L, 4L}; // Compliant, array initialisation
    final long[] lan = new long[] {3L, 4L}; // Compliant, array initialisation
    long[] array = new long[] {42}; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
    final Object[] o1 = new Object[42]; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
    Object[] o2 = new Object[42]; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
    final char[] c = new char[1024]; // Noncompliant {{Assign this magic number 1024 to a well-named constant, and use the constant instead.}}
    final List<Object> a = new ArrayList<>(42); // Compliant, class constructor
    final Long l = new Long(3L); // Compliant, class constructor
    final ByteBuffer b = ByteBuffer.allocateDirect(8); // Compliant, method usage
    return new byte[4096]; // Noncompliant {{Assign this magic number 4096 to a well-named constant, and use the constant instead.}}
  }

  private static final ThreadLocal<char[]> T = new ThreadLocal<char[]>() {
    @Override
    protected char[] initialValue() {
      return new char[1024]; // Compliant (known limitation), inside a final class (considered as Object)
    }
  };

  int nothing;
}

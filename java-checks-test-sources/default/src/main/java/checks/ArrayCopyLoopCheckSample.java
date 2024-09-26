package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

abstract class ArrayCopyLoopCheckSample implements Collection<Integer> {

  // test where src and dst are not primitives but compatible
  public void coverageEdgeCase(Long[] arrWE) {
    Number[] result = new Number[arrWE.length];
    for (int i = 0; i < arrWE.length; i++) {
      result[i] = arrWE[i]; // Noncompliant {{Use "Arrays.copyOf", "Arrays.asList", "Collections.addAll" or "System.arraycopy" instead.}}
    }
  }

  public Integer[] classToClass(Long[] arrWE) {
    Integer[] result = new Integer[arrWE.length];
    for (int i = 0; i < arrWE.length; i++) {
      result[i] = /* using auto boxing */ Integer.parseInt(String.valueOf(arrWE[i])); // Compliant
    }
    return result;
  }

  public Long[] classToClassLong(Integer[] arr) {
    Long[] result = new Long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ Long.parseLong(String.valueOf(arr[i])); // Compliant
    }
    return result;
  }

  public Integer[] boxed(int[] arr) {
    Integer[] result = new Integer[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ arr[i]; // Compliant
    }
    return result;
  }

  public int[] unboxed(Integer[] arr) {
    int[] result = new int[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto unboxing */ arr[i]; // Compliant
    }
    return result;
  }

  public Long[] boxed(long[] arr) {
    Long[] result = new Long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ arr[i]; // Compliant
    }
    return result;
  }

  public long[] unboxed(Long[] arr) {
    long[] result = new long[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto unboxing */ arr[i]; // Compliant
    }
    return result;
  }

  public Float[] boxed(float[] arr) {
    Float[] result = new Float[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ arr[i]; // Compliant
    }
    return result;
  }

  public float[] unboxed(Float[] arr) {
    float[] result = new float[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto unboxing */ arr[i]; // Compliant
    }
    return result;
  }

  public Double[] boxed(double[] arr) {
    Double[] result = new Double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ arr[i]; // Compliant
    }
    return result;
  }

  public double[] unboxed(Double[] arr) {
    double[] result = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto unboxing */ arr[i]; // Compliant
    }
    return result;
  }

  public Character[] boxed(char[] arr) {
    Character[] result = new Character[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto boxing */ arr[i]; // Compliant
    }
    return result;
  }

  public char[] unboxed(Character[] arr) {
    char[] result = new char[arr.length];
    for (int i = 0; i < arr.length; i++) {
      result[i] = /* using auto unboxing */ arr[i]; // Compliant
    }
    return result;
  }

  int x;

  void f() throws InterruptedException {
    int i = 0, from = 0, to = 0;
    char c = 0;
    byte b = 0;
    Integer[] src = new Integer[] {1, 2, 3, 4, 5};
    Integer[] dst = new Integer[src.length];
    List<Integer> list = new ArrayList<>();

    for (int j = 0; j < src.length; ++j) {
      dst[j] = src[j]; // Noncompliant {{Use "Arrays.copyOf", "Arrays.asList", "Collections.addAll" or "System.arraycopy" instead.}}
    }

    for (int j = 0; j < src.length; ++j) {
      list.add(src[j]); // Noncompliant
    }

    while (i < src.length) {
      dst[i] = src[i]; // Noncompliant
      ++i;
    }

    while (i < src.length) {
      list.add(src[i]); // Noncompliant
      ++i;
    }

    for (Integer n : src) {
      list.add(n); // Noncompliant
    }

    for (Integer n : src)
      list.add(n); // Noncompliant

    for (Integer n : new Integer[]{1, 2, 3, 4, 5}) {
      list.add(n); // Noncompliant
    }

    for (; from < to; ++from) {
      dst[from] = src[from]; // Noncompliant
    }

    for (; from < to; ++from) {
      list.add(src[from]); // Noncompliant
    }

    while (from < to) {
      list.add(src[from]); // Noncompliant
      ++from;
    }

    for (int j = 0; j < 5; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < 5 + 3; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j++) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j += 1) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j = j + 1) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j = 1 + j) {
      dst[j] = src[j]; // Noncompliant
    }

    while (i < src.length) {
      list.add(src[i]); // Noncompliant
      i++;
    }

    while (i < src.length) {
      list.add(src[i]); // Noncompliant
      i += 1;
    }

    while (i < src.length) {
      list.add(src[i]); // Noncompliant
      i = i + 1;
    }

    while (i < src.length) {
      list.add(src[i]); // Noncompliant
      i = 1 + i;
    }

    do {
      dst[i] = src[i]; // Noncompliant
      i++;
    } while (i < src.length);

    do {
      list.add(src[i]); // Noncompliant
      i++;
    } while (i < src.length);

    do {
      list.add(src[i]); // Noncompliant
      i += 1;
    } while (i < src.length);

    do {
      list.add(src[i]); // Noncompliant
      i = i + 1;
    } while (i < src.length);

    do {
      list.add(src[i]); // Noncompliant
      i = 1 + i;
    } while (i < src.length);

    for (int j = 0; j != src.length; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j <= src.length - 1; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; src.length > j; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; src.length - 1 >= j; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (;;) break;

    for (int j = 0; j < src.length; ++j);

    for (int j = 0; j < src.length; ++j) {}

    for (int j = 0; j < src.length; ++j) {
      System.out.println(src[i]);
    }

    for (int j = 0, k = 0; src.length - j != 0; ++j, ++k) {
      dst[j] = src[k];
    }

    for (int j = 0, k = 0; i < src.length; ++j, ++k) {
      dst[j] = src[k];
    }

    for (int j = 0; j < src.length;) {
      dst[j] = src[j];
      ++j;
    }

    for (int j = src.length - 1; j >= 0; j = j - 1) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; ++c) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; c += 1) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; c = (char)(c + 1)) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; j = j + one()) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; j = one() + j) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; ++j) {
      array()[i] = src[j];
    }

    for (int j = 0; j < src.length; ++j) {
      dst[j] = array()[i];
    }

    for (int j = 0; j < src.length; ++j) {
      list().add(src[j]);
    }

    for (int j = 0; j < src.length; ++j) {
      list.add(array()[j]);
    }

    for (int j = 0; src.length - j != 0; ++j) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; ++j) {
      dst[j] = j;
    }

    for (int j = 0; j < src.length; ++j) {
      i = src[j];
    }

    for (int j = 0; j < src.length; ++j) {
      i = j;
    }

    for (int n : new ArrayList<Integer>()) {
      list.add(n);
    }

    for (int j = 0; j < src.length; ++j) {
      dst.wait(src[j]);
    }

    for (int j = 0; j < src.length; ++j) {
      list.add(j);
    }

    for (int j = 0; j < src.length; ++j) {
      ;
    }

    for (int j = 0; j < src.length; ++j) {
      list();
    }

    for (int j = 0; j < src.length; j = j + 2) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; j = 2 + j) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; j = i + 2) {
      dst[j] = src[j];
    }

    for (int j = 0; j < src.length; j = 2 + i) {
      dst[j] = src[j];
    }

    i = 0;
    for (int n : src) {
      dst[i++] = n;
    }

    for (int j = 0; j < src.length; j += 2) {
      dst[j] = src[j];
    }

    for (int j = 0; j < j; ++j) {
      dst[j] = src[j];
    }

    for (this.x = 0; this.x < src.length; this.x += 1) {
      dst[this.x] = src[this.x];
    }

    for (this.x = 0; this.x < src.length; ++this.x) {
      dst[this.x] = src[this.x];
    }

    for (int j = 0; j < src.length; ++j) {
      add(src[j]);
    }

    while (i < src.length) {
      list.add(src[i++]);
    }

    while (i < src.length)
      list.add(src[i++]);

    while (i < src.length) {
      list.add(array()[i]);
      ++i;
    }

    while (i < src.length) {
      list().add(src[i]);
      ++i;
    }

    while (i < src.length) {
      list.contains(src[i]);
      ++i;
    }

    i = -1;
    while (i < src.length - 1) {
      ++i;
      list.add(src[i]);
    }

    while (i < src.length) {
      list.add(src[i++]);
      System.out.println(i);
    }

    while (c < b) {
      list.add(src[from]);
      ++from;
    }

    while (from < from) {
      list.add(src[from]);
      ++from;
    }

    while (from < to) {
      list.add(src[from++]);
    }

    while (to - from != 0) {
      list.add(src[from]);
      ++from;
    }

    while (from < to) {
      list.add(src[from++]);
      ;
    }

    for (int n : src) {
      list.contains(n);
    }

    for (int n : src) {
      list.add(n);
      ;
    }

    for (int n : src) {
      list();
    }

    for (int n : src) {
      dst.hashCode();
    }

    for (int n : src) {
      list.add(i);
    }

    for (int n : src) {
      list.add(1);
    }

    for (int n : src);

    for (int n : src) {
      add(n);
    }

    for (int n : src) {
      list().add(n);
    }

    for (int j = 0; !false; ++j) {
      dst[j] = src[j];
    }
  }

  void arraysOfPrimitives() {
    int i = 0, from = 0, to = 0;
    char c = 0;
    byte b = 0;
    int[] src = new int[]{1, 2, 3, 4, 5};
    int[] dst = new int[src.length];
    List<Integer> list = new ArrayList<>();

    for (int j = 0; j < src.length; ++j) {
      dst[j] = src[j]; // Noncompliant {{Use "Arrays.copyOf", "Arrays.asList", "Collections.addAll" or "System.arraycopy" instead.}}
    }

    for (int j = 0; j < src.length; ++j) {
      list.add(src[j]); // Compliant, no helper can copy an array of primitive into a collection
      // Collections.addAll(list, src); does not compile
      // Arrays.asList(src) will return a List<int[]>
    }

    do {
      dst[i] = src[i]; // Noncompliant
      ++i;
    } while (i < src.length);

    while (i < src.length) {
      dst[i] = src[i]; // Noncompliant
      ++i;
    }

    while (i < src.length) {
      list.add(src[i]); // Compliant, no helper can copy an array of primitive into a collection
      ++i;
    }

    for (int n : src) {
      list.add(n); // Compliant, no helper can copy an array of primitive into a collection
    }

    for (int n : src)
      list.add(n); // Compliant, no helper can copy an array of primitive into a collection

    for (int n : new int[]{1, 2, 3, 4, 5}) {
      list.add(n); // Compliant, no helper can copy an array of primitive into a collection
    }

    for (int n : new Integer[]{1, 2, 3, 4, 5}) {
      list.add(n); // Compliant, acceptable FN to avoid noise
    }

    for (; from < to; ++from) {
      dst[from] = src[from]; // Noncompliant
    }

    for (; from < to; ++from) {
      list.add(src[from]); // Compliant, no helper can copy an array of primitive into a collection
    }

    while (from < to) {
      list.add(src[from]); // Compliant, no helper can copy an array of primitive into a collection
      ++from;
    }

    for (int j = 0; j < 5; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < 5 + 3; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j++) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j += 1) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j = j + 1) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j < src.length; j = 1 + j) {
      dst[j] = src[j]; // Noncompliant
    }

    while (i < src.length) {
      list.add(src[i]); // Compliant, no helper can copy an array of primitive into a collection
      i++;
    }

    while (i < src.length) {
      list.add(src[i]); // Compliant, no helper can copy an array of primitive into a collection
      i += 1;
    }

    while (i < src.length) {
      list.add(src[i]); // Compliant, no helper can copy an array of primitive into a collection
      i = i + 1;
    }

    while (i < src.length) {
      list.add(src[i]); // Compliant, no helper can copy an array of primitive into a collection
      i = 1 + i;
    }

    for (int j = 0; j != src.length; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; j <= src.length - 1; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; src.length > j; ++j) {
      dst[j] = src[j]; // Noncompliant
    }

    for (int j = 0; src.length - 1 >= j; ++j) {
      dst[j] = src[j]; // Noncompliant
    }
  }

  public void copy(Collection<Integer> target, int[] source) {
    for (int s : source) {
      target.add(s); // FP: S3012. Collection.addAll does not accept int[] as an argument.
    }
  }

  private static void copyToSet(long[] array, Set<Long> set) {
    for (long labelId : array) {
      set.add(labelId); // Compliant
    }
  }

  private static void copyToNonCollection(int[] array) {
    class NonCollection {
      void add(Integer i) {}
    }

    NonCollection nonCollection = new NonCollection();
    for (Integer i : array) {
      nonCollection.add(i); // Compliant
    }
  }

  List<Integer> list() {
    return new ArrayList<>();
  }

  int[] array() {
    return new int[]{};
  }

  int one() {
    return 1;
  }

  @Override
  public boolean add(Integer n) {
    return true;
  }
}

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
class A extends Collection<Integer> {

  int x;

  void f() {
    int i = 0, from = 0, to = 0;
    char c = 0;
    byte b = 0;
    int[] src = new int[] {1, 2, 3, 4, 5};
    int[] dst = new int[src.length];
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

    for (int n : src) {
      list.add(n); // Noncompliant
    }

    for (int n : src)
      list.add(n); // Noncompliant

    for (int n : new int[]{1, 2, 3, 4, 5}) {
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

    for (int j = 0; j < src.length; c = c + 1) {
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

    for (int j = 0; !false; ++j) {
      dst[j] = src[j];
    }

    for (int n : new ArrayList<>()) {
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

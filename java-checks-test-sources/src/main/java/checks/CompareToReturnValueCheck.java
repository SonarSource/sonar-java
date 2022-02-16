package checks;

class CompareToReturnValueCheck {
  class A implements Comparable<A> {
    @Override
    public int compareTo(A a) {
      return Integer.MIN_VALUE; // Noncompliant [[sc=14;ec=31]] {{Simply return -1}}
    }

    public int compareTo() {
      return Short.MIN_VALUE; // Compliant
    }

    public int getMinValue() {
      return Integer.MIN_VALUE; // Compliant
    }

    public int compareTo(int a) {
      return -1; // Compliant
    }

    public boolean compareTo(Boolean a) {
      return a; // Compliant
    }

    public Long compareTo(Long a) {
      return Long.MIN_VALUE; // Compliant
    }

    public int compareTo(Short a) {
      return Integer.MAX_VALUE; // Compliant
    }
  }

  class B implements Comparable<B> {
    @Override
    public int compareTo(B b) {

      class C implements Comparable<C> {
        @Override
        public int compareTo(C c) {

          class D implements Comparable<D> {
            @Override
            public int compareTo(D d) {
              return Integer.MIN_VALUE; // Noncompliant
            }
          }

          return Integer.MIN_VALUE; // Noncompliant
        }
      }

      return Integer.MIN_VALUE; // Noncompliant
    }
  }

  class E implements Comparable<E> {
    @Override
    public int compareTo(E e) {
      return 0; // Compliant
    }
  }

  class F implements Comparable<F> {
    @Override
    public int compareTo(F e) {
      return Integer.MAX_VALUE; // Compliant
    }
  }

  class G implements Comparable<G> {
    @Override
    public int compareTo(G e) {
      return Short.MAX_VALUE; // Compliant
    }
  }

}

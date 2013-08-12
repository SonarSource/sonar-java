class A {
  private void f() {
    int a = new Comparable();

    new Comparable<T>() { // 1
      private void f() { // 2
        System.out.println(); // 3
        System.out.println(); // 4
      } // 5
    }; // 6

    new Comparable<T>() { // 1
      private void f() { // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
      } // 6
    }; // 7

    new Comparable<T>() { // 21

      @Override
      private void f(int a, int b) {
        if (a == b) {
          return 0;
        }

        return a < b;
      }

      private void g() {
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        return;
      }

    };
  }
}

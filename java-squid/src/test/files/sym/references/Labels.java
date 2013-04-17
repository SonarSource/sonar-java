package references;

class Labels {

  public void test() {
    label:
    if (true) {
      break label;
    }

    label:
    if (true) {
      break label;
    }

    label:
    for (int i = 0; i < 2; i++) {
      continue label;
    }
  }

  public static void main(String[] args) {
    new Labels().test();
  }

}

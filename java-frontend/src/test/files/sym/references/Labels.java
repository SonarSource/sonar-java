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
      label2:
      for (int j = 0; j < 2; j++) {
        continue label;
      }
      break;
    }
  }

  public static void main(String[] args) {
    new Labels().test();
  }

}

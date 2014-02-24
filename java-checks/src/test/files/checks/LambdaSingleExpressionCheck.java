class A {
  public void method() {
    IntStream.range(1, 5).map(x -> x * x - 1).forEach(x -> System.out.println(x));
    IntStream.range(1, 5).map(x -> {return x * x - 1;})
        .forEach(x -> {System.out.println(x + 11);});
  }

}

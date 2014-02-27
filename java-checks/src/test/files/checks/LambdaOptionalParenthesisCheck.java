class A{
  void method(){
    IntStream.range(1,5).map(a->a+1)
                        .map((a)->a+1);
  }
}
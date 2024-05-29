class A {
  private A[] toArr(String fo, A... as){
    return as;
  }

  void call() {
    toArr("asd");
  }
}

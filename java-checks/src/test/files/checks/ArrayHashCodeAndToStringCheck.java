class A {
  
  void method(String[] args, String string) {
    String argStr = args.toString(); // Noncompliant
    int argHash = args.hashCode(); // Noncompliant
    Class class1 = args.getClass();
    String str = string.toString();
    int hash = string.hashCode();
  }
}
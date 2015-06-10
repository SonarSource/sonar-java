class A {
  
  void method(String[] args, String string) {
    String argStr = args.toString(); // Noncompliant {{Use "Arrays.toString(array)" instead.}}
    int argHash = args.hashCode(); // Noncompliant {{Use "Arrays.hashCode(array)" instead.}}
    Class class1 = args.getClass();
    String str = string.toString();
    int hash = string.hashCode();
  }
}
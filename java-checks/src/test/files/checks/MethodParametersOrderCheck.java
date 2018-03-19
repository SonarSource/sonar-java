class A{
  
  public double divide(int divisor, int dividend) {
    return divisor/dividend;
  }

  public void doTheThing() {
    int divisor = 15;
    int dividend = 5;

    double result = divide(dividend, divisor);  // Noncompliant [[sc=21;ec=46]] {{Parameters to divide have the same names but not the same order as the method arguments.}}
  }
  
  public void doTheThing() {
    int divisor = 15;
    int dividend = 5;
    double result = divide(divisor, dividend);  // Compliant
  }
  
  int fun();
  
  void test(int parameter1, int parameter2) {
    int divisOr = 15;
    int dividend = 5;
  
    int result = divide(dividend, divisOr);  // Noncompliant {{Parameters to divide have the same names but not the same order as the method arguments.}}
  
    result = divide(divisOr, dividend);      // Compliant
    result = divide(parameter1, parameter2);  // Compliant
    result = divide(fun(), dividend);         //Compliant
  
    // coverage
    fun();
    
    java.util.Objects.equals(divisOr, dividend);
  }
  
  private void insert(Integer entry, @Nullable Integer oldEntryForKey) {
    
  } 
  
  public void test1() {
    int newEntry = 0;
    int oldEntryForKey  =0;
    int entry = 0;
    insert(newEntry, oldEntryForKey);  // Compliant 
    insert(entry, entry);           // Compliant
  }
}

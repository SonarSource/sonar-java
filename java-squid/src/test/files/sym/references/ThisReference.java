import java.util.Hashtable;
class A {

  private int param;

  A(int param) {
    this.param = param;
    this.init();
  }

  void init() {
    param++;
  }

  public final boolean IsEmpty()
  {
    return this.theHashtable.isEmpty();
  }

  // A hashtable to store the bindings
  private final Hashtable  theHashtable = new Hashtable();

}
package checks;

class DoubleCheckedLockingCheck {

  private UnknownType unknownType;

  public UnknownType unknownType() {
   if (unknownType == null) {
     synchronized (Helper.class) { // Noncompliant [[sc=6;ec=18;secondary=8,10]] {{Remove this dangerous instance of double-checked locking.}}
       if (unknownType == null) {
         this.unknownType = new UnknownType();
       }
     }
   }
  }

  class Helper  { }
}

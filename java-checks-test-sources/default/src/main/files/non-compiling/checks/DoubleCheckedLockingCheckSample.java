package checks;

class DoubleCheckedLockingCheckSample {

  private UnknownType unknownType;

  public UnknownType unknownType() {
   if (unknownType == null) {
//  ^^^<
     synchronized (Helper.class) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//   ^^^^^^^^^^^^
       if (unknownType == null) {
//  ^^^<
         this.unknownType = new UnknownType();
       }
     }
   }
  }

  class Helper  { }
}

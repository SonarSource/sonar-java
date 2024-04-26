class LazyArgEvaluationCheck {

  public static final java.util.logging.Logger logger = java.util.logging.Logger.getGlobal();

  public static void recordFieldsAccessors() {
    record MyUnknownRecord (Unknown name){
      Unknown name () {
        return name;
      }

      int age () {
        return 42;
      }
    }
    MyUnknownRecord myUnknownRecord = new MyUnknownRecord(new Unknown());
    logger.log(Level.SEVERE, "Something went wrong: " + myUnknownRecord.name()); // Compliant - unkown types are filtered out
    logger.log(Level.SEVERE, "Something went wrong: " + myUnknownRecord.age()); // Noncompliant
  }
}

package checks;

record LeftCurlyBraceEndLineCheck_record(Integer value) {

  // Test https://sonarsource.atlassian.net/browse/SONARJAVA-5116
  public LeftCurlyBraceEndLineCheck_record(String v) { // Compliant
    // but <- Complains about this curly brace
    this(Integer.parseInt(v));
  }

  public static final String FOR_SOME_REASON_THIS_NEEDS_TO_EXIST_HERE_TO_REPRODUCE_ISSUE = "IDK";
  public static final String FOR_SOME_REASON_THIS_NEEDS_TO_EXIST_HERE_TO_REPRODUCE_ISSUE_2 = "IDK";

}

record TestRecordSyntaxV2(String value) {

  // Test https://sonarsource.atlassian.net/browse/SONARJAVA-5116
  private void doThing() { // Compliant
    // but <- Complains about this curly brace
    new LeftCurlyBraceEndLineCheck_record("brace");
  }

  public record ThisRecordCausesTheIssueAsWell(String value) {
  }

}

record TestRecordSyntaxV3(Integer value)
{ // Noncompliant
  public TestRecordSyntaxV3(String v)
  { // Noncompliant
    this(Integer.parseInt(v));
  }
  public static final String CONST_1 = "IDK";
  public static final String CONST_2 = "IDK";
}

record GenericPair<T>(T first,
  T second) { // Compliant
}

record GenericPairWithMisplacedBrace<T>(T first,
  T second)
  { // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
//^
}

record EmptyGenericRecord<T>(
) { // Compliant
}

record GenericSerializableRecord<T>(T value)
  implements java.io.Serializable { // Compliant
}

record GenericSerializableRecordWithMisplacedBrace<T>(T value)
  implements java.io.Serializable
  { // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
//^
}

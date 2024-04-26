package checks.naming;

public class BadRecordNameNoncompliant {

  record badRecordName() { } // Noncompliant {{Rename this record name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.}}
//       ^^^^^^^^^^^^^
  record GoodRecordName() { }
}

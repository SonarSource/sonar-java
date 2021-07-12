package checks.naming;

public class BadRecordNameNoncompliant {

  record badRecordName() { } // Noncompliant [[sc=10;ec=23]] {{Rename this record name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.}}
  record GoodRecordName() { }
}

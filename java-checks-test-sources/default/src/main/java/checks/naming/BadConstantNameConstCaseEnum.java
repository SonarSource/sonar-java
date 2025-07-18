package checks.naming;

public enum BadConstantNameConstCaseEnum {
  TWENTY_ONE, // Noncompliant {{Rename this constant name to match the regular expression '^([A-Z][a-zA-Z0-9]*)*$'.}}
  THIRTY_TWO, // Noncompliant {{Rename this constant name to match the regular expression '^([A-Z][a-zA-Z0-9]*)*$'.}}
  FORTY_THREE, // Noncompliant {{Rename this constant name to match the regular expression '^([A-Z][a-zA-Z0-9]*)*$'.}}
}

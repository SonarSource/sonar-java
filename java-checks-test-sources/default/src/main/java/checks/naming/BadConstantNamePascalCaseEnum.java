package checks.naming;

public enum BadConstantNamePascalCaseEnum {
  TwentyOne,  // Noncompliant {{Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.}}
  ThirtyTwo, // Noncompliant {{Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.}}
  FourtyThree // Noncompliant {{Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.}}
}

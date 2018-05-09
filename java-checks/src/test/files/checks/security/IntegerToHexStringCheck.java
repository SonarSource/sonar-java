class A {
  public void foo(StringBuilder sb, byte b, int c) {
    sb.append(Integer.toHexString( b & 0xFF )); // Noncompliant {{Use String.format( "%02X", ...) instead.}}
    System.out.println(Integer.toHexString( b & 0xFF ));
    sb.append(Integer.toHexString( b )); // Noncompliant
    sb.append(Integer.toHexString( c ));
    sb.append(Integer.toHexString( 12 ));
    System.out.print(Integer.toHexString( b & 0xFF )); // Noncompliant
    System.out.print(Integer.toHexString( 0Xff & 23 )); // Noncompliant
    System.out.print(Integer.toHexString( b & c ));

    System.out.println(Integer.toHexString( b & 0xFF ));
  }

  public void bar(StringBuffer sb, byte b) {
    sb.append(Integer.toHexString( b & 0xFF )); // Noncompliant
  }
}

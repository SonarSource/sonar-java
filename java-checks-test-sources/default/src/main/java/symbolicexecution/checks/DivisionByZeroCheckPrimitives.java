package symbolicexecution.checks;

import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings(/* indentation check */ "java:S1120")
public abstract class DivisionByZeroCheckPrimitives {
  private static final double DOUBLE_ZERO = 0.0;
  private static final double DOUBLE_NOT_ZERO = 0.3048;
  private static final Double DOUBLE_WRAPPER_ZERO = 0.0;
  private static final Double DOUBLE_WRAPPER_NOT_ZERO = 0.3048;

  private static final float FLOAT_ZERO = 0.0f;
  private static final float FLOAT_NOT_ZERO = 0.3048f;
  private static final Float FLOAT_WRAPPER_ZERO = 0.0f;
  private static final Float FLOAT_WRAPPER_NOT_ZERO = 0.3048f;

  private static final int INT_ZERO = 0;
  private static final int INT_NOT_ZERO = 1;
  private static final Integer INT_WRAPPER_ZERO = 0;
  private static final Integer INT_WRAPPER_NOT_ZERO = 1;

  private static final long LONG_ZERO = 0L;
  private static final long LONG_NOT_ZERO = 1L;
  private static final Long LONG_WRAPPER_ZERO = 0L;
  private static final Long LONG_WRAPPER_NOT_ZERO = 1L;

  private static final byte BYTE_ZERO = 0;
  private static final byte BYTE_NOT_ZERO = 1;
  private static final Byte BYTE_WRAPPER_ZERO = 0;
  private static final Byte BYTE_WRAPPER_NOT_ZERO = 1;

  private static final short SHORT_ZERO = 0;
  private static final short SHORT_NOT_ZERO = 1;
  private static final Short SHORT_WRAPPER_ZERO = 0;
  private static final Short SHORT_WRAPPER_NOT_ZERO = 1;

  private static final char CHAR_ZERO = 0;
  private static final char CHAR_NOT_ZERO = 'a';

  public int notZero(byte b)      { return b / BYTE_NOT_ZERO; }
  public int notZero(short s)     { return s / SHORT_NOT_ZERO; }
  public int notZero(int i)       { return i / INT_NOT_ZERO; }
  public long notZero(long l)     { return l / LONG_NOT_ZERO; }
  public double notZero(double d) { return d / DOUBLE_NOT_ZERO; }
  public float notZero(float f)   { return f / FLOAT_NOT_ZERO; }
  public int notZero(char c)      { return c / CHAR_NOT_ZERO; }
  public BigInteger notZero1(BigInteger bi) { return bi.divide(BigInteger.ONE); }
  public BigInteger notZero2(BigInteger bi) { return bi.divide(BigInteger.valueOf(LONG_NOT_ZERO)); }
  public BigDecimal notZero1(BigDecimal bd) { return bd.divide(BigDecimal.ONE); }
  public BigDecimal notZero2(BigDecimal bd) { return bd.divide(BigDecimal.valueOf(DOUBLE_NOT_ZERO)); }

  public int notZero(Byte b)      { return b / BYTE_WRAPPER_NOT_ZERO; }
  public int notZero(Short s)     { return s / SHORT_WRAPPER_NOT_ZERO; }
  public int notZero(Integer i)   { return i / INT_WRAPPER_NOT_ZERO; }
  public long notZero(Long l)     { return l / LONG_WRAPPER_NOT_ZERO; }
  public double notZero(Double d) { return d / DOUBLE_WRAPPER_NOT_ZERO; }
  public float notZero(Float f)   { return f / FLOAT_WRAPPER_NOT_ZERO; }

  public int zero(byte b)      { return b / BYTE_ZERO; } // Noncompliant
  public int zero(short s)     { return s / SHORT_ZERO; } // Noncompliant
  public int zero(int i)       { return i / INT_ZERO; } // Noncompliant
  public long zero(long l)     { return l / LONG_ZERO; } // Noncompliant
  public double zero(double d) { return d / DOUBLE_ZERO; } // Noncompliant
  public float zero(float f)   { return f / FLOAT_ZERO; } // Noncompliant
  public int zero(char c)      { return c / CHAR_ZERO; } // Noncompliant
  public BigInteger zero1(BigInteger bi) { return bi.divide(BigInteger.ZERO); } // Noncompliant
  public BigInteger zero2(BigInteger bi) { return bi.divide(BigInteger.valueOf(LONG_ZERO)); } // Noncompliant
  public BigDecimal zero1(BigDecimal bd) { return bd.divide(BigDecimal.ZERO); } // Noncompliant
  public BigDecimal zero2(BigDecimal bd) { return bd.divide(BigDecimal.valueOf(DOUBLE_ZERO)); } // Noncompliant

  public int zero(Byte b)      { return b / BYTE_WRAPPER_ZERO; }   // FN
  public int zero(Short s)     { return s / SHORT_WRAPPER_ZERO; }  // FN
  public int zero(Integer i)   { return i / INT_WRAPPER_ZERO; }    // FN
  public long zero(Long l)     { return l / LONG_WRAPPER_ZERO; }   // FN
  public double zero(Double d) { return d / DOUBLE_WRAPPER_ZERO; } // FN
  public float zero(Float f)   { return f / FLOAT_WRAPPER_ZERO; }  // FN
}

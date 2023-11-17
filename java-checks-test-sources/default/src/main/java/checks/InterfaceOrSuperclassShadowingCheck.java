package checks;

public class InterfaceOrSuperclassShadowingCheck {
  interface Closeable extends java.io.Closeable {} // Compliant - inner class
}

interface MyCloseable extends java.io.Closeable {}
interface Closeable extends java.io.Closeable {} // Noncompliant {{Rename this interface.}}

class MyFile extends java.io.File {
  public MyFile(String pathname) { super(pathname); }
}

class File extends java.io.File {  // Noncompliant {{Rename this class.}}
  public File(String pathname) { super(pathname); }
}

abstract class MySerializable implements java.io.Serializable {}
abstract class Serializable implements java.io.Serializable {} // Noncompliant [[sc=16;ec=28]] {{Rename this class.}}

abstract class MyFlushable implements java.io.Flushable {}
abstract class Flushable extends MyFlushable {}

record Function<T, R> () implements java.util.function.Function { // Noncompliant [[sc=8;ec=16]] {{Rename this record.}}
  @Override public Object apply(Object t) { return null; }
}

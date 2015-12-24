interface MyCloseable extends java.io.Closeable {}
interface Closeable extends java.io.Closeable {} // Noncompliant {{Rename this interface.}}

class MyFile extends java.io.File {}
class File extends java.io.File {} // Noncompliant {{Rename this class.}}

abstract class MySerializable implements java.io.Serializable {}
abstract class Serializable implements java.io.Serializable {} // Noncompliant [[sc=16;ec=28]] {{Rename this class.}}

abstract class MyFlushable implements java.io.Flushable {}
abstract class Flushable extends MyFlushable {}

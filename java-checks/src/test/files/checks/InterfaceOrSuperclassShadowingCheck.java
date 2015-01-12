interface MyCloseable extends java.io.Closeable {}
interface Closeable extends java.io.Closeable {} // Noncompliant

class MyFile extends java.io.File {}
class File extends java.io.File {} // Noncompliant

abstract class MySerializable implements java.io.Serializable {}
abstract class Serializable implements java.io.Serializable {} // Noncompliant

abstract class MyFlushable implements java.io.Flushable {}
abstract class Flushable extends MyFlushable {}

import java.io.Serializable;
import java.util.Comparator;

class A implements Comparator<String> {} // Noncompliant
class B implements Comparator<String>, Serializable {}
abstract class C implements Comparator<String> {}
class D extends C {} // Noncompliant
class E implements Cloneable {}

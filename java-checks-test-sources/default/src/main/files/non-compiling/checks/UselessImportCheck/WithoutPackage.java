import a.Foo;    // Compliant
import a.Bar; // Noncompliant
//^[sc=1;ec=14]

class Foobar extends Foo {

}

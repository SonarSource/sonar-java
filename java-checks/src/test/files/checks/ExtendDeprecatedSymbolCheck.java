@Deprecated
class DeprecatedClass {}
@Deprecated
interface DeprecatedInterface {}

class A extends DeprecatedClass {} // Noncompliant {{"DeprecatedClass" is deprecated, extend the suggested replacement instead.}}
class B implements DeprecatedInterface {} // Noncompliant
interface I extends DeprecatedInterface {} // Noncompliant
enum MyEnum implements DeprecatedInterface {} // Noncompliant
class C extends A implements I {} //compliant not directly extending deprecated symbols.
@Deprecated
class D extends DeprecatedClass {} //compliant class is deprecated itself.

@Deprecated
class DeprecatedClass {}
@Deprecated
interface DeprecatedInterface {}

class A extends DeprecatedClass {} //Non-Compliant
class B implements DeprecatedInterface {} //Non-Compliant
interface I extends DeprecatedInterface {} //Non-Compliant
enum MyEnum implements DeprecatedInterface {} //Non-Compliant
class C extends A implements I {} //compliant not directly extending deprecated symbols.
@Deprecated
class D extends DeprecatedClass {} //compliant class is deprecated itself.

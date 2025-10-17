public class NoTestInTestClassCheckUnknownParent {

}

class SanityCheckTest { // Noncompliant {{Add some tests to this class.}}

}

class UnknownParentTest extends Unknown { // Compliant : Uknown is not resolved

}

class UnknownParentTransitiveTest extends UnknownParentTest { // Compliant : Transitively unknown from UnknownParentTest

}

class UnknownInterfaceTest implements IUnknown { // Compliant : IUknown is not resolved

}

class UnknownInterfaceTransitive1Test extends UnknownInterfaceTest { // Compliant : Transitively unknown from UnknownInterfaceTest

}

interface ITest extends IUnknown {

}

class UnknownInterfaceTransitive2Test implements ITest { // Compliant : Transitively unknown from ITest

}

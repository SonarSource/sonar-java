package checks;

class MethodOverrideAccessibilityCheckSample {

  // --- Instance override: protected -> public ---

  static class Parent1 {
    protected void process() {}
//                 ^^^^^^^>
  }

  static class ChildProtectedToPublic extends Parent1 {
    @Override
    public void process() {} // Noncompliant {{Increase of accessibility from "protected" to "public" when overriding method.}}
//              ^^^^^^^
  }

  // --- Instance override: package-private -> public ---

  static class Parent2 {
    void handle() {}
//       ^^^^^^>
  }

  static class ChildPackageToPublic extends Parent2 {
    @Override
    public void handle() {} // Noncompliant {{Increase of accessibility from "package-private" to "public" when overriding method.}}
//              ^^^^^^
  }

  // --- Instance override: package-private -> protected ---

  static class Parent3 {
    void doWork() {}
//       ^^^^^^>
  }

  static class ChildPackageToProtected extends Parent3 {
    @Override
    protected void doWork() {} // Noncompliant {{Increase of accessibility from "package-private" to "protected" when overriding method.}}
//                 ^^^^^^
  }

  // --- Static hiding: protected -> public ---

  static class Parent4 {
    protected static void compute(int x) {}
//                        ^^^^^^^>
  }

  static class ChildStaticProtectedToPublic extends Parent4 {
    public static void compute(int x) {} // Noncompliant {{Increase of accessibility from "protected" to "public" when hiding method.}}
//                     ^^^^^^^
  }

  // --- Static hiding: package-private -> protected ---

  static class Parent5 {
    static void resolve(String s) {}
//              ^^^^^^^>
  }

  static class ChildStaticPackageToProtected extends Parent5 {
    protected static void resolve(String s) {} // Noncompliant {{Increase of accessibility from "package-private" to "protected" when hiding method.}}
//                        ^^^^^^^
  }

  // --- Static hiding: package-private -> public ---

  static class Parent6 {
    static void transform(int y) {}
//              ^^^^^^^^^>
  }

  static class ChildStaticPackageToPublic extends Parent6 {
    public static void transform(int y) {} // Noncompliant {{Increase of accessibility from "package-private" to "public" when hiding method.}}
//                     ^^^^^^^^^
  }

  // --- Abstract method implementation with increased access ---

  static abstract class AbstractParent1 {
    abstract void doTask();
//                ^^^^^^>
  }

  static class ConcretePackageToProtected extends AbstractParent1 {
    @Override
    protected void doTask() {} // Noncompliant {{Increase of accessibility from "package-private" to "protected" when overriding method.}}
//                 ^^^^^^
  }

  static abstract class AbstractParent2 {
    abstract void perform();
//                ^^^^^^^>
  }

  static class ConcretePackageToPublic extends AbstractParent2 {
    @Override
    public void perform() {} // Noncompliant {{Increase of accessibility from "package-private" to "public" when overriding method.}}
//              ^^^^^^^
  }

  static abstract class AbstractParent3 {
    protected abstract void execute();
//                          ^^^^^^^>
  }

  static class ConcreteProtectedToPublic extends AbstractParent3 {
    @Override
    public void execute() {} // Noncompliant {{Increase of accessibility from "protected" to "public" when overriding method.}}
//              ^^^^^^^
  }

  // --- Multi-level hierarchy ---

  static class GrandParent {
    protected void action() {}
//                 ^^^^^^>
  }

  static class MiddleClass extends GrandParent {
    @Override
    protected void action() {} // Compliant - same access level
  }

  static class GrandChild extends GrandParent {
    @Override
    public void action() {} // Noncompliant {{Increase of accessibility from "protected" to "public" when overriding method.}}
//              ^^^^^^
  }

  // GrandChild via already-widened parent: compliant since direct parent is public
  static class AlreadyPublicParent {
    public void method() {}
  }

  static class GrandChildCompliant extends AlreadyPublicParent {
    @Override
    public void method() {} // Compliant - same as direct parent
  }

  // --- Compliant: same access level maintained ---

  static class Parent7 {
    protected void keep() {}
  }

  static class SameProtected extends Parent7 {
    @Override
    protected void keep() {} // Compliant
  }

  static class Parent8 {
    void maintain() {}
  }

  static class SamePackagePrivate extends Parent8 {
    @Override
    void maintain() {} // Compliant
  }

  static class Parent9 {
    public void stay() {}
  }

  static class SamePublic extends Parent9 {
    @Override
    public void stay() {} // Compliant
  }

  // --- Compliant: same access level for static hiding ---

  static class Parent10 {
    protected static void staticKeep(int x) {}
  }

  static class SameStaticProtected extends Parent10 {
    protected static void staticKeep(int x) {} // Compliant
  }

  // --- Compliant: interface implementation ---

  interface Processor {
    void run();
  }

  static class ProcessorImpl implements Processor {
    @Override
    public void run() {} // Compliant - interface methods are implicitly public
  }

  // --- Compliant: constructors ---

  static class ParentWithConstructor {
    protected ParentWithConstructor() {}
  }

  static class ChildWithConstructor extends ParentWithConstructor {
    public ChildWithConstructor() {} // Compliant - constructors don't override
  }

  // --- Compliant: method not overriding anything ---

  static class Parent11 {
    protected void existing() {}
  }

  static class NewMethod extends Parent11 {
    public void newMethod() {} // Compliant - not overriding
  }

  // --- Compliant: private methods cannot be overridden ---

  static class BaseWithPrivate {
    private void secret() {}
  }

  static class ChildOfPrivate extends BaseWithPrivate {
    public void secret() {} // Compliant - not overriding, private methods are not visible
  }
}

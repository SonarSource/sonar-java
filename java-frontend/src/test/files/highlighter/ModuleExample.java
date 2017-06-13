/*
 * Header
 */
import com.google.common.annotations.Beta;

/**
 * Javadoc
 */
@Beta
open module com.greetings {
  requires transitive;
  requires static transitive;
  requires static transitive org.foo.bar;
  exports foo.bar to com.module1, gul.bar.qix;
  opens gul.lom to moc.loe.module2, ahah.bro.force;
  uses bar.foo.MyInterface;
  provides com.Greetings with org.foo.Greetings, foo.bar.Salutations;
  // usages of restricted keywords in module name and package names
  exports com.sun.security.module;
  provides javax.security.Module with alpha.with.to.exports.module.MyModule;
}

/**
 * TODO remove {@link org.sonar.java.model.JavaTree#getLine()} which is unfortunately not the same as
 * {@link org.sonar.plugins.java.api.tree.Tree#firstToken() firstToken()}.{@link org.sonar.plugins.java.api.tree.SyntaxToken#line() line()}
 * see for example {@link org.sonar.java.model.declaration.MethodTreeImpl#getLine()}
 *
 * TODO remove {@link org.sonar.java.model.JavaTree#isLeaf()}
 *
 * TODO {@link org.sonar.java.model.JavaTree#getChildren()} should not throw exception
 *
 * TODO idea: introduction of zero-width token for {@link org.sonar.plugins.java.api.tree.InferedTypeTree} allows to get rid of nullability of
 * {@link org.sonar.plugins.java.api.tree.Tree#firstToken()} and {@link org.sonar.plugins.java.api.tree.Tree#lastToken()}
 * or maybe get rid of the first one?
 *
 * TODO make {@link org.sonar.plugins.java.api.tree.ListTree} immutable
 *
 * TODO incorrect number of trivias in some tests
 *
 * TODO review differences:
 * computation of {@link org.sonar.plugins.java.api.tree.Tree#firstToken()} and {@link org.sonar.plugins.java.api.tree.Tree#lastToken()}
 *
 * TODO test weird syntax:
 * variable vs name in try-with-resources
 */
@javax.annotation.ParametersAreNonnullByDefault
package org.sonar.java.ecj;

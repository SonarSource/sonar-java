package org.sonar.java.checks.helpers;

import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public class UtilClassUtils {

  private UtilClassUtils() {
  }

  public static boolean isPrivateInnerClass(ClassTree classTree) {
    return !classTree.symbol().owner().isPackageSymbol() &&
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PRIVATE);
  }

  public static boolean isUtilityClass(ClassTree classTree) {
    return hasOnlyStaticMembers(classTree) && !anonymousClass(classTree) && !extendsAnotherClassOrImplementsSerializable(classTree)
      && !containsMainMethod(classTree);
  }

  private static boolean containsMainMethod(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .anyMatch(method -> MethodTreeUtils.isMainMethod((MethodTree) method));
  }

  private static boolean hasOnlyStaticMembers(ClassTree classTree) {
    List<Tree> members = classTree.members();
    if (noStaticMember(members)) {
      return false;
    }
    return members.stream().allMatch(member -> isConstructor(member) || isStatic(member) || member.is(Tree.Kind.EMPTY_STATEMENT));
  }

  private static boolean anonymousClass(ClassTree classTree) {
    return classTree.simpleName() == null;
  }

  private static boolean extendsAnotherClassOrImplementsSerializable(ClassTree classTree) {
    return classTree.superClass() != null || classTree.symbol().type().isSubtypeOf("java.io.Serializable");
  }

  private static boolean noStaticMember(List<Tree> members) {
    return members.stream().noneMatch(UtilClassUtils::isStatic);
  }

  private static boolean isStatic(Tree member) {
    if (member.is(Tree.Kind.STATIC_INITIALIZER)) {
      return true;
    }
    if (member.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) member;
      return hasStaticModifier(variableTree.modifiers());
    } else if (member.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) member;
      return hasStaticModifier(methodTree.modifiers());
    } else if (isClassTree(member)) {
      ClassTree classTree = (ClassTree) member;
      return hasStaticModifier(classTree.modifiers());
    }
    return false;
  }

  private static boolean isClassTree(Tree member) {
    return member.is(Tree.Kind.CLASS) || member.is(Tree.Kind.ANNOTATION_TYPE) || member.is(Tree.Kind.INTERFACE) || member.is(Tree.Kind.ENUM);
  }

  private static boolean hasStaticModifier(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC);
  }

  private static boolean isConstructor(Tree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR);
  }

}

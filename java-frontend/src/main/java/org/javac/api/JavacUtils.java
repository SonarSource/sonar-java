package org.javac.api;

import com.sun.source.tree.MethodTree;

public class JavacUtils {

  public static boolean isImplicitConstructor(com.sun.source.tree.Tree tree) {
    if (tree.getKind() != com.sun.source.tree.Tree.Kind.METHOD) {
      return false;
    }
    MethodTree methodTree = (MethodTree) tree;
    return methodTree.getName().contentEquals("<init>") && methodTree.getParameters().isEmpty();
  }

}

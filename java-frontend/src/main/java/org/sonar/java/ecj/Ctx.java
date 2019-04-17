package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IBinding;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Ctx {

  final AST ast;

  final Map<IBinding, Tree> declarations = new HashMap<>();

  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();

  Ctx(AST ast) {
    this.ast = ast;
  }

  void declaration(IBinding binding, Tree node) {
    if (binding == null) {
      return;
    }
    declarations.put(binding, node);
  }

  void usage(IBinding binding, EIdentifier identifier) {
    if (binding == null) {
      return;
    }
    usages.computeIfAbsent(binding, k -> new ArrayList<>()).add(identifier);
  }
}

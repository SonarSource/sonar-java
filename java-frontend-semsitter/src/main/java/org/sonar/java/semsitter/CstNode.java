/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonarsource.semsitter.wire.Ref;

import static org.sonar.java.semsitter.CstNodes.isComment;

final class CstNode {

  private static final String BOUND = "bound";

  final org.sonarsource.semsitter.wire.Node proto;

  CstNode(org.sonarsource.semsitter.wire.Node proto) {
    this.proto = proto;
  }

  String k() {
    return proto.getKind();
  }

  String f() {
    return proto.getField();
  }

  String text() {
    return proto.getText();
  }

  boolean missing() {
    return proto.getMissing();
  }

  int line() {
    return proto.getStartLine();
  }

  int col() {
    return proto.getStartCol();
  }

  List<CstNode> children() {
    List<CstNode> children = new ArrayList<>();
    for (CstNode child : allChildren()) {
      if (!isComment(child)) {
        children.add(child);
      }
    }
    return children;
  }

  List<CstNode> allChildren() {
    return proto.getChildrenList().stream().map(CstNode::new).toList();
  }

  @Nullable
  String decl() {
    return proto.getDeclsCount() == 0 ? null : proto.getDecls(0);
  }

  List<String> decls() {
    return proto.getDeclsList();
  }

  boolean unresolved() {
    if (proto.getRefsCount() == 0) {
      return "type_identifier".equals(k());
    }
    for (Ref ref : proto.getRefsList()) {
      if (BOUND.equals(ref.getOutcome())) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  String valueTarget() {
    return target(true);
  }

  @Nullable
  String typeTarget() {
    return target(false);
  }

  @Nullable
  String target(boolean value) {
    String fallback = null;
    for (Ref ref : proto.getRefsList()) {
      if (ref.getTarget().isEmpty() || !BOUND.equals(ref.getOutcome())) {
        continue;
      }
      String access = ref.getAccess();
      boolean isValue = "r".equals(access) || "w".equals(access) || "rw".equals(access);
      if (isValue == value) {
        return ref.getTarget();
      }
      fallback = ref.getTarget();
    }
    return value ? null : fallback;
  }
}

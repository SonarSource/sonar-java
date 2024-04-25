/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.java.api.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.sonar.plugins.java.api.tree.Tree;

public class Selector<T> {

  public interface Context {
    void reportIssue(Tree tree, String message);
  }

  private final Class<T> selectorType;
  private final Selector<?> root;
  private final Selector<?> parent;
  protected final List<BiConsumer<Context, T>> visitors = new ArrayList<>();

  public Selector(Class<T> selectorType, Selector<?> parent) {
    this.selectorType = selectorType;
    this.root = parent != null ? parent.root : this;
    this.parent = parent;
  }

  public Selector<T> visit(BiConsumer<Context, T> visitor) {
    this.visitors.add(visitor);
    return this;
  }

  public Selector<T> apply(Context ctx, Tree tree) {
    root.rootApply(ctx, tree);
    return this;
  }

  private void rootApply(Context ctx, Tree tree) {
    if (selectorType.isInstance(tree)) {
      visit(ctx, selectorType.cast(tree));
    }
  }

  protected void visit(Context ctx, T tree) {
    visitors.forEach(visitor -> visitor.accept(ctx, tree));
  }

  protected <R, Q extends Selector<R>> Q addConversion(Q resultQuery, Function<T, R> conversion) {
    var visitor = new Conversion<T, R, Q>(resultQuery, conversion);
    visitors.add(visitor::visit);
    return resultQuery;
  }

  protected <R, Q extends Selector<R>> Q add(Conversion<T, R, Q> visitor) {
    visitors.add(visitor::visit);
    return visitor.query();
  }

  protected <R, Q extends Selector<R>> Q add(ListConversion<T, R, Q> visitor) {
    visitors.add(visitor::visit);
    return visitor.query();
  }

  protected record Conversion<T, R, Q extends Selector<R>>(Q query, Function<T, R> getter) {
    void visit(Context ctx, T tree) {
      var childTree = getter.apply(tree);
      if (childTree != null) {
        query.visit(ctx, childTree);
      }
    }
  }

  protected record ListConversion<T, R, Q extends Selector<R>>(Q query, Function<T, List<R>> getter) {
    void visit(Context ctx, T tree) {
      var childTreeList = getter.apply(tree);
      if (childTreeList != null) {
        for (R childTree : childTreeList) {
          if (childTree != null) {
            query.visit(ctx, childTree);
          }
        }
      }
    }
  }

}

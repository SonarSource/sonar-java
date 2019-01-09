/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.collections;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SinglyLinkedListTest {

  @Test
  public void test() {
    PStack<Object> empty = PCollections.emptyStack();
    assertThat(empty.isEmpty()).isTrue();
    assertThat(empty.toString()).isEqualTo("[]");

    Object a = new Object(){
      @Override
      public String toString() {
        return "a";
      }
    };
    PStack<Object> one = empty.push(a);
    assertThat(one.toString()).isEqualTo("[a]");
    assertThat(one.isEmpty()).isFalse();
    assertThat(one.peek()).isSameAs(a);
    assertThat(one.pop()).isSameAs(empty);

    Object b = new Object(){
      @Override
      public String toString() {
        return "b";
      }
    };
    PStack<Object> two = one.push(b);
    assertThat(two.toString()).isEqualTo("[b, a]");
    assertThat(two.isEmpty()).isFalse();
    assertThat(two.peek()).isSameAs(b);
    assertThat(two.pop()).isSameAs(one);
  }

  @Test
  public void forEach() {
    List<Object> consumer = new ArrayList<>();
    PCollections.emptyStack().forEach(consumer::add);
    assertThat(consumer).isEmpty();

    Object a = new Object();
    Object b = new Object();
    PStack<Object> s = PCollections.emptyStack().push(b).push(a);
    s.forEach(consumer::add);
    assertThat(consumer).isEqualTo(ImmutableList.of(a, b));
  }

  @Test
  public void equality() {
    Object a = new Object();
    Object b = new Object();
    Object c = new Object();

    PStack<Object> s1 = PCollections.emptyStack().push(b).push(a);
    PStack<Object> s2 = PCollections.emptyStack().push(b).push(a);
    assertThat(s1.equals(s2)).isTrue();

    // twice to cover hashCode cache
    assertThat(s1.hashCode() == s2.hashCode()).isTrue();
    assertThat(s1.hashCode() == s2.hashCode()).isTrue();

    assertThat(s1).isNotEqualTo(null);
    assertThat(s1).isEqualTo(s1);

    s1 = PCollections.emptyStack().push(b).push(a);
    s2 = PCollections.emptyStack().push(c).push(a);
    assertThat(s1.equals(s2)).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void empty_pop() {
    PCollections.emptyStack().pop();
  }

  @Test(expected = IllegalStateException.class)
  public void empty_peek() {
    PCollections.emptyStack().peek();
  }

  @Test
  public void anyMatch() {
    PStack<Object> s = PCollections.emptyStack();
    Object a = new Object();
    Object b = new Object();
    assertThat(s.anyMatch(e -> e == a)).isFalse();
    assertThat(s.push(a).anyMatch(e -> e == a)).isTrue();
    assertThat(s.push(a).push(b).anyMatch(e -> e == a)).isTrue();
    Object c = new Object();
    assertThat(s.push(a).push(b).anyMatch(e -> e == c)).isFalse();
  }

  @Test
  public void size() {
    PStack<Object> s = PCollections.emptyStack();
    assertThat(s.size()).isEqualTo(0);
    s = s.push(new Object());
    assertThat(s.size()).isEqualTo(1);
    s = s.push(new Object());
    assertThat(s.size()).isEqualTo(2);
    s = s.pop().pop();
    assertThat(s.size()).isEqualTo(0);
  }

  @Test
  public void peek() {
    assertThatThrownBy(() -> PCollections.emptyStack().peek(0)).isInstanceOf(IllegalStateException.class);

    Object a = new Object();
    PStack<Object> s = PCollections.emptyStack().push(a);
    assertThat(s.peek(0)).isEqualTo(s.peek());
    Object b = new Object();
    s = s.push(b);
    assertThat(s.peek(1)).isEqualTo(a);
    PStack<Object> finalS = s;
    assertThatThrownBy(() -> finalS.peek(2)).isInstanceOf(IllegalStateException.class);
  }

}

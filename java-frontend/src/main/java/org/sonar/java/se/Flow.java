/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Flow implements List<JavaFileScannerContext.Location> {

  private final List<JavaFileScannerContext.Location> items;
  private boolean exceptional;

  public Flow() {
    this(new ArrayList<>(), false);
  }

  private Flow(List<JavaFileScannerContext.Location> items, boolean exceptional) {
    this.items = items;
    this.exceptional = exceptional;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
      .append(items)
      .append(exceptional)
      .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    Flow other = (Flow) obj;
    return new EqualsBuilder()
      .append(items, other.items)
      .append(exceptional, other.exceptional)
      .isEquals();
  }

  public void setAsExceptional() {
    exceptional = true;
  }

  public boolean isExceptional() {
    return exceptional;
  }

  public boolean isNonExceptional() {
    return !exceptional;
  }

  public static Flow empty() {
    return new Flow(Collections.emptyList(), false);
  }

  public static Flow copyOf(Flow currentFlow) {
    return new Flow(ImmutableList.copyOf(currentFlow.items), currentFlow.exceptional);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Flow of(JavaFileScannerContext.Location location) {
    return new Flow(Collections.singletonList(location), false);
  }

  public Flow reverse() {
    return new Flow(Lists.reverse(items), exceptional);
  }

  @Override
  public int size() {
    return items.size();
  }

  @Override
  public boolean isEmpty() {
    return items.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return items.contains(o);
  }

  @Override
  public Iterator<JavaFileScannerContext.Location> iterator() {
    return items.iterator();
  }

  @Override
  public Object[] toArray() {
    return items.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return items.toArray(a);
  }

  @Override
  public boolean add(JavaFileScannerContext.Location e) {
    return items.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return items.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return items.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends JavaFileScannerContext.Location> c) {
    return items.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends JavaFileScannerContext.Location> c) {
    return items.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return items.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return items.retainAll(c);
  }

  @Override
  public void clear() {
    items.clear();
  }

  @Override
  public JavaFileScannerContext.Location get(int index) {
    return items.get(index);
  }

  @Override
  public JavaFileScannerContext.Location set(int index, JavaFileScannerContext.Location element) {
    return items.set(index, element);
  }

  @Override
  public void add(int index, JavaFileScannerContext.Location element) {
    items.add(index, element);
  }

  @Override
  public JavaFileScannerContext.Location remove(int index) {
    return items.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return items.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return items.lastIndexOf(o);
  }

  @Override
  public ListIterator<JavaFileScannerContext.Location> listIterator() {
    return items.listIterator();
  }

  @Override
  public ListIterator<JavaFileScannerContext.Location> listIterator(int index) {
    return items.listIterator(index);
  }

  @Override
  public List<JavaFileScannerContext.Location> subList(int fromIndex, int toIndex) {
    return items.subList(fromIndex, toIndex);
  }

  public static class Builder {

    private final ImmutableList.Builder<JavaFileScannerContext.Location> builder;
    private boolean exceptional;

    private Builder() {
      this.builder = ImmutableList.builder();
      this.exceptional = false;
    }

    public Builder setAsExceptional() {
      exceptional = true;
      return this;
    }

    public Builder add(JavaFileScannerContext.Location element) {
      builder.add(element);
      return this;
    }

    public Builder addAll(Flow flow) {
      builder.addAll(flow.items);
      this.exceptional |= flow.exceptional;
      return this;
    }

    public Builder addAll(List<JavaFileScannerContext.Location> items) {
      return addAll(new Flow(items, false));
    }

    public Flow build() {
      return new Flow(builder.build(), exceptional);
    }
  }
}

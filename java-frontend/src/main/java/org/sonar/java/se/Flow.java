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
package org.sonar.java.se;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Flow {

  private final List<JavaFileScannerContext.Location> elements;
  private boolean exceptional;

  private Flow(List<JavaFileScannerContext.Location> elements, boolean exceptional) {
    this.elements = elements;
    this.exceptional = exceptional;
  }

  @Override
  public int hashCode() {
    return Objects.hash(elements, exceptional);
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
    return Objects.equals(elements, other.elements)
      && exceptional == other.exceptional;
  }

  public boolean isNonExceptional() {
    return !exceptional;
  }

  public Flow reverse() {
    return new Flow(Lists.reverse(elements), exceptional);
  }

  public Stream<JavaFileScannerContext.Location> stream() {
    return elements.stream();
  }

  public Stream<JavaFileScannerContext.Location> firstFlowLocation() {
    return elements.stream().reduce((a, b) -> b).map(Stream::of).orElseGet(Stream::empty);
  }

  public List<JavaFileScannerContext.Location> elements() {
    // list is unmodifiable by construction, so can be safely returned
    return elements;
  }

  public boolean isEmpty() {
    return elements.isEmpty();
  }

  public static Flow empty() {
    return new Flow(Collections.emptyList(), false);
  }

  public static Flow of(JavaFileScannerContext.Location location) {
    return new Flow(Collections.singletonList(Objects.requireNonNull(location)), false);
  }

  public static Flow of(Flow currentFlow) {
    return new Flow(ImmutableList.copyOf(currentFlow.elements), currentFlow.exceptional);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private final ImmutableList.Builder<JavaFileScannerContext.Location> elementsBuilder;
    private boolean exceptional;

    private Builder() {
      this.elementsBuilder = ImmutableList.builder();
      this.exceptional = false;
    }

    public Builder setAsExceptional() {
      exceptional = true;
      return this;
    }

    public Builder add(JavaFileScannerContext.Location element) {
      elementsBuilder.add(element);
      return this;
    }

    public Builder addAll(Flow flow) {
      elementsBuilder.addAll(flow.elements);
      this.exceptional |= flow.exceptional;
      return this;
    }

    public Flow build() {
      return new Flow(elementsBuilder.build(), exceptional);
    }
  }
}

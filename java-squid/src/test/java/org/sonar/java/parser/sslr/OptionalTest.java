/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.parser.sslr;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

public class OptionalTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final Optional<String> present = Optional.of("foo");
  private final Optional<String> absent = Optional.absent();

  @Test
  public void present() {
    assertThat(present.isPresent()).isTrue();

    assertThat(present.orNull()).isSameAs("foo");

    assertThat(present.or("bar")).isSameAs("foo");

    assertThat(present.get()).isSameAs("foo");

    assertThat(present.toString()).isEqualTo("Optional.of(foo)");

    assertThat(present.equals(present)).isTrue();
    assertThat(present.equals(Optional.of("foo"))).isTrue();
    assertThat(present.equals(Optional.of("bar"))).isFalse();
    assertThat(present.equals(absent)).isFalse();

    assertThat(present.hashCode()).isEqualTo(0x598df91c + "foo".hashCode());
  }

  @Test
  public void absent() {
    assertThat(absent.isPresent()).isFalse();

    assertThat(absent.orNull()).isNull();

    assertThat(absent.or("bar")).isSameAs("bar");

    assertThat(absent.toString()).isEqualTo("Optional.absent()");

    assertThat(absent.equals(present)).isFalse();
    assertThat(absent.equals(absent)).isTrue();

    assertThat(absent.hashCode()).isEqualTo(0x598df91c);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("value is absent");
    absent.get();
  }

  @Test
  public void present_or_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("use orNull() instead of or(null)");
    present.or(null);
  }

  @Test
  public void absent_or_null() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("use orNull() instead of or(null)");
    absent.or(null);
  }

}

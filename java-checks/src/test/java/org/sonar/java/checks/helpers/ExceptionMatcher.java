/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

package org.sonar.java.checks.helpers;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Krzysztof Suszyński <krzysztof.suszynski@wavesoftware.pl>
 * @since 2015-08-23
 */
public class ExceptionMatcher extends TypeSafeMatcher<Exception> {

    private Class<? extends Exception> instanceOf;
    private ExceptionMatcher casueMatcher;
    private Matcher<String> messageMatcher;

    public static ExceptionMatcher createFor(Class<? extends Exception> cls) {
        return new ExceptionMatcher(cls);
    }

    private ExceptionMatcher(Class<? extends Exception> cls) {
        expectInstanceOf(cls);
    }

    @Nonnull
    public ExceptionMatcher expectInstanceOf(@Nonnull Class<? extends Exception> cls) {
        this.instanceOf = checkNotNull(cls);
        return this;
    }

    @Nonnull
    public ExceptionMatcher expectCause(@Nonnull ExceptionMatcher causeExceptionMatcher) {
        this.casueMatcher = checkNotNull(causeExceptionMatcher);
        return this;
    }

    @Override
    public boolean matchesSafely(@Nonnull Exception ex) {
        boolean matches;
        if (instanceOf.isAssignableFrom(ex.getClass())) {
            matches = messageMatcher == null || messageMatcher.matches(ex.getMessage());
            if (casueMatcher != null) {
                matches = matches && casueMatcher.matches(ex.getCause());
            }
        } else {
            matches = false;
        }
        return matches;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(this.instanceOf.getName());
        if (messageMatcher != null) {
            description.appendText(" with message ");
            messageMatcher.describeTo(description);
        }
        if (casueMatcher != null) {
            description.appendText(" with cause ");
            casueMatcher.describeTo(description);
        }
    }



    public ExceptionMatcher expectMessage(String message) {
        return expectMessage(CoreMatchers.equalTo(message));
    }

    public ExceptionMatcher expectMessage(Matcher<String> messageMatcher) {
        this.messageMatcher = messageMatcher;
        return this;
    }
}

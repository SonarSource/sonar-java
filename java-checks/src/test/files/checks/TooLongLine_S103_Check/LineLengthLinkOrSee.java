// Noncompliant@+4 {{Split this 217 characters long line (which is greater than 100 authorized).}}
/**
 * Example of usage:
 * <pre>
 * Noncompliant: long link, but also long text on the same line, longer than the marker line at the top. See {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">HTTP/1.1 documentation</a>}.
 * </pre>
 */

// Noncompliant@+1 {{Split this 211 characters long line (which is greater than 100 authorized).}}
// Long link, but also long text on the same line, longer than the marker line above.............. See {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">HTTP/1.1 documentation</a>}.

// Noncompliant@+6 {{Split this 206 characters long line (which is greater than 100 authorized).}}
// Noncompliant@+6 {{Split this 138 characters long line (which is greater than 100 authorized).}}
// Noncompliant@+6 {{Split this 134 characters long line (which is greater than 100 authorized).}}
/**
 * Example of usage:
 * <pre>
 * Long see, but also long text on the same line, longer than the marker line above.................. @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">HTTP/1.1 documentation</a>
 * Long see, but also long text on the same line, longer than the marker line above.................. @see "The Java Programming Language"
 * Long see, but also long text on the same line, longer than the marker line above.................. @see package.class#member  label
 * </pre>
 */

// Noncompliant@+1 {{Split this 206 characters long line (which is greater than 100 authorized).}}
// Long see, but also long text on the same line, longer than the marker line above.................. @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">HTTP/1.1 documentation</a>
// Noncompliant@+1 {{Split this 138 characters long line (which is greater than 100 authorized).}}
// Long see, but also long text on the same line, longer than the marker line above.................. @see "The Java Programming Language"
// Noncompliant@+1 {{Split this 134 characters long line (which is greater than 100 authorized).}}
// Long see, but also long text on the same line, longer than the marker line above.................. @see package.class#member  label

class LineLength {}

class ClasName {
}

class AbstractLikeName { // Noncompliant {{Make this class abstract or rename it, since it matches the regular expression '^Abstract[A-Z][a-zA-Z0-9]*$'.}}
}

abstract public class BadAbstractClassName { // Noncompliant {{Rename this abstract class name to match the regular expression '^Abstract[A-Z][a-zA-Z0-9]*$'.}}
}

abstract class AbstractClassName {
}

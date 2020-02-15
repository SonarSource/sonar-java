package checks.naming;

interface badInterfaceName { // Noncompliant [[sc=11;ec=27]] {{Rename this interface name to match the regular expression '^[A-Z][a-zA-Z0-9]*$'.}}
}

interface GoodInterfaceName {
}

@interface should_not_be_checked_annotation {
}

class should_not_be_checked_class {
}

enum should_not_be_checked {
}

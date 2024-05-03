package checks;

class OneClassInterfacePerFileCheckNoncompliantA {
}

interface OneClassInterfacePerFileCheckNoncompliantB {
}

enum OneClassInterfacePerFileCheckNoncompliantC {
}

@interface OneClassInterfacePerFileCheckNoncompliantD {
}

// Noncompliant@0 {{There are 4 top-level types in this file; move all but one of them to other files.}}

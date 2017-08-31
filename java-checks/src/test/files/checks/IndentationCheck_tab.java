class IndentationCheck_tab {
	int a;                          // Compliant
		int b;                         // Noncompliant {{Make this line start at column 3.}}
		int c;                           // Compliant - already reported

	void foo() {
		if(a == 0) {
				foo(); // Noncompliant
		}
	}


}

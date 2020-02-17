class IndentationCheck_tab {
	int a;                          // Compliant
		int b;                         // Noncompliant {{Make this line start after 2 spaces to indent the code consistently.}}
		int c;                           // Compliant - already reported

	void foo() {
		if(a == 0) {
				foo(); // Noncompliant
		}
	}


}

package checks;

class IndentationCheck_tab {
	int a;                          // Compliant
		int b;                        // Noncompliant {{Make this line start after 2 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 2.)}}
		int c;                        // Compliant - already reported

	void foo() {
		if(a == 0) {
				foo(); // Noncompliant
		}
	}


}

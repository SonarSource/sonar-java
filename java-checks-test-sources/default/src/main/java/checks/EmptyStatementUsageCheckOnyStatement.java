; // Noncompliant@-1 [[quickfixes=qf_first_statement]]
//^[sc=1;ec=2]

// fix@qf_first_statement {{Remove this empty statement}}
// edit@qf_first_statement [[sc=1;ec=2]] {{}}

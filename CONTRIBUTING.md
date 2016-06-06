# Contributing Guidelines

Contributions are welcome!

**Before spending lots of time on something, ask for feedback on your idea first!**
Reach out to the [Google Group](https://groups.google.com/forum/#!forum/sonarqube) to ask if we would be interested in your contribution!
Please search issues on our [Jira](http://jira.sonarsource.com/browse/SONARJAVA).
 
 To avoid frustration, please discuss before submitting any contribution! 

## Coding recommendation

#### Make your changes minimal !
 The less code is modified the easier it is to review and makes your contribution more likely to be accepted.
 This means that your commits should be atomic and have a single purpose. Formatting modification should not clutter your changes in order to make reviewer job easy.
  
#### Test, test, and test !
 Coding is the easy part, if you want your contribution to be accepted, demonstrates it solves an issue by providing the unit test it solves.
 Your contribution should also not make the [integration tests](#ITs) fails.
  
#### <a name="ITs"></a>Integration tests (ITs) !

  Your contribution should not break the integration tests (that will be run on any opened pull requests)
  To run ITs locally, checkout the [README](https://github.com/SonarSource/sonar-java/blob/master/README.md) of the project.

#### clean commit history

  To ease the review please minimize and have a clean history of commit in your pull request. Your commit should have a single purpose.
  This will help to make your contribution accepted as we like to keep a clean linear history and prefer rebase over merging commits.

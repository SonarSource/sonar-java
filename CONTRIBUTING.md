# Contributing Guidelines

Contributions are welcome!

**Before spending lots of time on something, ask for feedback on your idea first.**
Reach out to the [SonarQube Community Forum](https://community.sonarsource.com/) to ask if we would be interested in your contribution.
Please search issues on our [Jira](http://jira.sonarsource.com/browse/SONARJAVA).
 
To avoid frustration, please discuss before submitting any contributions.

## Coding recommendations

### Make your changes minimal

The less code is modified, the easier to review, and it makes your contribution more likely to be accepted.
This means that your commits should be atomic and have a single purpose. Formatting modifications should not clutter your changes in order to make the reviewer's job easier.
  
### Test, test, and test

Coding is the easy part. If you want your contribution to be accepted, demonstrate it solves an issue by providing the unit test it solves (that would have failed before).
Your contribution should also not make the [integration tests](#ITs) fail.
  
### <a name="ITs"></a>Integration tests (ITs)

Your contribution should not break the integration tests (ITs). Note that ITs are run on any opened pull requests.
To run ITs locally, please follow the [README](https://github.com/SonarSource/sonar-java/blob/master/README.md) of the project.

### Clean commit history

To ease the review, please have a clean, minimal history of commits in your pull request. Your commits should have a single purpose.
This will help to make your contribution accepted as we like to keep a clean linear history and prefer rebase over merging commits.

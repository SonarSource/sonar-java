package checks.spring;

import com.mongodb.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

class OptionalRestParametersShouldBeObjects {

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(@PathVariable(required = false) int articleId, int unused) { // Noncompliant [[sc=29;ec=75]] {{Convert this optional parameter to an Object type.}}
    return new Article(articleId);
  }

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(
    @PathVariable(required = false) float articleId, // Noncompliant [[sc=5;ec=53]] {{Convert this optional parameter to an Object type.}}
    int unused,
    @PathVariable(required = false) boolean anotherIssue // Noncompliant [[sc=5;ec=57]] {{Convert this optional parameter to an Object type.}}
  ) {
    return new Article((int) (Math.floor(articleId)));
  }

  @GetMapping(value = {"/article", "/article/{id}"})
  public Article getArticle(@PathVariable(required = false) Integer articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButImplictlyRequired(@PathVariable int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButExplicitlyRequired(@PathVariable(required = true) int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButSettingADifferentValue(@PathVariable(name = "articleId") int articleId) { // Compliant
    return new Article(articleId);
  }

  @GetMapping(value = {"/article/{id}"})
  public Article getArticleButDifferentlyAnnotated(@Nullable int articleId) { // Compliant
    return new Article(articleId);
  }

  record Article(int id) {
  }
}

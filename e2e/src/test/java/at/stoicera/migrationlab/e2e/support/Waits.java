package at.stoicera.migrationlab.e2e.support;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit waits only, one timeout policy for the whole suite. 10s is generous for a local/CI
 * stand; the poll interval keeps runs fast.
 */
public final class Waits {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final Duration POLL = Duration.ofMillis(200);

  /**
   * Which "UI is idle" strategy {@link #idle()} uses, per target UI. Comes from the selector map
   * (selectors/&lt;target&gt;.properties, key {@code wait.strategy}) because it is a property of
   * the UI under test, not of the scenario: the legacy stand serves the AngularJS UI ({@code
   * angularjs}, polls $http.pendingRequests); the stage-5 Angular UI uses {@code angular} — the
   * app-maintained pending-request counter, NOT the classic Testability API (the app is zoneless,
   * isStable observes nothing there). Unknown values throw — fail-loud by design, a silently
   * skipped idle wait would reintroduce the lost-update race of flaky log #3.
   */
  private static final String WAIT_STRATEGY =
      at.stoicera.migrationlab.e2e.selectors.SelectorMap.value("wait.strategy");

  private final WebDriver driver;

  public Waits(WebDriver driver) {
    this.driver = driver;
  }

  private WebDriverWait driverWait() {
    return new WebDriverWait(driver, TIMEOUT, POLL);
  }

  public WebElement visible(By locator) {
    return driverWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
  }

  public WebElement clickable(By locator) {
    return driverWait().until(ExpectedConditions.elementToBeClickable(locator));
  }

  public List<WebElement> allVisible(By locator) {
    return driverWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
  }

  public void gone(By locator) {
    driverWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
  }

  public void urlContains(String fragment) {
    driverWait().until(ExpectedConditions.urlContains(fragment));
  }

  public void textIn(By locator, String expected) {
    driverWait().until(ExpectedConditions.textToBePresentInElementLocated(locator, expected));
  }

  /** Exact text match (textIn is a substring match — too weak for numbers like "0" vs "10"). */
  public void textIs(By locator, String expected) {
    driverWait()
        .until(
            d -> {
              List<WebElement> found = d.findElements(locator);
              return !found.isEmpty() && expected.equals(found.get(0).getText());
            });
  }

  public void countAtLeast(By locator, int minimum) {
    driverWait().until(d -> d.findElements(locator).size() >= minimum);
  }

  public void countIs(By locator, int exact) {
    driverWait().until(d -> d.findElements(locator).size() == exact);
  }

  public void alertAndAccept() {
    driverWait().until(ExpectedConditions.alertIsPresent());
    driver.switchTo().alert().accept();
  }

  /** Waits for a JS alert/confirm, accepts it, returns its text (to pin messages exactly). */
  public String alertTextAndAccept() {
    driverWait().until(ExpectedConditions.alertIsPresent());
    org.openqa.selenium.Alert alert = driver.switchTo().alert();
    String text = alert.getText();
    alert.accept();
    return text;
  }

  /**
   * Explicit wait until the UI under test has no pending HTTP requests. Needed after writes whose
   * success produces NO visible DOM change (the legacy UI gives no save feedback — flaky log #3:
   * navigating away too early aborts the in-flight PUT and silently loses the update), and after
   * async view loads that render into an already-present template.
   *
   * <p>Dispatches on the {@code wait.strategy} key of the selector map: "angularjs" polls
   * AngularJS' $http.pendingRequests; "angular" polls the app-maintained pending-request counter
   * (zoneless app — no Testability); "hybrid" dispatches per current document (stage-5 port
   * window). This method throws on anything unknown instead of guessing, because a wrong strategy
   * times out on every save (angular UI) or silently skips the gate.
   */
  public void idle() {
    switch (WAIT_STRATEGY) {
      case "angularjs":
        angularJsIdle();
        break;
      case "angular":
        angularIdle();
        break;
      case "hybrid":
        hybridIdle();
        break;
      default:
        throw new IllegalStateException(
            "Unknown wait.strategy '"
                + WAIT_STRATEGY
                + "' in the selector map — supported: 'angularjs', 'angular', 'hybrid'. "
                + "Fail-loud by design: weakening or skipping the idle wait is not an option "
                + "(see e2e/README.md, wait strategy).");
    }
  }

  /** AngularJS strategy: no pending $http requests ⇒ last digest has rendered. */
  private void angularJsIdle() {
    driverWait()
        .until(
            d ->
                (Boolean)
                    ((org.openqa.selenium.JavascriptExecutor) d)
                        .executeScript(
                            "return (window.angular !== undefined)"
                                + " && (angular.element(document.body).injector() !== undefined)"
                                + " && (angular.element(document.body).injector().get('$http').pendingRequests.length === 0);"));
  }

  /**
   * Angular (stage 5) strategy: the app is zoneless, so instead of the classic Testability API the
   * UI maintains a pending-request counter with the same semantic as AngularJS'
   * $http.pendingRequests (see modern/frontend/src/app/offene-requests.interceptor.ts). Two
   * hardenings from review session 10: (1) the app creates the marker only at/after bootstrap, so a
   * missing marker is "not idle" — the pre-bootstrap dead window cannot pass vacuously; (2) the
   * counter reaches 0 in the interceptor's finalize, which runs BEFORE the zoneless scheduler
   * flushes the render — the probe therefore confirms counter==0 across a double
   * requestAnimationFrame, i.e. after a paint, so "idle" implies "rendered" again (the guarantee
   * the AngularJS digest gave for free).
   */
  private void angularIdle() {
    driverWait()
        .until(
            d ->
                (Boolean)
                    ((org.openqa.selenium.JavascriptExecutor) d)
                        .executeAsyncScript(
                            "var done = arguments[arguments.length - 1];"
                                + "var zero = function () { return typeof window.werkstattOffeneRequests === 'number'"
                                + " && window.werkstattOffeneRequests === 0; };"
                                + "if (!zero()) { done(false); return; }"
                                + "requestAnimationFrame(function () { requestAnimationFrame(function () {"
                                + " done(zero()); }); });"));
  }

  /**
   * Hybrid strategy for the stage-5 Strangler-Fig window, when one flow can cross between the
   * Angular UI and the AngularJS UI: dispatches on whichever framework marker the CURRENT document
   * carries (same render-flush hardening as {@link #angularIdle()} on the Angular branch). Neither
   * marker present = not idle (e.g. mid page-swap) — the wait polls on, fail-loud on timeout.
   */
  private void hybridIdle() {
    driverWait()
        .until(
            d ->
                (Boolean)
                    ((org.openqa.selenium.JavascriptExecutor) d)
                        .executeAsyncScript(
                            "var done = arguments[arguments.length - 1];"
                                + "if (typeof window.werkstattOffeneRequests === 'number') {"
                                + " if (window.werkstattOffeneRequests !== 0) { done(false); return; }"
                                + " requestAnimationFrame(function () { requestAnimationFrame(function () {"
                                + "  done(window.werkstattOffeneRequests === 0); }); });"
                                + " return; }"
                                + "if (window.angular !== undefined"
                                + " && angular.element(document.body).injector() !== undefined) {"
                                + " done(angular.element(document.body).injector().get('$http').pendingRequests.length === 0);"
                                + " return; }"
                                + "done(false);"));
  }

  /** Generic condition on a located element (element must already exist). */
  public void until(By locator, java.util.function.Predicate<WebElement> condition) {
    driverWait()
        .until(
            d -> {
              List<WebElement> found = d.findElements(locator);
              return !found.isEmpty() && condition.test(found.get(0));
            });
  }
}

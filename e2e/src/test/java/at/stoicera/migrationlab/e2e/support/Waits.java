package at.stoicera.migrationlab.e2e.support;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Explicit waits only, one timeout policy for the whole suite.
 * 10s is generous for a local/CI stand; the poll interval keeps runs fast.
 */
public final class Waits {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);
	private static final Duration POLL = Duration.ofMillis(200);

	/**
	 * Which "UI is idle" strategy {@link #idle()} uses, per target UI. Comes from
	 * the selector map (selectors/&lt;target&gt;.properties, key
	 * {@code wait.strategy}) because it is a property of the UI under test, not of
	 * the scenario: today both stands serve the AngularJS UI ({@code angularjs});
	 * the stage-5 Angular UI gets an {@code angular} strategy against the Angular
	 * testability API. Unknown values throw — fail-loud by design, a silently
	 * skipped idle wait would reintroduce the lost-update race of flaky log #3.
	 */
	private static final String WAIT_STRATEGY = at.stoicera.migrationlab.e2e.selectors.SelectorMap.value("wait.strategy");

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
		driverWait().until(d -> {
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
	 * Explicit wait until the UI under test has no pending HTTP requests. Needed
	 * after writes whose success produces NO visible DOM change (the legacy UI
	 * gives no save feedback — flaky log #3: navigating away too early aborts the
	 * in-flight PUT and silently loses the update), and after async view loads
	 * that render into an already-present template.
	 *
	 * Dispatches on the {@code wait.strategy} key of the selector map:
	 * "angularjs" polls AngularJS' $http.pendingRequests. The stage-5 Angular UI
	 * adds an "angular" strategy against the Angular testability API — this
	 * method throws on anything unknown instead of guessing, because a wrong
	 * strategy times out on every save (angular UI) or silently skips the gate.
	 */
	public void idle() {
		switch (WAIT_STRATEGY) {
			case "angularjs":
				angularJsIdle();
				break;
			default:
				throw new IllegalStateException("Unknown wait.strategy '" + WAIT_STRATEGY
						+ "' in the selector map — supported: 'angularjs' (stage 5 adds 'angular'). "
						+ "Fail-loud by design: weakening or skipping the idle wait is not an option "
						+ "(see e2e/README.md, wait strategy).");
		}
	}

	/** AngularJS strategy: no pending $http requests ⇒ last digest has rendered. */
	private void angularJsIdle() {
		driverWait().until(d -> (Boolean) ((org.openqa.selenium.JavascriptExecutor) d).executeScript(
				"return (window.angular !== undefined)"
						+ " && (angular.element(document.body).injector() !== undefined)"
						+ " && (angular.element(document.body).injector().get('$http').pendingRequests.length === 0);"));
	}

	/** Generic condition on a located element (element must already exist). */
	public void until(By locator, java.util.function.Predicate<WebElement> condition) {
		driverWait().until(d -> {
			List<WebElement> found = d.findElements(locator);
			return !found.isEmpty() && condition.test(found.get(0));
		});
	}
}

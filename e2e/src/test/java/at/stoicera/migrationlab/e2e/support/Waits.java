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

	/** Generic condition on a located element (element must already exist). */
	public void until(By locator, java.util.function.Predicate<WebElement> condition) {
		driverWait().until(d -> {
			List<WebElement> found = d.findElements(locator);
			return !found.isEmpty() && condition.test(found.get(0));
		});
	}
}

package at.stoicera.migrationlab.e2e.selectors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openqa.selenium.By;

import at.stoicera.migrationlab.e2e.config.TestConfig;

/**
 * The abstraction that lets one scenario run against both UIs: page objects
 * address elements by INTENT KEY only; the mapping to a concrete CSS selector
 * lives in selectors/&lt;target&gt;.properties. Porting the suite to the new UI
 * (stage 5) means writing a new properties file, not new tests.
 */
public final class SelectorMap {

	private static final Properties SELECTORS = load();

	private SelectorMap() {
	}

	private static Properties load() {
		String resource = "/selectors/" + TestConfig.target() + ".properties";
		try (InputStream in = SelectorMap.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IllegalStateException("Selector map not found: " + resource);
			}
			Properties p = new Properties();
			p.load(in);
			return p;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read selector map " + resource, e);
		}
	}

	/** CSS selector for an intent key; fails fast when the map has a hole. */
	public static By css(String key) {
		String value = SELECTORS.getProperty(key);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					"Selector key '" + key + "' missing in map for target '" + TestConfig.target() + "'");
		}
		return By.cssSelector(value);
	}
}

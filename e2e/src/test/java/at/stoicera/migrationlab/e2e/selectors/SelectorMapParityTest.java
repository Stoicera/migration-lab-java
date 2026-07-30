package at.stoicera.migrationlab.e2e.selectors;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The selector maps are the ONLY thing that differs between the two UIs — so
 * their key sets must be identical, always. Stage 1–4 kept this by copy;
 * stage 5 edits modern.properties key by key while the Angular UI grows, and
 * this guard turns a forgotten/mistyped key into a red build instead of a
 * "selector key missing" failure halfway through a scenario (or worse, a green
 * run that silently tested the wrong UI). Target-independent by construction:
 * it loads both files directly, not via {@link SelectorMap}.
 */
class SelectorMapParityTest {

	@Test
	@DisplayName("legacy.properties und modern.properties: identische Key-Sets")
	void keySetsAreIdentical() throws IOException {
		Properties legacy = load("/selectors/legacy.properties");
		Properties modern = load("/selectors/modern.properties");
		assertThat(modern.keySet())
				.as("every intent key exists in both maps (values may differ)")
				.containsExactlyInAnyOrderElementsOf(legacy.keySet());
	}

	private Properties load(String resource) throws IOException {
		try (InputStream in = getClass().getResourceAsStream(resource)) {
			assertThat(in).as("selector map missing: " + resource).isNotNull();
			Properties p = new Properties();
			p.load(in);
			return p;
		}
	}
}

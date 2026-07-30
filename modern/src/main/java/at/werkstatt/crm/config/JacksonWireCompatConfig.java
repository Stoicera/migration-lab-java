package at.werkstatt.crm.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Wire-format compatibility with the legacy stand (Boot 1.5 / Jackson 2.8).
 *
 * Jackson >= 2.9 serializes java.sql.Date as an epoch timestamp when
 * write-dates-as-timestamps is on; Jackson 2.8 always wrote "yyyy-MM-dd".
 * Our only DATE column (fahrzeug.pickerl_datum) changed shape in the 1.5->2.7
 * jump — caught by the characterization suite, invisible in the UI
 * (playbook ch. 3, break #3).
 *
 * Boot 4 note (playbook ch. 4): this shim was the ONLY compile break of the
 * 3.5->4.1 leg — Boot 4 moved to Jackson 3 (tools.jackson), which replaced
 * the Jackson2ObjectMapperBuilderCustomizer with this JsonMapper variant.
 * The API contract itself is unchanged and stays pinned until a stage
 * deliberately changes it (with ADR + golden update).
 */
@Configuration
public class JacksonWireCompatConfig {

	@Bean
	public JsonMapperBuilderCustomizer legacySqlDateShape() {
		return builder -> builder.withConfigOverride(java.sql.Date.class,
				override -> override.setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd")));
	}
}

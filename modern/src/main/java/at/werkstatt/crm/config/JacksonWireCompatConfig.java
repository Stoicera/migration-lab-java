package at.werkstatt.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Wire-format compatibility with the legacy stand (Boot 1.5 / Jackson 2.8).
 *
 * Jackson >= 2.9 serializes java.sql.Date as an epoch timestamp when
 * WRITE_DATES_AS_TIMESTAMPS is on; Jackson 2.8 always wrote its toString()
 * ("yyyy-MM-dd"). Our only DATE column (fahrzeug.pickerl_datum) therefore
 * changed its JSON shape in the 1.5->2.7 jump — caught by the characterization
 * suite, invisible in the UI (playbook ch. 3, break #3).
 *
 * The API contract is the contract: this override pins the legacy shape until
 * a stage deliberately changes the API (with ADR + golden update).
 */
@Configuration
public class JacksonWireCompatConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer legacySqlDateShape() {
		return builder -> builder.postConfigurer(mapper -> mapper
				.configOverride(java.sql.Date.class)
				.setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd")));
	}
}

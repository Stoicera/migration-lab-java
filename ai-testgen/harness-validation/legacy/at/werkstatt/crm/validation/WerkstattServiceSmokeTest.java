package at.werkstatt.crm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Harness validation, stratum shape S1 (God service: field injection + JdbcTemplate + @Value).
 * PROTOCOL.md §5 — this test exists to prove the PIPELINE works (compile → run → JaCoCo → PIT), not
 * to test the application. It is written by a human, is excluded from every experiment metric, and
 * must never be confused with generated material.
 *
 * <p>Note for the corpus A/corpus B comparison: getting an instance of this class requires @InjectMocks
 * field injection plus ReflectionTestUtils for the @Value field. The corpus B counterpart needs
 * neither — that difference is the experiment's subject, not an accident.
 */
@ExtendWith(MockitoExtension.class)
class WerkstattServiceSmokeTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private WerkstattService service;

  @Test
  void getAlleKunden_reicht_die_gemappten_zeilen_durch() {
    Kunde moser = new Kunde();
    moser.setNachname("Moser");
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Kunde>>any()))
        .thenReturn(Collections.singletonList(moser));

    List<Kunde> kunden = service.getAlleKunden();

    assertThat(kunden).extracting(Kunde::getNachname).containsExactly("Moser");
  }

  @Test
  void getKunde_liefert_null_wenn_die_abfrage_leer_bleibt() {
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Kunde>>any()))
        .thenReturn(Collections.<Kunde>emptyList());

    assertThat(service.getKunde(4711L)).isNull();
  }

  @Test
  void der_value_satz_ist_ohne_spring_kontext_nur_per_reflection_setzbar() {
    ReflectionTestUtils.setField(service, "ustSatz", 20);

    assertThat(ReflectionTestUtils.getField(service, "ustSatz")).isEqualTo(20);
  }
}

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Harness validation, stratum shape S1 on the MIGRATED counterpart (corpus B). PROTOCOL.md §5 — proves
 * the pipeline, not the application; excluded from every experiment metric.
 *
 * <p>The corpus A twin of this file needs @InjectMocks plus ReflectionTestUtils to get an instance.
 * Here the constructor does it — same behaviour under test, different testability. That delta is
 * what corpus B measures.
 */
@ExtendWith(MockitoExtension.class)
class WerkstattServiceSmokeTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void getAlleKunden_reicht_die_gemappten_zeilen_durch() {
    WerkstattService service = new WerkstattService(jdbcTemplate, 20);
    Kunde moser = new Kunde();
    moser.setNachname("Moser");
    when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Kunde>>any()))
        .thenReturn(Collections.singletonList(moser));

    List<Kunde> kunden = service.getAlleKunden();

    assertThat(kunden).extracting(Kunde::getNachname).containsExactly("Moser");
  }

  /**
   * Note the different JdbcTemplate overload compared to the corpus A twin: stage 4 parameterized this
   * query when it closed the B4 injection (ADR-0004, SD-1), so the modern code calls {@code
   * query(sql, rowMapper, args...)} where the legacy code concatenates and calls {@code query(sql,
   * rowMapper)}. The corpora therefore differ in more than the injection style — pre-declared as threat
   * T7 in PROTOCOL.md §8 so no reader mistakes the corpus A/B delta for an injection effect alone.
   */
  @Test
  void getKunde_liefert_null_wenn_die_abfrage_leer_bleibt() {
    WerkstattService service = new WerkstattService(jdbcTemplate, 20);
    when(jdbcTemplate.query(
            anyString(), ArgumentMatchers.<RowMapper<Kunde>>any(), ArgumentMatchers.<Object>any()))
        .thenReturn(Collections.<Kunde>emptyList());

    assertThat(service.getKunde(4711L)).isNull();
  }
}

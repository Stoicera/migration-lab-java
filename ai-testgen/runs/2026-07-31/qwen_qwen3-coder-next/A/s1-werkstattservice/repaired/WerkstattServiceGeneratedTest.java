package at.werkstatt.crm.gen;

import at.werkstatt.crm.model.*;
import at.werkstatt.crm.service.WerkstattService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WerkstattServiceGeneratedTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private WerkstattService service;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @BeforeEach
    void setUp() {
        service = new WerkstattService();
        // Inject mock via reflection as constructor injection is not available
        org.springframework.test.util.ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "ustSatz", 20);
    }

    // --- KUNDE ---

    @Test
    void getAlleKunden_returnsAllKunden() {
        // Arrange
        List<Kunde> expected = List.of(createKunde(1L, "Müller", "Hans"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        List<Kunde> result = service.getAlleKunden();

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("SELECT * FROM kunde ORDER BY nachname, vorname");
    }

    @Test
    void sucheKunden_includesAllFields() {
        // Arrange
        List<Kunde> expected = List.of(createKunde(1L, "Schmidt", "Anna"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        List<Kunde> result = service.sucheKunden("schmidt");

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("lower(nachname) LIKE '%schmidt%'");
        assertThat(sql).contains("lower(vorname) LIKE '%schmidt%'");
        assertThat(sql).contains("lower(ort) LIKE '%schmidt%'");
    }

    @Test
    void getKunde_returnsKundeIfExists() {
        // Arrange
        Kunde expected = createKunde(1L, "Mayer", "Klaus");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(expected));

        // Act
        Kunde result = service.getKunde(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getKunde_returnsNullIfNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        Kunde result = service.getKunde(999L);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void speichereKunde_insertsNewKunde() {
        // Arrange
        Kunde kunde = new Kunde();
        kunde.setAnrede("Herr");
        kunde.setVorname("Max");
        kunde.setNachname("Mustermann");
        kunde.setTelefon("0664/1234567");
        kunde.setEmail("max@example.com");
        kunde.setStrasse("Hauptstr. 1");
        kunde.setPlz("1010");
        kunde.setOrt("Wien");
        kunde.setNotiz("Test");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(123L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(kunde)); // getKunde after insert

        // Act
        Kunde result = service.speichereKunde(kunde);

        // Assert
        assertThat(result.getId()).isEqualTo(123L);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("INSERT INTO kunde");
        assertThat(sql).contains("RETURNING id");
    }

    @Test
    void speichereKunde_updatesExistingKunde() {
        // Arrange
        Kunde kunde = new Kunde();
        kunde.setId(1L);
        kunde.setAnrede("Frau");
        kunde.setVorname("Erika");
        kunde.setNachname("Musterfrau");
        kunde.setTelefon("0664/9876543");
        kunde.setEmail("erika@example.com");
        kunde.setStrasse("Nebenstr. 2");
        kunde.setPlz("1020");
        kunde.setOrt("Wien");
        kunde.setNotiz("Update");

        // Mock getKunde to return updated kunde
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(kunde));

        // Act
        Kunde result = service.speichereKunde(kunde);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("UPDATE kunde SET");
        assertThat(sql).contains("WHERE id=?");
    }

    @Test
    void loescheKunde_deletesKunde() {
        // Act
        service.loescheKunde(1L);

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("DELETE FROM kunde WHERE id = 1");
    }

    // --- FAHRZEUG ---

    @Test
    void getAlleFahrzeuge_returnsAllFahrzeugeWithKundeName() throws SQLException {
        // Arrange
        List<Fahrzeug> expected = new ArrayList<>();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getLong("kunde_id")).thenReturn(10L);
        when(rs.getString("kennzeichen")).thenReturn("W-AB 123");
        when(rs.getString("marke")).thenReturn("BMW");
        when(rs.getString("modell")).thenReturn("X1");
        when(rs.getString("fahrgestellnr")).thenReturn("WBA123456");
        when(rs.getInt("baujahr")).thenReturn(2020);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getInt("km_stand")).thenReturn(15000);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getDate("pickerl_datum")).thenReturn(new java.sql.Date(0));
        when(rs.getTimestamp("angelegt_am")).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(rs.getString("nachname")).thenReturn("Müller");
        when(rs.getString("vorname")).thenReturn("Hans");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Fahrzeug> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        // Act
        List<Fahrzeug> result = service.getAlleFahrzeuge();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKundeName()).isEqualTo("Müller Hans");
    }

    @Test
    void getFahrzeugeZuKunde_returnsFahrzeugeForKunde() {
        // Arrange
        List<Fahrzeug> expected = List.of(createFahrzeug(1L, 10L, "W-AB 123"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        List<Fahrzeug> result = service.getFahrzeugeZuKunde(10L);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("WHERE kunde_id = 10");
    }

    @Test
    void getFahrzeug_returnsFahrzeugIfExists() {
        // Arrange
        Fahrzeug expected = createFahrzeug(1L, 10L, "W-AB 123");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(expected));

        // Act
        Fahrzeug result = service.getFahrzeug(1L);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getFahrzeug_returnsNullIfNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        Fahrzeug result = service.getFahrzeug(999L);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void speichereFahrzeug_insertsNewFahrzeug() {
        // Arrange
        Fahrzeug fahrzeug = createFahrzeug(null, 10L, "W-AB 123");
        fahrzeug.setMarke("BMW");
        fahrzeug.setModell("X1");
        fahrzeug.setFahrgestellnr("WBA123");
        fahrzeug.setBaujahr(2020);
        fahrzeug.setKmStand(15000);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(123L);

        // Act
        service.speichereFahrzeug(fahrzeug);

        // Assert
        assertThat(fahrzeug.getId()).isEqualTo(123L);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("INSERT INTO fahrzeug");
        assertThat(sql).contains("RETURNING id");
    }

    @Test
    void speichereFahrzeug_updatesExistingFahrzeug() {
        // Arrange
        Fahrzeug fahrzeug = createFahrzeug(1L, 10L, "W-AB 123");
        fahrzeug.setMarke("BMW");
        fahrzeug.setModell("X1");
        fahrzeug.setFahrgestellnr("WBA123");
        fahrzeug.setBaujahr(2020);
        fahrzeug.setKmStand(15000);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(fahrzeug));

        // Act
        service.speichereFahrzeug(fahrzeug);

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("UPDATE fahrzeug SET");
        assertThat(sql).contains("WHERE id=?");
    }

    @Test
    void loescheFahrzeug_deletesFahrzeug() {
        // Act
        service.loescheFahrzeug(1L);

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("DELETE FROM fahrzeug WHERE id = 1");
    }

    // --- AUFTRAG ---

    @Test
    void getAuftraege_returnsAllAuftraege() {
        // Arrange
        List<Auftrag> expected = List.of(createAuftrag(1L, 10L, 20L, "ANGENOMMEN"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        List<Auftrag> result = service.getAuftraege(null);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("FROM auftrag a LEFT JOIN kunde k");
        assertThat(sql).contains("ORDER BY a.angenommen_am DESC");
    }

    @Test
    void getAuftraege_filtersByStatus() {
        // Arrange
        List<Auftrag> expected = List.of(createAuftrag(1L, 10L, 20L, "IN_ARBEIT"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        service.getAuftraege("IN_ARBEIT");

        // Assert
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("WHERE a.status = 'IN_ARBEIT'");
    }

    @Test
    void getAuftrag_returnsAuftragWithPositionen() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "FERTIG");
        List<AuftragPosition> positions = List.of(createPosition(1L, 1L, "ARBEIT", "Reparatur", 2.0, 100.0));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag))
                .thenReturn(positions);

        // Act
        Auftrag result = service.getAuftrag(1L);

        // Assert
        assertThat(result).isEqualTo(auftrag);
        assertThat(result.getPositionen()).isEqualTo(positions);
    }

    @Test
    void getAuftrag_returnsNullIfNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        Auftrag result = service.getAuftrag(999L);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void neuerAuftrag_createsNewAuftragWithNumber() {
        // Arrange
        Auftrag auftrag = createAuftrag(null, 10L, 20L, null);
        auftrag.setBeschreibung("Reparatur");
        auftrag.setKmStand(15000);

        Calendar cal = Calendar.getInstance();
        int jahr = cal.get(Calendar.YEAR);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(42); // max number
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(123L); // new id
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act
        Auftrag result = service.neuerAuftrag(auftrag);

        // Assert
        assertThat(result.getAuftragNr()).isEqualTo("A-" + jahr + "-0043");
        assertThat(result.getStatus()).isEqualTo(Auftrag.STATUS_ANGENOMMEN);
        assertThat(result.getId()).isEqualTo(123L);
    }

    @Test
    void setzeStatus_throwsExceptionIfStatusChangeNotPermitted() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "ANGENOMMEN");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act & Assert
        assertThatThrownBy(() -> service.setzeStatus(1L, "ABGEHOLT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nicht erlaubt");
    }

    @Test
    void setzeStatus_allowsValidStatusTransitions() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "ANGENOMMEN");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act
        service.setzeStatus(1L, "IN_ARBEIT");

        // Assert — the new status is written to the DB; the returned Auftrag is re-read via the mock
        verify(jdbcTemplate).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("status = 'IN_ARBEIT'");
    }

    @Test
    void setzeStatus_allowsBackToArbeitFromFertig() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "FERTIG");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act
        service.setzeStatus(1L, "IN_ARBEIT");

        // Assert — the new status is written to the DB; the returned Auftrag is re-read via the mock
        verify(jdbcTemplate).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("status = 'IN_ARBEIT'");
    }

    @Test
    void setzeStatus_setsTimestampsForStatusChanges() {
        // Arrange — FERTIG is only reachable from IN_ARBEIT
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "IN_ARBEIT");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act
        service.setzeStatus(1L, "FERTIG");

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("fertig_am = now()");
    }

    @Test
    void neuePosition_throwsExceptionIfAuftragNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> service.neuePosition(999L, new AuftragPosition()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nicht gefunden");
    }

    @Test
    void neuePosition_throwsExceptionIfAuftragIsAbgeschlossen() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "ABGEHOLT");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act & Assert
        assertThatThrownBy(() -> service.neuePosition(1L, new AuftragPosition()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("abgeschlossen");
    }

    @Test
    void neuePosition_insertsPosition() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "IN_ARBEIT");
        AuftragPosition position = new AuftragPosition();
        position.setTyp("MATERIAL");
        position.setBezeichnung("Bremsbelag");
        position.setMenge(2.0);
        position.setEinzelpreis(50.0);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(123L);

        // Act
        AuftragPosition result = service.neuePosition(1L, position);

        // Assert
        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getAuftragId()).isEqualTo(1L);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("INSERT INTO auftrag_position");
        assertThat(sql).contains("RETURNING id");
    }

    @Test
    void loeschePosition_deletesPosition() {
        // Act
        service.loeschePosition(1L);

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("DELETE FROM auftrag_position WHERE id = 1");
    }

    // --- RECHNUNG ---

    @Test
    void erstelleRechnung_throwsExceptionIfAuftragNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> service.erstelleRechnung(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nicht gefunden");
    }

    @Test
    void erstelleRechnung_throwsExceptionIfAuftragNotFertig() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "IN_ARBEIT");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag));

        // Act & Assert
        assertThatThrownBy(() -> service.erstelleRechnung(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nur bei Status FERTIG");
    }

    @Test
    void erstelleRechnung_throwsExceptionIfRechnungAlreadyExists() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "FERTIG");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag))
                .thenReturn(Collections.emptyList());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1); // count > 0

        // Act & Assert
        assertThatThrownBy(() -> service.erstelleRechnung(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("gibt es schon eine Rechnung");
    }

    @Test
    void erstelleRechnung_createsRechnungWithCorrectAmounts() {
        // Arrange
        Auftrag auftrag = createAuftrag(1L, 10L, 20L, "FERTIG");
        List<AuftragPosition> positions = List.of(
                createPosition(1L, 1L, "ARBEIT", "Reparatur", 2.0, 100.0)
        );
        auftrag.setPositionen(positions);

        Calendar cal = Calendar.getInstance();
        int jahr = cal.get(Calendar.YEAR);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(auftrag)) // getAuftrag
                .thenReturn(positions) // getPositionen
                .thenReturn(Collections.emptyList()); // getRechnung after insert
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM rechnung"), eq(Integer.class)))
                .thenReturn(0); // no existing rechnung
        when(jdbcTemplate.queryForObject(contains("MAX("), eq(Integer.class)))
                .thenReturn(42); // max rechnung nr
        List<Object[]> insertArgs = new ArrayList<>();
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    insertArgs.add(inv.getArguments());
                    return 123L; // new rechnung id
                });

        // Act
        service.erstelleRechnung(1L);

        // Assert — the computed amounts are observable as the INSERT arguments
        Object[] werte = insertArgs.get(0);
        assertThat((String) werte[0]).contains("INSERT INTO rechnung");
        assertThat(werte[2]).isEqualTo("R-" + jahr + "-0043");
        assertThat(werte[4]).isEqualTo(200.0);
        assertThat(werte[5]).isEqualTo(40.0);
        assertThat(werte[6]).isEqualTo(240.0);
    }

    @Test
    void setzeBezahlt_setsRechnungAsPaid() {
        // Arrange
        Rechnung rechnung = new Rechnung();
        rechnung.setId(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(rechnung));

        // Act
        service.setzeBezahlt(1L);

        // Assert
        verify(jdbcTemplate).update(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("bezahlt = true");
        assertThat(sql).contains("bezahlt_am = now()");
    }

    // --- BERICHT ---

    @Test
    void getMonatsBericht_returnsBerichtForYear() throws SQLException {
        // Arrange
        List<MonatsBericht> expected = new ArrayList<>();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("monat")).thenReturn(1);
        when(rs.getInt("anzahl_auftraege")).thenReturn(5);
        when(rs.getInt("anzahl_rechnungen")).thenReturn(3);
        when(rs.getDouble("umsatz_netto")).thenReturn(1000.0);
        when(rs.getDouble("umsatz_brutto")).thenReturn(1200.0);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<MonatsBericht> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        // Act
        List<MonatsBericht> result = service.getMonatsBericht(2023);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJahr()).isEqualTo(2023);
        assertThat(result.get(0).getMonat()).isEqualTo(1);
        assertThat(result.get(0).getAnzahlAuftraege()).isEqualTo(5);
        assertThat(result.get(0).getAnzahlRechnungen()).isEqualTo(3);
        assertThat(result.get(0).getUmsatzNetto()).isEqualTo(1000.0);
        assertThat(result.get(0).getUmsatzBrutto()).isEqualTo(1200.0);
    }

    @Test
    void getTopKunden_returnsTopKunden() {
        // Arrange
        List<Map<String, Object>> expected = List.of(Map.of(
                "id", 1L,
                "nachname", "Müller",
                "vorname", "Hans",
                "anzahl_auftraege", 5,
                "umsatz", 1200.0
        ));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(expected);

        // Act
        List<Map<String, Object>> result = service.getTopKunden(2023);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    // --- ADMIN ---

    @Test
    void getAdminStatistik_returnsStatistics() {
        // Arrange
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kunde", Integer.class)).thenReturn(10);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fahrzeug", Integer.class)).thenReturn(20);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auftrag", Integer.class)).thenReturn(30);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM auftrag WHERE status IN ('ANGENOMMEN','IN_ARBEIT')", Integer.class)).thenReturn(5);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rechnung", Integer.class)).thenReturn(15);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rechnung WHERE bezahlt = false", Integer.class)).thenReturn(3);

        // Act
        Map<String, Object> result = service.getAdminStatistik();

        // Assert
        assertThat(result).hasSize(7);
        assertThat(result.get("kunden")).isEqualTo(10);
        assertThat(result.get("fahrzeuge")).isEqualTo(20);
        assertThat(result.get("auftraege")).isEqualTo(30);
        assertThat(result.get("auftraegeOffen")).isEqualTo(5);
        assertThat(result.get("rechnungen")).isEqualTo(15);
        assertThat(result.get("rechnungenOffen")).isEqualTo(3);
        assertThat(result.get("stand")).isNotNull();
    }

    @Test
    void bereinigeStornierte_deletesOldStornierteAuftraege() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(ids);

        // Act
        int deleted = service.bereinigeStornierte();

        // Assert
        assertThat(deleted).isEqualTo(2);
        verify(jdbcTemplate, times(4)).update(sqlCaptor.capture());
        List<String> capturedSqls = sqlCaptor.getAllValues();
        assertThat(capturedSqls).anyMatch(sql -> sql.contains("DELETE FROM auftrag_position"));
        assertThat(capturedSqls).anyMatch(sql -> sql.contains("DELETE FROM auftrag WHERE id ="));
    }

    // --- HELPERS ---

    private Kunde createKunde(Long id, String nachname, String vorname) {
        Kunde k = new Kunde();
        k.setId(id);
        k.setNachname(nachname);
        k.setVorname(vorname);
        return k;
    }

    private Fahrzeug createFahrzeug(Long id, Long kundeId, String kennzeichen) {
        Fahrzeug f = new Fahrzeug();
        f.setId(id);
        f.setKundeId(kundeId);
        f.setKennzeichen(kennzeichen);
        return f;
    }

    private Auftrag createAuftrag(Long id, Long kundeId, Long fahrzeugId, String status) {
        Auftrag a = new Auftrag();
        a.setId(id);
        a.setKundeId(kundeId);
        a.setFahrzeugId(fahrzeugId);
        a.setStatus(status);
        return a;
    }

    private AuftragPosition createPosition(Long id, Long auftragId, String typ, String bezeichnung, double menge, double einzelpreis) {
        AuftragPosition p = new AuftragPosition();
        p.setId(id);
        p.setAuftragId(auftragId);
        p.setTyp(typ);
        p.setBezeichnung(bezeichnung);
        p.setMenge(menge);
        p.setEinzelpreis(einzelpreis);
        return p;
    }
}

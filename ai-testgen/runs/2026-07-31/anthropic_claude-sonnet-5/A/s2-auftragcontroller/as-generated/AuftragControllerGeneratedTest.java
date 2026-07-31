package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import at.werkstatt.crm.controller.AuftragController;
import at.werkstatt.crm.model.Auftrag;
import at.werkstatt.crm.model.AuftragPosition;
import at.werkstatt.crm.service.WerkstattService;

@ExtendWith(MockitoExtension.class)
class AuftragControllerGeneratedTest {

	@Mock
	private WerkstattService werkstattService;

	private AuftragController controller;

	@BeforeEach
	void setUp() {
		controller = new AuftragController();
		ReflectionTestUtils.setField(controller, "werkstattService", werkstattService);
	}

	// ---------- liste ----------

	@Test
	void liste_gibtListeVomServiceZurueck() {
		Auftrag a1 = new Auftrag();
		Auftrag a2 = new Auftrag();
		List<Auftrag> erwartet = Arrays.asList(a1, a2);
		when(werkstattService.getAuftraege("IN_ARBEIT")).thenReturn(erwartet);

		List<Auftrag> ergebnis = controller.liste("IN_ARBEIT");

		assertThat(ergebnis).isSameAs(erwartet);
		verify(werkstattService).getAuftraege("IN_ARBEIT");
	}

	@Test
	void liste_mitNullStatus_wirdDurchgereicht() {
		when(werkstattService.getAuftraege(isNull())).thenReturn(Collections.emptyList());

		List<Auftrag> ergebnis = controller.liste(null);

		assertThat(ergebnis).isEmpty();
		verify(werkstattService).getAuftraege(isNull());
	}

	// ---------- einzeln ----------

	@Test
	void einzeln_gefunden_gibtOkMitAuftragZurueck() {
		Auftrag auftrag = new Auftrag();
		auftrag.setId(5L);
		when(werkstattService.getAuftrag(5L)).thenReturn(auftrag);

		ResponseEntity<Auftrag> response = controller.einzeln(5L);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(auftrag);
	}

	@Test
	void einzeln_nichtGefunden_gibtNotFoundZurueck() {
		when(werkstattService.getAuftrag(99L)).thenReturn(null);

		ResponseEntity<Auftrag> response = controller.einzeln(99L);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
	}

	// ---------- anlegen ----------

	@Test
	void anlegen_erfolgreich_gibtOkMitNeuemAuftragZurueck() {
		Auftrag eingabe = new Auftrag();
		Auftrag gespeichert = new Auftrag();
		gespeichert.setId(1L);
		when(werkstattService.neuerAuftrag(eingabe)).thenReturn(gespeichert);

		ResponseEntity<?> response = controller.anlegen(eingabe);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(gespeichert);
	}

	@Test
	void anlegen_serviceWirftException_gibt500MitFehlermeldungZurueck() {
		Auftrag eingabe = new Auftrag();
		when(werkstattService.neuerAuftrag(eingabe)).thenThrow(new RuntimeException("Fehler beim Anlegen"));

		ResponseEntity<?> response = controller.anlegen(eingabe);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Fehler beim Anlegen");
	}

	// ---------- status ----------

	@Test
	void status_erfolgreich_gibtOkMitAktualisiertemAuftragZurueck() {
		Auftrag aktualisiert = new Auftrag();
		aktualisiert.setStatus(Auftrag.STATUS_FERTIG);
		when(werkstattService.setzeStatus(3L, Auftrag.STATUS_FERTIG)).thenReturn(aktualisiert);

		ResponseEntity<?> response = controller.status(3L, Auftrag.STATUS_FERTIG);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(aktualisiert);
	}

	@Test
	void status_serviceWirftException_gibt500MitFehlermeldungZurueck() {
		when(werkstattService.setzeStatus(anyLong(), anyString()))
				.thenThrow(new RuntimeException("Ungueltiger Status"));

		ResponseEntity<?> response = controller.status(3L, "UNGUELTIG");

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Ungueltiger Status");
	}

	// ---------- position ----------

	@Test
	void position_erfolgreich_gibtOkMitNeuerPositionZurueck() {
		AuftragPosition eingabe = new AuftragPosition();
		AuftragPosition gespeichert = new AuftragPosition();
		gespeichert.setId(7L);
		when(werkstattService.neuePosition(eq(2L), eq(eingabe))).thenReturn(gespeichert);

		ResponseEntity<?> response = controller.position(2L, eingabe);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isSameAs(gespeichert);
	}

	@Test
	void position_serviceWirftException_gibt500MitFehlermeldungZurueck() {
		AuftragPosition eingabe = new AuftragPosition();
		when(werkstattService.neuePosition(anyLong(), any(AuftragPosition.class)))
				.thenThrow(new RuntimeException("Auftrag nicht gefunden"));

		ResponseEntity<?> response = controller.position(2L, eingabe);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Auftrag nicht gefunden");
	}

	// ---------- positionLoeschen ----------

	@Test
	void positionLoeschen_erfolgreich_gibtOkOhneBodyZurueck() {
		ResponseEntity<?> response = controller.positionLoeschen(4L);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNull();
		verify(werkstattService, times(1)).loeschePosition(4L);
	}

	@Test
	void positionLoeschen_serviceWirftException_gibt500MitFehlermeldungZurueck() {
		doThrow(new RuntimeException("Position nicht gefunden")).when(werkstattService).loeschePosition(4L);

		ResponseEntity<?> response = controller.positionLoeschen(4L);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Position nicht gefunden");
	}
}

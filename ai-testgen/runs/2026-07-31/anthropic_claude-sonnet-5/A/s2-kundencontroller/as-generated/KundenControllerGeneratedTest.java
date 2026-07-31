package at.werkstatt.crm.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import at.werkstatt.crm.controller.KundenController;
import at.werkstatt.crm.model.Fahrzeug;
import at.werkstatt.crm.model.Kunde;
import at.werkstatt.crm.service.WerkstattService;

@ExtendWith(MockitoExtension.class)
class KundenControllerGeneratedTest {

	@Mock
	private WerkstattService werkstattService;

	private KundenController kundenController;

	@BeforeEach
	void setUp() {
		kundenController = new KundenController();
		ReflectionTestUtils.setField(kundenController, "werkstattService", werkstattService);
	}

	// ---- liste() ----

	@Test
	void liste_ohneSuche_gibtAlleKundenZurueck() {
		Kunde k1 = new Kunde();
		Kunde k2 = new Kunde();
		List<Kunde> alle = Arrays.asList(k1, k2);
		when(werkstattService.getAlleKunden()).thenReturn(alle);

		List<Kunde> result = kundenController.liste(null);

		assertThat(result).isEqualTo(alle);
		verify(werkstattService).getAlleKunden();
		verify(werkstattService, never()).sucheKunden(any());
	}

	@Test
	void liste_mitLeerstring_gibtAlleKundenZurueck() {
		List<Kunde> alle = Collections.singletonList(new Kunde());
		when(werkstattService.getAlleKunden()).thenReturn(alle);

		List<Kunde> result = kundenController.liste("   ");

		assertThat(result).isEqualTo(alle);
		verify(werkstattService).getAlleKunden();
		verify(werkstattService, never()).sucheKunden(any());
	}

	@Test
	void liste_mitLeerstringOhneWhitespace_gibtAlleKundenZurueck() {
		List<Kunde> alle = Collections.singletonList(new Kunde());
		when(werkstattService.getAlleKunden()).thenReturn(alle);

		List<Kunde> result = kundenController.liste("");

		assertThat(result).isEqualTo(alle);
		verify(werkstattService).getAlleKunden();
		verify(werkstattService, never()).sucheKunden(any());
	}

	@Test
	void liste_mitSuchbegriff_suchtKunden() {
		List<Kunde> gefunden = Collections.singletonList(new Kunde());
		when(werkstattService.sucheKunden("Mustermann")).thenReturn(gefunden);

		List<Kunde> result = kundenController.liste("Mustermann");

		assertThat(result).isEqualTo(gefunden);
		verify(werkstattService).sucheKunden("Mustermann");
		verify(werkstattService, never()).getAlleKunden();
	}

	@Test
	void liste_mitSuchbegriffMitLeerzeichen_trimmtVorSuche() {
		List<Kunde> gefunden = Collections.singletonList(new Kunde());
		when(werkstattService.sucheKunden("Mustermann")).thenReturn(gefunden);

		List<Kunde> result = kundenController.liste("  Mustermann  ");

		assertThat(result).isEqualTo(gefunden);
		verify(werkstattService).sucheKunden("Mustermann");
		verify(werkstattService, never()).getAlleKunden();
	}

	// ---- einzeln() ----

	@Test
	void einzeln_kundeGefunden_gibt200MitKundeZurueck() {
		Kunde kunde = new Kunde();
		kunde.setId(5L);
		when(werkstattService.getKunde(5L)).thenReturn(kunde);

		ResponseEntity<Kunde> response = kundenController.einzeln(5L);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(kunde);
	}

	@Test
	void einzeln_kundeNichtGefunden_gibt404Zurueck() {
		when(werkstattService.getKunde(99L)).thenReturn(null);

		ResponseEntity<Kunde> response = kundenController.einzeln(99L);

		assertThat(response.getStatusCodeValue()).isEqualTo(404);
		assertThat(response.getBody()).isNull();
	}

	// ---- fahrzeuge() ----

	@Test
	void fahrzeuge_gibtFahrzeugeZuKundeZurueck() {
		Fahrzeug f1 = new Fahrzeug();
		List<Fahrzeug> fahrzeuge = Collections.singletonList(f1);
		when(werkstattService.getFahrzeugeZuKunde(3L)).thenReturn(fahrzeuge);

		List<Fahrzeug> result = kundenController.fahrzeuge(3L);

		assertThat(result).isEqualTo(fahrzeuge);
		verify(werkstattService).getFahrzeugeZuKunde(3L);
	}

	@Test
	void fahrzeuge_leereListe() {
		when(werkstattService.getFahrzeugeZuKunde(7L)).thenReturn(Collections.emptyList());

		List<Fahrzeug> result = kundenController.fahrzeuge(7L);

		assertThat(result).isEmpty();
	}

	// ---- anlegen() ----

	@Test
	void anlegen_setztIdAufNullUndSpeichert() {
		Kunde eingabe = new Kunde();
		eingabe.setId(123L);
		eingabe.setNachname("Test");

		Kunde gespeichert = new Kunde();
		gespeichert.setId(1L);
		gespeichert.setNachname("Test");

		when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(gespeichert);

		Kunde result = kundenController.anlegen(eingabe);

		assertThat(eingabe.getId()).isNull();
		assertThat(result).isEqualTo(gespeichert);
		verify(werkstattService).speichereKunde(eingabe);
	}

	// ---- aendern() ----

	@Test
	void aendern_erfolgreich_gibt200MitGespeichertemKundeZurueck() {
		Kunde eingabe = new Kunde();
		Kunde gespeichert = new Kunde();
		gespeichert.setId(10L);

		when(werkstattService.speichereKunde(any(Kunde.class))).thenReturn(gespeichert);

		ResponseEntity<?> response = kundenController.aendern(10L, eingabe);

		assertThat(eingabe.getId()).isEqualTo(10L);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody()).isEqualTo(gespeichert);
		verify(werkstattService).speichereKunde(eingabe);
	}

	@Test
	void aendern_wirftException_gibt500MitMeldungZurueck() {
		Kunde eingabe = new Kunde();
		when(werkstattService.speichereKunde(any(Kunde.class)))
				.thenThrow(new RuntimeException("Fehler beim Speichern"));

		ResponseEntity<?> response = kundenController.aendern(5L, eingabe);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Fehler beim Speichern");
	}

	// ---- loeschen() ----

	@Test
	void loeschen_erfolgreich_gibt200Zurueck() {
		doNothing().when(werkstattService).loescheKunde(4L);

		ResponseEntity<?> response = kundenController.loeschen(4L);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		verify(werkstattService).loescheKunde(4L);
	}

	@Test
	void loeschen_wirftException_gibt500MitMeldungZurueck() {
		doThrow(new RuntimeException("Kunde hat noch Fahrzeuge")).when(werkstattService).loescheKunde(8L);

		ResponseEntity<?> response = kundenController.loeschen(8L);

		assertThat(response.getStatusCodeValue()).isEqualTo(500);
		assertThat(response.getBody()).isEqualTo("Kunde hat noch Fahrzeuge");
	}
}

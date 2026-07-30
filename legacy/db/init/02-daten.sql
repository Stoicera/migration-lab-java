-- =========================================================
-- WerkstattCRM Testdaten (Demo-Stand)
-- Fiktive Kunden und Fahrzeuge, Raum Perg/Linz.
-- =========================================================

INSERT INTO kunde (id, anrede, vorname, nachname, telefon, email, strasse, plz, ort, notiz, angelegt_am) VALUES
	(1, 'Herr', 'Franz', 'Hofer', '+43 664 1111111', 'franz.hofer@aon.at', 'Hauptstraße 4', '4320', 'Perg', 'Stammkunde seit 2016', '2026-01-05 09:12:00'),
	(2, 'Frau', 'Maria', 'Leitner', '+43 664 2222222', 'm.leitner@gmx.at', 'Bahnhofstraße 18', '4311', 'Schwertberg', NULL, '2026-01-08 10:30:00'),
	(3, 'Herr', 'Johann', 'Wimmer', '+43 699 3333333', 'j.wimmer@gmail.com', 'Donaulände 3', '4310', 'Mauthausen', 'zahlt gern bar', '2026-01-15 14:05:00'),
	(4, 'Frau', 'Elisabeth', 'Brunner', '+43 650 4444444', 'e.brunner@liwest.at', 'Landstraße 101', '4020', 'Linz', NULL, '2026-02-02 08:45:00'),
	(5, 'Herr', 'Karl', 'Steiner', '+43 676 5555555', 'karl.steiner@aon.at', 'Feldweg 7', '4320', 'Perg', 'Reifen lagern bei uns (Keller)', '2026-02-11 16:20:00'),
	(6, 'Frau', 'Anna', 'Mayr', '+43 664 6666666', 'anna.mayr@gmx.at', 'Am Anger 12', '4331', 'Naarn', NULL, '2026-03-01 11:00:00'),
	(7, 'Herr', 'Josef', 'Pichler', '+43 699 7777777', NULL, 'Marktplatz 2', '4360', 'Grein', 'kein E-Mail, immer anrufen!', '2026-03-19 09:40:00'),
	(8, 'Firma', NULL, 'Huber Transporte GmbH', '+43 7262 88888', 'office@huber-transporte.at', 'Gewerbestraße 9', '4320', 'Perg', 'Flottenkunde, Sammelrechnung Monatsende gewuenscht (machen wir nicht)', '2026-04-03 07:55:00'),
	(9, 'Frau', 'Christine', 'Wagner', '+43 650 9999999', 'ch.wagner@gmail.com', 'Ringstraße 22', '4312', 'Ried in der Riedmark', NULL, '2026-05-06 13:15:00'),
	(10, 'Herr', 'Thomas', 'Aigner', '+43 676 1010101', 't.aigner@aon.at', 'Waldweg 5', '4391', 'Waldhausen', NULL, '2026-06-12 10:10:00');

INSERT INTO fahrzeug (id, kunde_id, kennzeichen, marke, modell, fahrgestellnr, baujahr, km_stand, pickerl_datum, angelegt_am) VALUES
	(1, 1, 'PE-123AB', 'VW', 'Golf V 1.9 TDI', 'WVWZZZ1KZ6W000001', 2006, 248000, '2026-11-30', '2026-01-05 09:15:00'),
	(2, 1, 'PE-456CD', 'Skoda', 'Octavia Combi', 'TMBJF7NE0F0000002', 2015, 132500, '2027-03-31', '2026-01-05 09:18:00'),
	(3, 2, 'PE-789EF', 'Opel', 'Corsa D 1.2', 'W0L0SDL08C4000003', 2012, 98000, '2026-09-30', '2026-01-08 10:33:00'),
	(4, 3, 'PE-234GH', 'Ford', 'Focus 1.6 TDCi', 'WF0KXXGCBK9000004', 2010, 175300, '2026-08-31', '2026-01-15 14:08:00'),
	(5, 4, 'L-345IJ', 'Audi', 'A4 Avant 2.0 TDI', 'WAUZZZ8K9EA000005', 2014, 141200, '2027-01-31', '2026-02-02 08:50:00'),
	(6, 5, 'PE-567KL', 'VW', 'Passat Variant', 'WVWZZZ3CZBE000006', 2011, 189900, '2026-10-31', '2026-02-11 16:24:00'),
	(7, 6, 'PE-890MN', 'Renault', 'Clio IV', 'VF15R240A54000007', 2016, 61000, '2027-05-31', '2026-03-01 11:04:00'),
	(8, 7, 'PE-321OP', 'Toyota', 'Yaris 1.33', 'VNKKC96380A000008', 2013, 88700, '2026-12-31', '2026-03-19 09:44:00'),
	(9, 8, 'PE-654QR', 'Mercedes-Benz', 'Sprinter 316 CDI', 'WDB90663213000009', 2015, 234000, '2026-09-30', '2026-04-03 08:00:00'),
	(10, 8, 'PE-655QS', 'Mercedes-Benz', 'Sprinter 316 CDI', 'WDB90663213000010', 2016, 198500, '2027-02-28', '2026-04-03 08:02:00'),
	(11, 9, 'PE-987ST', 'Seat', 'Ibiza 1.0 TSI', 'VSSZZZ6JZHR000011', 2017, 45200, '2027-06-30', '2026-05-06 13:18:00'),
	(12, 10, 'PE-135UV', 'BMW', '320d Touring', 'WBA3K51000F000012', 2012, 167800, '2026-08-31', '2026-06-12 10:14:00'),
	(13, 4, 'L-246WX', 'VW', 'Up 1.0', 'WVWZZZAAZJD000013', 2018, 29800, '2027-04-30', '2026-02-02 08:52:00');

-- Auftraege 2026, Statusfluss: ANGENOMMEN -> IN_ARBEIT -> FERTIG -> ABGEHOLT (STORNIERT moeglich)
INSERT INTO auftrag (id, auftrag_nr, fahrzeug_id, kunde_id, status, beschreibung, km_stand, angenommen_am, fertig_am, abgeholt_am) VALUES
	(1, 'A-2026-0001', 1, 1, 'ABGEHOLT', 'Jahresservice inkl. Öl und Filter', 246800, '2026-01-12 08:00:00', '2026-01-13 15:30:00', '2026-01-14 17:00:00'),
	(2, 'A-2026-0002', 3, 2, 'ABGEHOLT', 'Bremsen vorne komplett, Geräusch beim Bremsen', 97400, '2026-01-20 09:30:00', '2026-01-21 16:00:00', '2026-01-22 08:30:00'),
	(3, 'A-2026-0003', 4, 3, 'ABGEHOLT', '§57a Begutachtung (Pickerl)', 174900, '2026-02-05 07:45:00', '2026-02-05 11:00:00', '2026-02-05 16:30:00'),
	(4, 'A-2026-0004', 2, 1, 'STORNIERT', 'Klimaanlage kühlt nicht - Kunde hat Termin abgesagt', 132000, '2026-02-10 10:15:00', NULL, NULL),
	(5, 'A-2026-0005', 5, 4, 'ABGEHOLT', 'Zahnriemen + Wasserpumpe lt. Serviceplan', 140100, '2026-03-03 08:20:00', '2026-03-04 17:15:00', '2026-03-05 09:00:00'),
	(6, 'A-2026-0006', 6, 5, 'ABGEHOLT', 'Kupplung rutscht, Tausch Kupplungssatz', 189200, '2026-03-16 09:00:00', '2026-03-18 14:40:00', '2026-03-18 17:30:00'),
	(7, 'A-2026-0007', 7, 6, 'ABGEHOLT', 'Kleines Service, Innenraumfilter', 60500, '2026-04-08 13:30:00', '2026-04-09 10:20:00', '2026-04-09 15:45:00'),
	(8, 'A-2026-0008', 9, 8, 'ABGEHOLT', '§57a + Service, Firmenfahrzeug', 232900, '2026-05-05 07:30:00', '2026-05-06 12:00:00', '2026-05-06 14:00:00'),
	(9, 'A-2026-0009', 11, 9, 'FERTIG', 'Klimaservice, Kältemittel auffüllen', 45100, '2026-05-19 10:45:00', '2026-05-20 09:30:00', NULL),
	(10, 'A-2026-0010', 8, 7, 'ABGEHOLT', 'Auspuff Endtopf durchgerostet', 88300, '2026-06-09 08:10:00', '2026-06-10 11:50:00', '2026-06-10 16:20:00'),
	(11, 'A-2026-0011', 12, 10, 'IN_ARBEIT', 'Motorlampe leuchtet, Injektor Zylinder 3 defekt', 167800, '2026-06-24 09:20:00', NULL, NULL),
	(12, 'A-2026-0012', 10, 8, 'FERTIG', 'Bremsen hinten + Handbremse nachstellen', 198500, '2026-07-07 07:40:00', '2026-07-08 13:10:00', NULL),
	(13, 'A-2026-0013', 13, 4, 'IN_ARBEIT', '§57a Begutachtung (Pickerl)', 29800, '2026-07-21 08:50:00', NULL, NULL),
	(14, 'A-2026-0014', 2, 1, 'ANGENOMMEN', 'Jahresservice, bitte auch Wischerblätter', 132500, '2026-07-27 09:10:00', NULL, NULL),
	(15, 'A-2026-0015', 1, 1, 'ANGENOMMEN', 'Klappern vorne rechts bei Kopfsteinpflaster', 248000, '2026-07-28 14:00:00', NULL, NULL),
	(16, 'A-2026-0016', 11, 9, 'STORNIERT', 'Radio ohne Funktion - Kunde macht es doch selbst', 45200, '2026-07-15 11:30:00', NULL, NULL);

INSERT INTO auftrag_position (auftrag_id, typ, bezeichnung, menge, einzelpreis) VALUES
	(1, 'ARBEIT', 'Jahresservice lt. Plan', 2.00, 78.00),
	(1, 'MATERIAL', 'Ölfilter', 1.00, 12.50),
	(1, 'MATERIAL', 'Motoröl 5W-30 (Liter)', 4.50, 11.00),
	(2, 'ARBEIT', 'Bremsen vorne erneuern', 3.00, 78.00),
	(2, 'MATERIAL', 'Bremsscheibe vorne (Stk)', 2.00, 45.00),
	(2, 'MATERIAL', 'Bremsbeläge Satz vorne', 1.00, 38.40),
	(3, 'ARBEIT', '§57a Begutachtung', 1.00, 78.00),
	(3, 'MATERIAL', 'Prüfplakette', 1.00, 6.00),
	(5, 'ARBEIT', 'Zahnriemen + WaPu tauschen', 4.50, 88.00),
	(5, 'MATERIAL', 'Zahnriemensatz', 1.00, 185.00),
	(5, 'MATERIAL', 'Wasserpumpe', 1.00, 98.00),
	(6, 'ARBEIT', 'Kupplung tauschen', 5.00, 88.00),
	(6, 'MATERIAL', 'Kupplungssatz', 1.00, 320.00),
	(7, 'ARBEIT', 'Kleines Service', 1.50, 78.00),
	(7, 'MATERIAL', 'Innenraumfilter + Öl', 1.00, 32.60),
	(8, 'ARBEIT', '§57a + Service Sprinter', 3.00, 88.00),
	(8, 'MATERIAL', 'Filter, Öl, Kleinmaterial', 1.00, 76.00),
	(9, 'ARBEIT', 'Klimaservice', 1.00, 78.00),
	(9, 'MATERIAL', 'Kältemittel R134a', 1.00, 45.00),
	(10, 'ARBEIT', 'Endtopf tauschen', 2.00, 78.00),
	(10, 'MATERIAL', 'Endschalldämpfer', 1.00, 89.00),
	(11, 'ARBEIT', 'Fehlersuche + Injektor tauschen', 3.50, 88.00),
	(11, 'MATERIAL', 'Injektor (AT-Teil)', 1.00, 289.00),
	(12, 'ARBEIT', 'Bremsen hinten erneuern', 2.50, 88.00),
	(12, 'MATERIAL', 'Bremsscheiben + Beläge hinten', 1.00, 148.00),
	(13, 'ARBEIT', '§57a Begutachtung', 1.00, 78.00),
	(13, 'MATERIAL', 'Prüfplakette', 1.00, 6.00);

-- Rechnungen zu den abgeholten Auftraegen (Betraege = Summe Positionen, 20% USt)
INSERT INTO rechnung (id, rechnung_nr, auftrag_id, ausgestellt_am, summe_netto, ust, summe_brutto, bezahlt, bezahlt_am) VALUES
	(1, 'R-2026-0001', 1, '2026-01-14 16:45:00', 218.00, 43.60, 261.60, TRUE, '2026-01-14 17:00:00'),
	(2, 'R-2026-0002', 2, '2026-01-22 08:15:00', 362.40, 72.48, 434.88, TRUE, '2026-01-29 10:00:00'),
	(3, 'R-2026-0003', 3, '2026-02-05 16:00:00', 84.00, 16.80, 100.80, TRUE, '2026-02-05 16:30:00'),
	(4, 'R-2026-0004', 5, '2026-03-05 08:45:00', 679.00, 135.80, 814.80, TRUE, '2026-03-12 09:30:00'),
	(5, 'R-2026-0005', 6, '2026-03-18 17:00:00', 760.00, 152.00, 912.00, FALSE, NULL),
	(6, 'R-2026-0006', 7, '2026-04-09 15:30:00', 149.60, 29.92, 179.52, TRUE, '2026-04-09 15:45:00'),
	(7, 'R-2026-0007', 8, '2026-05-06 13:45:00', 340.00, 68.00, 408.00, TRUE, '2026-05-28 11:20:00'),
	(8, 'R-2026-0008', 10, '2026-06-10 16:00:00', 245.00, 49.00, 294.00, TRUE, '2026-06-10 16:20:00');

-- Sequenzen nachziehen, sonst knallt der naechste INSERT
SELECT setval('kunde_id_seq', (SELECT MAX(id) FROM kunde));
SELECT setval('fahrzeug_id_seq', (SELECT MAX(id) FROM fahrzeug));
SELECT setval('auftrag_id_seq', (SELECT MAX(id) FROM auftrag));
SELECT setval('auftrag_position_id_seq', (SELECT MAX(id) FROM auftrag_position));
SELECT setval('rechnung_id_seq', (SELECT MAX(id) FROM rechnung));

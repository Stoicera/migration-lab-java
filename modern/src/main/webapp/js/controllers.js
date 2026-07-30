'use strict';

// =========================================================
// Alle Controller in einer Datei. War 2016 "vorlaeufig",
// ist es immer noch. Ausnahme: bericht-controller.js.
// =========================================================

// ---------------------------------------------------------
// Startseite: Stage 5 - portiert ins neue Angular-Frontend
// ---------------------------------------------------------

// ---------------------------------------------------------
// Kunden: Stage 5 - portiert ins neue Angular-Frontend
// ---------------------------------------------------------

// ---------------------------------------------------------
// Fahrzeuge (Gesamtliste)
// ---------------------------------------------------------
werkstattApp.controller('FahrzeugeCtrl', ['$scope', '$http', function ($scope, $http) {

	$scope.fahrzeuge = [];
	$scope.filter = '';

	$http.get('api/fahrzeuge').then(function (antwort) {
		$scope.fahrzeuge = antwort.data;
	}, function () {
		alert('Fahrzeuge konnten nicht geladen werden!');
	});

}]);

// ---------------------------------------------------------
// Auftraege
// ---------------------------------------------------------
werkstattApp.controller('AuftraegeCtrl', ['$scope', '$http', '$location', function ($scope, $http, $location) {

	$scope.auftraege = [];
	$scope.statusFilter = '';

	$scope.laden = function () {
		var url = 'api/auftraege';
		if ($scope.statusFilter) {
			url = url + '?status=' + $scope.statusFilter;
		}
		$http.get(url).then(function (antwort) {
			$scope.auftraege = antwort.data;
		});
	};

	$scope.filterSetzen = function (status) {
		$scope.statusFilter = status;
		$scope.laden();
	};

	$scope.oeffnen = function (auftrag) {
		$location.path('/auftraege/' + auftrag.id);
	};

	$scope.laden();

}]);

werkstattApp.controller('AuftragNeuCtrl', ['$scope', '$http', '$location', function ($scope, $http, $location) {

	$scope.kunden = [];
	$scope.fahrzeuge = [];
	$scope.auftrag = {};
	$scope.gewaehlterKunde = null;

	$http.get('api/kunden').then(function (antwort) {
		$scope.kunden = antwort.data;
	});

	$scope.kundeGewaehlt = function () {
		$scope.auftrag.fahrzeugId = null;
		$scope.fahrzeuge = [];
		if ($scope.gewaehlterKunde) {
			$http.get('api/kunden/' + $scope.gewaehlterKunde.id + '/fahrzeuge').then(function (antwort) {
				$scope.fahrzeuge = antwort.data;
			});
		}
	};

	$scope.anlegen = function () {
		if (!$scope.gewaehlterKunde || !$scope.auftrag.fahrzeugId) {
			alert('Bitte Kunde und Fahrzeug auswählen!');
			return;
		}
		$scope.auftrag.kundeId = $scope.gewaehlterKunde.id;
		$http.post('api/auftraege', $scope.auftrag).then(function (antwort) {
			$location.path('/auftraege/' + antwort.data.id);
		}, function (fehler) {
			alert('Auftrag anlegen fehlgeschlagen: ' + fehler.data);
		});
	};

}]);

werkstattApp.controller('AuftragDetailCtrl', ['$scope', '$routeParams', '$http', '$location',
	function ($scope, $routeParams, $http, $location) {

	$scope.auftrag = null;
	$scope.neuePosition = { typ: 'ARBEIT', menge: 1 };

	$scope.laden = function () {
		$http.get('api/auftraege/' + $routeParams.id).then(function (antwort) {
			$scope.auftrag = antwort.data;
		});
	};

	$scope.statusSetzen = function (neuerStatus) {
		$http.put('api/auftraege/' + $routeParams.id + '/status?neu=' + neuerStatus).then(function () {
			$scope.laden();
		}, function (fehler) {
			alert(fehler.data);
		});
	};

	$scope.positionSpeichern = function () {
		if (!$scope.neuePosition.bezeichnung) {
			alert('Bezeichnung fehlt!');
			return;
		}
		$http.post('api/auftraege/' + $routeParams.id + '/positionen', $scope.neuePosition).then(function () {
			$scope.neuePosition = { typ: 'ARBEIT', menge: 1 };
			$scope.laden();
		}, function (fehler) {
			alert(fehler.data);
		});
	};

	$scope.positionLoeschen = function (position) {
		$http.delete('api/auftraege/positionen/' + position.id).then(function () {
			$scope.laden();
		});
	};

	$scope.rechnungErstellen = function () {
		if (!confirm('Rechnung zu diesem Auftrag erstellen?')) {
			return;
		}
		$http.post('api/rechnungen/auftrag/' + $routeParams.id).then(function (antwort) {
			$location.path('/rechnungen/' + antwort.data.id);
		}, function (fehler) {
			alert(fehler.data);
		});
	};

	$scope.laden();

}]);

// ---------------------------------------------------------
// Rechnungen
// ---------------------------------------------------------
werkstattApp.controller('RechnungenCtrl', ['$scope', 'Api', function ($scope, Api) {

	$scope.rechnungen = [];
	$scope.nurOffene = false;

	$scope.laden = function () {
		Api.rechnungen().then(function (antwort) {
			$scope.rechnungen = antwort.data;
		});
	};

	$scope.bezahltSetzen = function (rechnung) {
		Api.rechnungBezahlt(rechnung.id).then(function () {
			$scope.laden();
		});
	};

	$scope.anzeigen = function (rechnung) {
		if ($scope.nurOffene) {
			return !rechnung.bezahlt;
		}
		return true;
	};

	$scope.laden();

}]);

werkstattApp.controller('RechnungDetailCtrl', ['$scope', '$routeParams', 'Api', '$http',
	function ($scope, $routeParams, Api, $http) {

	$scope.rechnung = null;
	$scope.auftrag = null;

	Api.rechnung($routeParams.id).then(function (antwort) {
		$scope.rechnung = antwort.data;
		// Auftrag samt Positionen nachladen fuer die Detailzeilen
		$http.get('api/auftraege/' + antwort.data.auftragId).then(function (a2) {
			$scope.auftrag = a2.data;
		});
	});

	$scope.drucken = function () {
		window.print();
	};

}]);

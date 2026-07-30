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
// Fahrzeuge (Gesamtliste): Stage 5 - portiert ins neue Angular-Frontend
// ---------------------------------------------------------

// ---------------------------------------------------------
// Auftraege: Stage 5 - portiert ins neue Angular-Frontend
// ---------------------------------------------------------

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

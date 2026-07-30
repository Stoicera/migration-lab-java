'use strict';

// Monatsbericht - eigene Datei (Praktikant, Sommer 2018).
// Stil weicht vom Rest ab, funktioniert aber.
werkstattApp.controller('BerichtCtrl', ['$scope', '$http', function ($scope, $http) {

	var heuer = new Date().getFullYear();

	$scope.jahre = [];
	for (var j = heuer; j >= 2016; j--) {
		$scope.jahre.push(j);
	}
	$scope.jahr = heuer;

	$scope.monate = [];
	$scope.topKunden = [];

	$scope.laden = function () {
		$http.get('api/bericht/monat?jahr=' + $scope.jahr).then(function (antwort) {
			$scope.monate = antwort.data;
			var netto = 0;
			var brutto = 0;
			var auftraege = 0;
			antwort.data.forEach(function (m) {
				netto += m.umsatzNetto;
				brutto += m.umsatzBrutto;
				auftraege += m.anzahlAuftraege;
			});
			$scope.summeNetto = netto;
			$scope.summeBrutto = brutto;
			$scope.summeAuftraege = auftraege;
		});
		$http.get('api/bericht/topkunden?jahr=' + $scope.jahr).then(function (antwort) {
			$scope.topKunden = antwort.data;
		});
	};

	$scope.laden();

}]);

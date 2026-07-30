'use strict';

// WerkstattCRM Frontend, AngularJS 1.x
// gewachsen seit 2016, bitte Aenderungen mit Hrn. B. absprechen
var werkstattApp = angular.module('werkstattApp', ['ngRoute']);

// Stage 5 (Strangler Fig, ADR-0009): Routen, die schon im neuen Angular-
// Frontend leben, verlassen diese App per komplettem Seitenwechsel. Ein
// Eintrag wandert pro portierter Route hierher, bis die App leer ist.
var portierteRouten = [
	/^\/start$/,
	/^\/kunden(\/.*)?$/,
	/^\/fahrzeuge$/,
	/^\/auftraege(\/.*)?$/
];

werkstattApp.config(['$routeProvider', function ($routeProvider) {
	$routeProvider
		.when('/rechnungen', {
			templateUrl: 'views/rechnungen.html',
			controller: 'RechnungenCtrl'
		})
		.when('/rechnungen/:id', {
			templateUrl: 'views/rechnung-detail.html',
			controller: 'RechnungDetailCtrl'
		})
		.when('/bericht', {
			templateUrl: 'views/bericht.html',
			controller: 'BerichtCtrl'
		})
		.otherwise({
			redirectTo: '/start'
		});
}]);

// Uebergabe an das neue Frontend: greift auch, wenn eine alte Route (z.B. die
// .otherwise-Weiterleitung auf /start) auf eine schon portierte Route zeigt.
werkstattApp.run(['$rootScope', '$location', function ($rootScope, $location) {
	$rootScope.$on('$routeChangeStart', function () {
		var pfad = $location.path();
		for (var i = 0; i < portierteRouten.length; i++) {
			if (portierteRouten[i].test(pfad)) {
				window.location.replace(pfad);
				return;
			}
		}
	});
}]);

werkstattApp.run(['$rootScope', function ($rootScope) {
	// Status-Anzeigetexte, muessen mit dem Backend zusammenpassen
	$rootScope.statusText = {
		'ANGENOMMEN': 'Angenommen',
		'IN_ARBEIT': 'In Arbeit',
		'FERTIG': 'Fertig',
		'ABGEHOLT': 'Abgeholt',
		'STORNIERT': 'Storniert'
	};
}]);

// Betraege: wir haengen einfach das Euro-Zeichen an, der currency-Filter
// wuerde Dollar anzeigen (Locale-Thema, nie fertig geloest)
werkstattApp.filter('euro', function () {
	return function (betrag) {
		if (betrag === null || betrag === undefined) {
			return '';
		}
		return betrag.toFixed(2).replace('.', ',') + ' €';
	};
});

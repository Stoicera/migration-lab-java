'use strict';

// WerkstattCRM Frontend, AngularJS 1.x
// gewachsen seit 2016, bitte Aenderungen mit Hrn. B. absprechen
var werkstattApp = angular.module('werkstattApp', ['ngRoute']);

werkstattApp.config(['$routeProvider', function ($routeProvider) {
	$routeProvider
		.when('/start', {
			templateUrl: 'views/dashboard.html',
			controller: 'DashboardCtrl'
		})
		.when('/kunden', {
			templateUrl: 'views/kunden.html',
			controller: 'KundenCtrl'
		})
		.when('/kunden/neu', {
			templateUrl: 'views/kunde-detail.html',
			controller: 'KundeDetailCtrl'
		})
		.when('/kunden/:id', {
			templateUrl: 'views/kunde-detail.html',
			controller: 'KundeDetailCtrl'
		})
		.when('/fahrzeuge', {
			templateUrl: 'views/fahrzeuge.html',
			controller: 'FahrzeugeCtrl'
		})
		.when('/auftraege', {
			templateUrl: 'views/auftraege.html',
			controller: 'AuftraegeCtrl'
		})
		.when('/auftraege/neu', {
			templateUrl: 'views/auftrag-neu.html',
			controller: 'AuftragNeuCtrl'
		})
		.when('/auftraege/:id', {
			templateUrl: 'views/auftrag-detail.html',
			controller: 'AuftragDetailCtrl'
		})
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

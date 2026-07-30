'use strict';

// Api-Service, Idee war mal, alle $http-Aufrufe hier zu sammeln.
// Verwendet wird er nur von manchen Controllern, der Rest ruft $http direkt.
werkstattApp.factory('Api', ['$http', function ($http) {
	return {
		// Kunden-Methoden: Stage 5 - portiert ins neue Angular-Frontend
		rechnungen: function () {
			return $http.get('api/rechnungen');
		},
		rechnung: function (id) {
			return $http.get('api/rechnungen/' + id);
		},
		rechnungBezahlt: function (id) {
			return $http.put('api/rechnungen/' + id + '/bezahlt');
		}
	};
}]);

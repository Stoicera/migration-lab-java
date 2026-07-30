'use strict';

// Api-Service, Idee war mal, alle $http-Aufrufe hier zu sammeln.
// Verwendet wird er nur von manchen Controllern, der Rest ruft $http direkt.
werkstattApp.factory('Api', ['$http', function ($http) {
	return {
		kunden: function (suche) {
			var url = 'api/kunden';
			if (suche) {
				url = url + '?suche=' + encodeURIComponent(suche);
			}
			return $http.get(url);
		},
		kunde: function (id) {
			return $http.get('api/kunden/' + id);
		},
		kundeSpeichern: function (kunde) {
			if (kunde.id) {
				return $http.put('api/kunden/' + kunde.id, kunde);
			}
			return $http.post('api/kunden', kunde);
		},
		kundeLoeschen: function (id) {
			return $http.delete('api/kunden/' + id);
		},
		fahrzeugeZuKunde: function (kundeId) {
			return $http.get('api/kunden/' + kundeId + '/fahrzeuge');
		},
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

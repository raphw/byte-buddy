var byteBuddyApp = angular.module('byteBuddy', ['ngRoute']);

byteBuddyApp.directive('markdown', function () {
    var converter = new Showdown.converter();
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var htmlText = converter.makeHtml(element.text());
            element.html(htmlText);
        }
    };
});

byteBuddyApp.config(function ($routeProvider) {
    $routeProvider
        .when('/', {
            controller: 'noOp',
            templateUrl: 'partial/main.html'
        })
        .when('/tutorial', {
            controller: 'noOp',
            templateUrl: 'partial/tutorial.html'
        })
        .when('/develop', {
            controller: 'noOp',
            templateUrl: 'partial/develop.html'
        })
        .otherwise({redirectTo: '/'});

});

byteBuddyApp.controller('menuController', function ($scope, $location) {
    $scope.menuItems = [
        {name: 'Welcome', target: '#/'},
        {name: 'Learn', target: '#/tutorial'},
        {name: 'Develop', target: '#/develop'},
        {name: 'API', target: 'javadoc/v0_1/index.html'}
    ];
    $scope.activeClass = function (current) {
        return current.target === '#' + ($location.path() || '/') ? 'active' : '';
    };
});

byteBuddyApp.controller('socialMediaController', function ($scope) {
    $scope.icons = [
        {name: 'Google', style: 'google', target: 'https://plus.google.com/share?url=_URL_'},
        {name: 'LinkedIn', style: 'linkedin', target: 'http://www.linkedin.com/shareArticle?url=_URL_'},
        {name: 'Twitter', style: 'twitter', target: 'http://twitter.com/share?url=_URL_&text=_ADDITIONAL_TEXT_&via=TWITTER_NAME'},
        {name: 'Stack Overflow', style: 'stackover', target: '#'},
        {name: 'GitHub', style: 'github', target: '#'},
        {name: 'RSS', style: 'rss', target: '#'},
        {name: 'Reddit', style: 'reedit', target: '#'}
    ];
});

byteBuddyApp.controller('noOp', function ($scope) {

});

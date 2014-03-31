var byteBuddyApp = angular.module('byteBuddy', ['ngRoute', 'ui.bootstrap', 'ui.bootstrap.affix', 'placeholders', 'duScroll']);

byteBuddyApp.value('duScrollDuration', 3500);

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
    $scope.isCollapsed = true;
});

byteBuddyApp.controller('socialMediaController', function ($scope) {
    $scope.icons = [
        {name: 'Google', style: 'google', target: '#'},
        {name: 'LinkedIn', style: 'linkedin', target: '#'},
        {name: 'Twitter', style: 'twitter', target: '#'},
        {name: 'Stack Overflow', style: 'stackover', target: '#'},
        {name: 'GitHub', style: 'github', target: '#'},
        {name: 'RSS', style: 'rss', target: '#'},
        {name: 'Reddit', style: 'reedit', target: '#'}
    ];
});

byteBuddyApp.controller('noOp', function ($scope) {

});

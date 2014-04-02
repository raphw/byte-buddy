angular.module('byteBuddy', ['ngRoute', 'ui.bootstrap', 'ui.bootstrap.affix', 'placeholders', 'duScroll'])

    .value('duScrollDuration', 3500)

    .config(function ($routeProvider) {
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
    })

    .controller('menuController', function ($scope, $location, $rootScope, scroller) {
        $scope.menuItems = [
            {name: 'Welcome', target: '#/'},
            {name: 'Learn', target: '#/tutorial'},
            {name: 'Develop', target: '#/develop'},
            {name: 'API', target: 'javadoc/v0_1/index.html'}
        ];
        $scope.activeClass = function (current) {
            return current.target === '#' + ($location.path() || '/') ? 'active' : '';
        };
        var collapsed = true;
        $scope.isCollapsed = function() {
            return collapsed;
        };
        $scope.toggleCollapse = function() {
            collapsed = !collapsed;
        };
        $scope.collapse = function() {
            collapsed = true;
        };
        $rootScope.$on("$routeChangeStart", function (event, next, current) {
            scroller.scrollTo(0, 0, 1500);
        });
    })

    .controller('socialMediaController', function ($scope) {
        $scope.icons = [
            {name: 'Google', style: 'google', target: '#'},
            {name: 'LinkedIn', style: 'linkedin', target: '#'},
            {name: 'Twitter', style: 'twitter', target: '#'},
            {name: 'Stack Overflow', style: 'stackover', target: '#'},
            {name: 'GitHub', style: 'github', target: '#'},
            {name: 'RSS', style: 'rss', target: '#'},
            {name: 'Reddit', style: 'reedit', target: '#'}
        ];
    })

    .controller('noOp', function ($scope) {
    });

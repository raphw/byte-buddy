angular.module('byteBuddy', ['ngRoute', 'ui.bootstrap', 'ui.bootstrap.affix', 'duScroll'])

    .value('duScrollDuration', 3500)

    .config(function ($routeProvider) {
        $routeProvider
            .when('/', {
                controller: 'mainController',
                templateUrl: 'partial/main.html'
            })
            .when('/tutorial', {
                controller: 'tutorialController',
                templateUrl: 'partial/tutorial.html'
            })
            .when('/develop', {
                controller: 'developController',
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
        $scope.isCollapsed = function () {
            return collapsed;
        };
        $scope.toggleCollapse = function () {
            collapsed = !collapsed;
        };
        $scope.collapse = function () {
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

    .controller('mainController', function ($scope) {
        $scope.links = [
            {name: 'Welcome', target: '#welcome'},
            {name: 'Hello world', target: '#helloworld'},
            {name: 'Getting Byte Buddy', target: '#getbytebuddy'},
            {name: 'Dependency management', target: '#dependency'},
            {name: 'Support', target: '#support'}
        ];
    })

    .controller('tutorialController', function ($scope) {
        $scope.links = [
            {name: 'Use cases', target: '#concept'},
            {name: 'Creating a class', target: '#gettingstarted'},
            {name: 'Fields and methods', target: '#members'},
            {name: 'Annotations', target: '#attributes'},
            {name: 'Cook book', target: '#cookbook'},
            {name: 'Custom methods', target: '#customization'}
        ];
    })

    .controller('developController', function ($scope) {
    });

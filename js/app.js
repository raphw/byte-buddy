angular.module('byteBuddy', ['ngRoute', 'ui.bootstrap', 'ui.bootstrap.affix', 'duScroll'])

    .value('duScrollDuration', 2000)

    .constant('repository', {
        groupId: 'net.bytebuddy',
        artifactId: 'byte-buddy',
        version: '1.2.3'
    })

    .config(function ($routeProvider) {
        $routeProvider
            .when('/', {
                controller: 'mainController',
                templateUrl: 'partial/main.partial.html'
            })
            .when('/tutorial', {
                controller: 'tutorialController',
                templateUrl: 'partial/tutorial.partial.html'
            })
            .when('/develop', {
                controller: 'developController',
                templateUrl: 'partial/develop.partial.html'
            })
            .otherwise({redirectTo: '/'});
    })

    .directive('prettyprint', function () {
        return {
            restrict: 'C',
            link: function postLink(scope, element) {
                element.html(prettyPrintOne(element.html(), '', true));
            }
        };
    })

    .controller('menuController', function ($scope, $location, $rootScope, scroller, repository) {
        $scope.menuItems = [
            {name: 'Welcome', target: '#/'},
            {name: 'Learn', target: '#/tutorial'},
            {name: 'Develop', target: '#/develop'},
            {name: 'API', target: 'javadoc/' + repository.version + '/index.html'}
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
            {name: 'Google', style: 'google', target: 'https://plus.google.com/share?url=http%3A%2F%2Fbytebuddy.net'},
            {
                name: 'LinkedIn',
                style: 'linkedin',
                target: 'http://www.linkedin.com/shareArticle?mini=true&url=http%3A%2F%2Fbytebuddy.net&title=Byte%20Buddy%20-%20runtime%20code%20generation%20for%20Java&summary=Byte%20Buddy%20is%20a%20light-weight%20and%20easy%20to%20use%20library%20for%20generating%20Java%20code%20at%20the%20run%20time%20of%20a%20Java%20application.%20It%20is%20open-source%20and%20free.%20Check%20it%20out%20at%20http%3A%2F%2Fbytebuddy.net&source=http%3A%2F%2Fbytebuddy.net'
            },
            {
                name: 'Twitter',
                style: 'twitter',
                target: 'https://twitter.com/intent/tweet?source=webclient&text=Check%20out%20%23ByteBuddy%2C%20a%20library%20for%20generating%20%23Java%20code%20at%20run%20time%20%28http%3A%2F%2Fbytebuddy.net%29'
            },
            {
                name: 'Stack Overflow',
                style: 'stackover',
                target: 'http://stackoverflow.com/questions/tagged/byte-buddy'
            },
            {name: 'GitHub', style: 'github', target: 'https://github.com/raphw/byte-buddy'},
            {
                name: 'Facebook',
                style: 'facebook',
                target: 'https://www.facebook.com/sharer/sharer.php?u=http%3A%2F%2Fbytebuddy.net'
            },
            {name: 'Reddit', style: 'reedit', target: 'http://www.reddit.com/submit?url=http%3A%2F%2Fbytebuddy.net'}
        ];
    })

    .controller('mainController', function ($scope, repository) {
        $scope.links = [
            {name: 'Welcome', target: '#welcome'},
            {name: 'Hello world', target: '#helloworld'},
            {name: 'Getting Byte Buddy', target: '#getbytebuddy'},
            {name: 'Dependency management', target: '#dependency'},
            {name: 'Support', target: '#support'}
        ];
        $scope.version = repository.version;
        $scope.tabs = [
            {
                title: 'Maven', content: '<dependency>\n  <groupId>'
            + repository.groupId + '</groupId>\n  <artifactId>'
            + repository.artifactId + '</artifactId>\n  <version>'
            + repository.version + '</version>\n</dependency>'
            },
            {
                title: 'Gradle', content: repository.groupId + ':'
            + repository.artifactId + ':'
            + repository.version
            },
            {
                title: 'SBT', content: 'libraryDependencies += "'
            + repository.groupId + '" % "'
            + repository.artifactId + '" % "'
            + repository.version + '"'
            },
            {
                title: 'Ivy', content: '<dependency org="'
            + repository.groupId + '" name="'
            + repository.artifactId + '" rev="'
            + repository.version + '" />'
            },
            {
                title: 'Buildr', content: '\'' + repository.groupId + ':'
            + repository.artifactId + ':jar:'
            + repository.version + '\''
            },
            {
                title: 'Grape', content: '@Grapes(\n  @Grab(group=\''
            + repository.groupId + '\', module=\''
            + repository.artifactId + '\', version=\''
            + repository.version + '\')\n)'
            },
            {
                title: 'Leiningen', content: '[' + repository.groupId + '/'
            + repository.artifactId + ' "'
            + repository.version + '"]'
            }
        ];
    })

    .controller('tutorialController', function ($scope) {
        $scope.links = [
            {name: 'Preliminary', target: '#rational'},
            {name: 'Creating a class', target: '#gettingstarted'},
            {name: 'Fields and methods', target: '#members'},
            {name: 'Annotations', target: '#annotation'},
            {name: 'Custom instrumentation', target: '#customization'}
        ];
    })

    .controller('developController', function ($scope) {
    });

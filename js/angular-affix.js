angular.module('ui.bootstrap.affix', [])
    .directive('affix', [ '$window', '$document', '$parse', function ($window, $document, $parse) {
        return {
            scope: { affix: '@' },
            link: function (scope, element, attrs) {
                var win = angular.element($window), affixed;
                win.bind('scroll', checkPosition);
                win.bind('click', function () {
                    setTimeout(checkPosition, 1);
                });
                function checkPosition() {
                    var target = document.getElementById('affixComponent');
                    if(!target) {
                        return;
                    }
                    var offset = $parse(scope.affix)(scope);
                    var currentOffset = win.prop('pageYOffset');
                    var documentHeight = target.clientHeight || target.offsetHeight || target.scrollHeight;
                    var bottomOffset = -180;
                    var affix;
                    if (currentOffset <= offset) {
                        affix = 'top';
                    } else if (documentHeight && currentOffset >= documentHeight + bottomOffset) {
                        affix = 'bottom';
                    } else {
                        affix = false;
                    }
                    if (affixed === affix) {
                        return;
                    }
                    affixed = affix;
                    var style = affix == 'bottom' ? 'top: ' + (documentHeight + bottomOffset) + 'px;' : '';
                    element
                        .removeClass('affix affix-top affix-bottom')
                        .addClass('affix' + (affix ? '-' + affix : ''))
                        .attr('style', style);
                }
            }
        };
    }]);

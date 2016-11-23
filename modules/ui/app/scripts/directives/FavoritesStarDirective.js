/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('favoritesStar', ['UtilitiesFactory',
    function (UtilitiesFactory) {
        return {
            restrict: 'A',
            scope: {
                afterFavoriteFunc: '=afterFavoriteFunc',
                favoritesStarExperimentId: '=favoritesStarExperimentId',
                favoritesStarFavoritesObject: '=favoritesStarFavoritesObject'
            },
            link: function (scope, element, attrs) {
                var tileWidth = $(element).closest('li').eq(0).width() + 10;
                var parentTag = (attrs.favoritesStarParentTag ? attrs.favoritesStarParentTag : 'li');

                if ((typeof(attrs.isFavorite) === 'string' && attrs.isFavorite === 'true') ||
                    (typeof(attrs.isFavorite) === 'boolean' && attrs.isFavorite)) {
                    $(element).closest(parentTag).toggleClass('favorite');
                    $(element).attr('title', 'Undo Mark as Favorite');
                }
                else {
                    $(element).attr('title', 'Mark as Favorite');
                }

                $(element).on('click', function() {
                    var l, t, $item, $temp, $move, $link = $(this);

                    $('.tooltip').remove();
                    $item = $link.closest(parentTag).toggleClass('favorite');
                    l = $item.position().left;
                    t = $item.position().top;

                    if ($item.hasClass('favorite')) {
                        // Save that this is now a favorite.
                        UtilitiesFactory.saveFavorite(scope.favoritesStarExperimentId, scope.favoritesStarFavoritesObject);
                        if (scope.isFavoriteFlag !== undefined) {
                            scope.isFavoriteFlag = true;
                        }

                        $link.attr('title', 'Undo Mark as Favorite');
                        if (scope.afterFavoriteFunc) {
                            // If the user of this directive has passed a function to call after the state of the
                            // favorite star is changed, call it.  Pass the starred item so it can be used, if necessary,
                            // like to animate it.
                            scope.afterFavoriteFunc.call(this, $item, tileWidth);
                        }
                        else {
                            // Default animation used when the directive user didn't pass an afterFavoriteFunc.
                            // This is used by the "card view".

                            // Move to position 1, the favorite.
                            if ($item.index() === 0) {
                                return false;
                            }
                            $move = $('#exprSummary>li:lt(' + $item.index() +')');
                            $temp = $item.clone().css('visibility', 'hidden').insertBefore($item);
                            $item.css({'left':l, 'top':t}).addClass('moving');

                            $move.each(function() {
                                if (this.offsetLeft > 450) {
                                    $(this).animate({'left': -tileWidth * 2, 'top': this.offsetHeight + 16}, 500); // move from rightmost down and to the left
                                }else {
                                    $(this).animate({'left': tileWidth}, 500); // slide right
                                }
                            });
                            $item.animate({'left':0, 'top':0}, 500);
                            // clean up after all animations have compmleted
                            $('#exprSummary>li').promise().done(function() {
                                $(this).css({'left':0, 'top':0});
                                $item.prependTo($('#exprSummary')).removeClass('moving');
                                $temp.remove();
                            });
                            $(document.body, document.documentElement).animate({'scrollTop':0}, 500);
                        }
                    }
                    else {
                        // Save that this is NO LONGER a favorite.
                        UtilitiesFactory.removeFavorite(scope.favoritesStarExperimentId, scope.favoritesStarFavoritesObject);
                        $link.attr('title', 'Mark as Favorite');

                        if (scope.afterFavoriteFunc) {
                            scope.afterFavoriteFunc.call(this, $item);
                        }
                        else {
                            // Move to the position to the right of the last favorite.
                            $move = $('#exprSummary>li:gt(' + $item.index() +')').filter('.favorite');
                            if (!$move.length) {
                                return false;
                            }
                            $temp = $item.clone().css('visibility', 'hidden').insertBefore($item);
                            $item.css({'left':l, 'top':t}).addClass('moving');
                            l = $move.last().position().left;
                            t = $move.last().position().top;

                            $move.each(function() {
                                if (this.offsetLeft === 0) {
                                    $(this).animate({'left': tileWidth * 2, 'top': -this.offsetHeight - 16}, 500); // move  from leftmost up and to the right
                                }else {
                                    $(this).animate({'left': -tileWidth}, 500); // slide left
                                }
                            });
                            $item.animate({'left':l, 'top':t}, 500);
                            // clean up after all animations have compmleted
                            $('#exprSummary>li').promise().done(function() {
                                $(this).css({'left':0, 'top':0});
                                $item.insertAfter( $move.last() ).removeClass('moving');
                                $temp.remove();
                            });
                        }
                    }

                    return false;
                });
            }
        };
    }
]);

/* global $:false */

'use strict';

angular.module('wasabi.directives').directive('feedbackGuys', [
    function () {
        return {
            restrict: 'A',
            scope: {
                saveFunction: '=saveFunction',
                closeFunction: '=closeFunction'
            },
            link: function (scope) {

                (function() {
                    var $container, $comments, $mouth, $contact, c = [];

                    function init() {
                        if ($container && $container.length) {
                            return;
                        }

                        $container = $('div.css3-guys');
                        $comments = $('#txtFeedbackComments');
                        $mouth = $('div.css3-mouth');
                        $contact = $('#chkFeedback');

                        (function() {
                            var a;
                            c = [];
                            $container.find('div.inner').each(function(index) {
                                var s, i, n = $(this), o = {};

                                if (index % 2 === 0) {
                                    a = n;
                                }else {
                                    o.eye1 = {};
                                    o.eye2 = {};
                                    o.eye1.defaultPos = {top: parseInt(a.css('top'), 10), left: parseInt(a.css('left'), 10)};
                                    o.eye2.defaultPos = {top: parseInt(n.css('top'), 10), left: parseInt(n.css('left'), 10)};
                                    o.eye1.$el = a;
                                    o.eye2.$el = n;
                                    o.eye1.client = a.offset();
                                    o.eye2.client = n.offset();
                                    s = {};
                                    i = {};
                                    s.$el = a.find('div.sparkle');
                                    i.$el = n.find('div.sparkle');
                                    s.defaultPos = {top: parseInt(i.$el.css('top'), 10), left: parseInt(i.$el.css('left'), 10)};
                                    o.eye1.sparkle = s;
                                    o.eye2.sparkle = i;
                                    c.push(o);
                                }
                            });
                        })();

                        $('#feedbackDialog').mouseenter(function() {
                            $container.addClass('active');

                        }).mouseleave(function() {
                            $comments.is(':focus') || $contact.is(':checked') || $('#nps .sel').length || $('body').hasClass('hurray') || $container.removeClass('active');
                        }).on('panelBeforeShow', function() {
                            $('body').removeClass('hurray');
                            $mouth.removeClass('smile1 smile2');
                            $container.removeClass('active');
                            $('#chkFeedback').removeAttr('checked');
                        });

                        $comments.mouseenter(function() {
                            $mouth.addClass('smile1');
                        }).mouseleave(function() {
                            $('#nps .sel').length || $mouth.removeClass('smile1');
                        }).focusin(function() {
                            $mouth.addClass('smile2');
                        }).blur(function() {
                            setTimeout(function() {
                                $contact.is(':checked') || $('body').hasClass('hurray') || $mouth.removeClass('smile2');
                            }, 100);
                        });

                        $contact.click(function() {
                            this.checked ? $mouth.addClass('smile2') : $mouth.removeClass('smile2');
                        });

                        $('body').on('mousemove', function(e) {
                            $.each(c, function(index, a) {
                                var s = a.eye1, i = a.eye2;

                                e.pageX < s.client.left ? (s.$el.css({
                                    left: s.defaultPos.left - 2
                                }), i.$el.css({
                                    left: i.defaultPos.left - 2
                                })) : e.pageX > i.client.left ? (s.$el.css({
                                    left: s.defaultPos.left + 5
                                }), i.$el.css({
                                    left: i.defaultPos.left + 5
                                })) : (s.$el.css({
                                    left: s.defaultPos.left
                                }), i.$el.css({
                                    left: i.defaultPos.left
                                }));

                                e.pageY < s.$el.offset().top ? (s.$el.css({
                                    top: s.defaultPos.top - 2
                                }), i.$el.css({
                                    top: i.defaultPos.top - 2
                                })) : e.pageY > s.$el.offset().top ? (s.$el.css({
                                    top: s.defaultPos.top + 2
                                }), i.$el.css({
                                    top: i.defaultPos.top + 2
                                })) : (s.$el.css({
                                    top: s.defaultPos.top
                                }), i.$el.css({
                                    top: i.defaultPos.top
                                }));
                            });
                        });

                        $('#nps').click(function() {
                            setTimeout(function() {
                                if ($('#nps .sel').length) {
                                    $mouth.addClass('smile1');
                                }
                            }, 15);
                        });
                        $('#btnFedbackSubmit').click(function() {
                            $mouth.addClass('smile2');
                            $('body').addClass('hurray');
                            scope.saveFunction.call();
                            setTimeout(function() {
                                scope.closeFunction.call();
                                $('body').removeClass('hurray');
                            }, 800);
                        });
                        $('#btnFeedbackCancel').click(function() {
                            $mouth.removeClass('smile1 smile2');
                            $container.removeClass('active');
                            setTimeout(function() {
                                scope.closeFunction.call();
                            }, 400);
                            return false;
                        });
                    }

                    return {
                        init: init
                    };
                })().init();

            }
        };
    }
]);

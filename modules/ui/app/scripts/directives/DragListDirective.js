/*global $:false */

'use strict';

angular.module('wasabi.directives').directive('dragList', ['$compile', 'PrioritiesFactory', 'MutualExclusionsFactory', 'UtilitiesFactory',
    function ($compile, PrioritiesFactory, MutualExclusionsFactory, UtilitiesFactory) {

        // NOTE: this directive pretty much is specific to the Wasabi Priority list.  It not only
        // implements the drag and drop change in priorities, but also implements the ability to change
        // the priority by editing the priority in the input fields in the list.
        // However, in order to support that AND the ability to change the sampling percentages of multiple
        // experiments at once, the priority input fields must have the class "priorityInput".
        function initDragListBehaviors(listId) {
            var $row, $rows, $visibleRows, $dragRow, yDiff, startY, maxY, rowIndex, visIndex, rowTop, rowHeight, firstRowTop, scrollTimer,
                mouseDown = false, dragging = false, suppressDrag = false;

            // direct priority assignment
            function doInputChange() {
                var $input = $(this),
                    $row = $input.closest('tr'),
                    rowHeight = $row[0].offsetHeight,
                    $rows = $row.closest('table').find('tr'),
                    $inputs, $visRow, a, i, n, t, v = $.trim($input.val());

                // change the index and clean up
                function changeIndex() {
                    $dragRow.remove();
                    $dragRow = null;
                    $row.removeClass('moving');
                    if (n < $row[0].rowIndex) {
                        $row.insertAfter($rows.eq(n - 1));
                    }else {
                        $row.insertAfter($rows.eq(n));
                    }
                    $rows = $('#' + listId).find('tr');
                    rowShading();
                    $inputs = $row.closest('table').find('input.priorityInput');
                    n = $inputs.length;
                    for (i = 0; i < n; i++) {
                        $inputs.eq(i).val(i + 1);
                    }
                    suppressDrag = false;

                    savePriorityChange($rows);
                }

                mouseDown = false;
                dragging = false;
                if (suppressDrag) {
                    return;
                }
                if (!v || isNaN(v)) {
                    $input.val($row[0].rowIndex);
                    return;
                }
                suppressDrag = true;
                n = Math.max(parseInt(v), 1);
                n = Math.min(n, $rows.length - 1);

                createDragElement($row);
                $row.addClass('moving');

                // "n" is the new index and "a" is the index of the row we're animating to (the visible index).
                // if row n is not hidden then a == n, othwise "a" is the closest visible row to "n"
                a = n;
                if ($rows.eq(a).css('display') === 'none') {
                    // moving up in the list
                    if (a < $row[0].rowIndex) {
                        $visRow = $rows.eq(a).nextAll(':visible'); // get the next higher visible row

                    // moving down in the list
                    }else {
                        $visRow = $rows.eq(a).prevAll(':visible'); // get the next lower visible row

                    }
                    a = $visRow[0].rowIndex; // row index of the first matched element ($visRow is a collection)
                    // if a == the current row index then we don't need to animate
                    if (a === $row[0].rowIndex) {
                        changeIndex();
                        return;
                    }
                }

                // scroll page if needed
                t = $rows.eq(a).offset().top;
                if ($(window).scrollTop() > t - 36) {
                    $('body, html').animate({'scrollTop':t-36}, 400);

                }else if($(window).scrollTop() + $(window).height() < t + rowHeight) {
                    $('body, html').animate({'scrollTop':t + rowHeight - $(window).height()}, 400);
                }

                // begin transition
                $dragRow.one('transitionend', changeIndex).css('transform', 'translate3d(-17px,' + ($rows.eq(a).offset().top - $row.offset().top + 3) + 'px,0)');
            }
            // end direct priority assignment

            // drag & drop reorder behavior
            $('#' + listId).on('mousedown', 'tr', function(e) {
                var i, $list = $('#' + listId);

                if (dragging) {
                    doDragEnd();
                    return;
                }
                if (suppressDrag || this.rowIndex === 0 || e.target.nodeName === 'A') {
                    return;
                }

                $row = $(this);
                $rows = $list.find('tr');

                // highlight mutually exclusive experiments
                $rows.removeClass('hilite active');
                $row.addClass('active');

                // Retrieve the experiments with which this experiment is mutually exclusive and then highlight them
                MutualExclusionsFactory.query({
                    experimentId: $row.data('id'),
                    exclusiveFlag: true
                }).$promise.then(function (meExperiments) {
                    for (var i = 0; i < meExperiments.length; i++) {
                        for (var j = 1; j < $rows.length; j++) {
                            if ($rows.eq(j).data('id') === meExperiments[i].id) {
                                $rows.eq(j).addClass('hilite');
                            }
                        }
                    }
                }, function(response) {
                    UtilitiesFactory.handleGlobalError(response, 'The mutual exclusions could not be retrieved.');
                });

                if (e.target.nodeName === 'INPUT') {
                    return;
                }

                if ($list.attr('no-drag') === 'true') {
                    return;
                }

                mouseDown = true;
                startY = e.pageY;
                $visibleRows = $rows.filter(':visible');
                yDiff = $row.offset().top - startY;
                rowIndex = this.rowIndex; // the index of the current row in the entire list
                visIndex = rowIndex;      // the index of the current row in the set of visible rows
                for (i = 1; i < rowIndex; i++) {
                    if ($rows.eq(i).css('display') === 'none') {
                        --visIndex;
                    }
                }
                rowTop = $row.offset().top;
                rowHeight = $row[0].offsetHeight;
                firstRowTop = $visibleRows.eq(1).offset().top;
                maxY = $list.offset().top + $list.height() - 10;

            }).on('change', 'input.priorityInput', doInputChange);

            function doMouseMove(e) {
                if (!mouseDown) {
                    return;
                }
                clearInterval(scrollTimer);

                var y = e.pageY + yDiff,
                    $window = $(window),
                    scrollTop = $window.scrollTop(),
                    scrollMax = (document.body.scrollHeight || document.documentElement.scrollHeight) - $window.height();

                if (!dragging) {
                    // init dragging if mouse Y delta exceeds threshold
                    if (Math.abs(e.pageY - startY) > 8) {
                        dragging = true;
                        // TODO: tooltips.disable();
                        createDragElement($row, y);
                        $row.addClass('moving');
                        $('<div id="dropArrow"><span>' + $row[0].rowIndex + '</span></div>')
                            .css({'left':$row.offset().left - 100, 'top':rowTop + rowHeight}).appendTo($('body'));
                    }
                    return false;
                }

                function moveRow() {
                    var index, vIndex, i, numRows = $rows.length;

                    y = Math.max(y, firstRowTop - 15);
                    y = Math.min(y, maxY);
                    $dragRow.css('top', y);

                    if (y === firstRowTop - 15) {
                        vIndex = 1;
                        index = 1;
                    }else if(y === maxY) {
                        vIndex = $visibleRows.length - 1;
                        index = numRows - 1;
                    }else if (y < rowTop) {
                        vIndex = Math.floor((y - firstRowTop ) / rowHeight) + 2;
                    }else if (y > rowTop + rowHeight) {
                        vIndex = Math.floor((y - firstRowTop) / rowHeight) + 1;
                    }else {
                        vIndex = visIndex;
                    }
                    if (!index) {
                        index = $visibleRows[vIndex].rowIndex;
                    }

                    if (index !== rowIndex) {
                        for (i = 1; i < numRows; i++) {
                            if (i >= index && i < $row[0].rowIndex) {
                                $rows.eq(i).find('input.priorityInput').val(i + 1);
                            }else if (i > $row[0].rowIndex && i <= index) {
                                $rows.eq(i).find('input.priorityInput').val(i - 1);
                            }else {
                                $rows.eq(i).find('input.priorityInput').val(i);
                            }
                        }
                        $('#dropArrow').stop().animate({'top': $visibleRows.eq(vIndex).offset().top + (vIndex >= visIndex ? rowHeight:0)}, 60, function() {
                            $(this).css('display', 'block');
                        });
                        $('#dropArrow').find('span').text(index);
                        $dragRow.find('input.priorityInput').val(index);
                        rowIndex = index;
                    }
                }

                moveRow();

                if (y + 60 > $window.height() + scrollTop && scrollTop < scrollMax) {
                    // scroll down
                    scrollTimer = setInterval(function() {
                        scrollTop += 2;
                        y += 2;
                        if (scrollTop > scrollMax) {
                            clearInterval(scrollTimer);
                            scrollTop = scrollMax;
                        }
                        $window.scrollTop(scrollTop);
                        moveRow();
                    }, 1);

                }else if(y - 50 < scrollTop && scrollTop > 0) {
                    // scroll up
                    scrollTimer = setInterval(function() {
                        scrollTop -= 2;
                        y -= 2;
                        if (scrollTop < 0) {
                            clearInterval(scrollTimer);
                            scrollTop = 0;
                        }
                        $window.scrollTop(scrollTop);
                        moveRow();
                    }, 1);
                }
                return false;
            }

            $(document).mousemove(doMouseMove).mouseup(function() {
                mouseDown = false;
                if (dragging) {
                    doDragEnd();
                }
            });

            function doDragEnd() {
                dragging = false;
                clearInterval(scrollTimer);
                $row.find('input.priorityInput').val(rowIndex);
                if (rowIndex < $row[0].rowIndex) {
                    $row.insertAfter($rows.eq(rowIndex - 1));
                }else {
                    $row.insertAfter($rows.eq(rowIndex));
                }
                $rows = $('#' + listId).find('tr');
                rowShading();
                $dragRow.css('box-shadow', 'none').animate({'left':$row.offset().left, 'top':$row.offset().top}, 80, function() {
                    $dragRow.remove();
                    $dragRow = null;
                    $('#dropArrow').remove();
                    $row.removeClass('moving');
                });

                // TODO: tooltips.enable();

                savePriorityChange($rows);
            }
            // end drag & drop reorder behavior

            // supporting functions
            function savePriorityChange($rows) {
                // Extract the IDs for all the rows, in order, and save the new priority order.
                var orderedIds = [];
                for (var i=1; i < $rows.length; i++) {
                    orderedIds.push($rows.eq(i).data('id') + '');
                }
                var appName = $rows.eq(1).data('application-name');
                PrioritiesFactory.update({
                    'applicationName': appName,
                    'experimentIDs': orderedIds
                }).$promise.then(function () {
                    // Nothing needs doing here after we've reordered the experiment priorities
                    UtilitiesFactory.trackEvent('saveItemSuccess',
                        {key: 'dialog_name', value: 'experimentsRePrioritized'},
                        {key: 'application_name', value: appName});

                    UtilitiesFactory.displaySuccessWithCacheWarning('Experiment Priorities Changed', 'Your experiment priority changes have been saved.');
                }, function(response) {
                    // Handler error
                    //console.log(response);
                    // TODO: Should revert to the previous positions...
                    UtilitiesFactory.handleGlobalError(response, 'Your experiment priorities could not be changed.');
                });

            }

            function createDragElement($row, y) {
                var i, $dragCells,
                    $rowCells = $row.children(),
                    n = $rowCells.length,
                    l = $row.offset().left + 16,
                    t = y || $row.offset().top;

                $dragRow = $('<div id="dragRow" class="tableContainer"><table></table></div>').css({'left':l, 'top':t});
                $dragRow.find('table').append($row.clone());
                // set the width of the cells
                $dragCells = $dragRow.find('td');
                for (i = 0; i < n; i++) {
                    $dragCells.eq(i).css('width', $rowCells.eq(i).width());
                }
                $dragRow.appendTo($('body'));
            }

            // can't rely on CSS becuase rows can be hidden
            function rowShading() {
                var i, $rows = $('#' + listId + ' tr:visible'), n = $rows.length;

                for (i = 1; i < n; i++) {
                    $rows.eq(i).css('background', i % 2 === 0? 'rgb(246,247,248)':'white');
                }
            }
        }

        return {
            restrict: 'A',
            scope: true,
            link: function (scope, element, attrs) {
                $compile(element.contents())(scope);

                initDragListBehaviors(attrs.id);
            }
        };
    }]);
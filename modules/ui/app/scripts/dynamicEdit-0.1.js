/*
 *  Created by Rainer Link
 */
(function($) { 
    
/* DynamicEdit constructor */
function DynamicEdit(el) {
    var $tbr = $(el).html('<a href="#" class="dynamicSave" title="Save"><span></span>Save</a><a href="#" class="dynamicCancel" title="Cancel"><span></span>Cancel</a><a href="#" class="dynamicEdit" title="Edit"><span></span>Edit</a>'),
        $saveCancel = $tbr.find('.dynamicSave, .dynamicCancel'),
        $edit = $tbr.find('.dynamicEdit');
    
    $tbr.on('click', 'a', function() {
        var $link = $(this);

        if ($link.hasClass('dynamicEdit')) {
            startEditing($link);
        }else if ($link.hasClass('dynamicCancel')) {
            doneEditing('editCancel');
            
        }else if ($link.hasClass('dynamicSave')) {
            doneEditing('editSave');
        }
        return false;
    });

    function startEditing($link, flag) {
        var sendStartEvent = (flag !== undefined ? flag : true);
        $tbr.addClass('editing');
        $link.attr('tempTabIndex', $link.attr('tabIndex') || 0).attr('tabIndex', -1).animate({'opacity':.2}, 300); // prevent tabbing to the link while in edit mode
        $saveCancel.css('opacity', 0).animate({'left':0, 'opacity':1}, 300, function() {});
        if (sendStartEvent) {
            $tbr.trigger('editStart');
        }
    }
    
    function doneEditing(event) {
        $edit.attr('tabIndex', $edit.attr('tempTabIndex')).animate({'opacity':1}, 300);
        $saveCancel.eq(0).animate({'left':60, 'opacity':0}, 300);
        $saveCancel.eq(1).animate({'left':30, 'opacity':0}, 300);
        $tbr.removeClass('editing').trigger(event);
    }

    return {
        // Make this externally visible so it can be accessed to expand the widgets, again, say in case of an
        // error on the save.
        displayWidgets: startEditing
    };
    
} /* end constructor */

$.fn.extend({
	dynamicEdit: function() {
		return this.each(function() {
			if (!$(this).data('dynamicEdit')) {
				$(this).data('dynamicEdit', new DynamicEdit(this));
			}
		});
	}
});
})(jQuery);
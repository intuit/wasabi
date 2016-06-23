'use strict';

describe('DialogsFactory', function () {

    var DialogsFactory;
    beforeEach(inject(function(){
        var $injector = angular.injector(['ng', 'ui.bootstrap', 'wasabi.services']);

        DialogsFactory = function () {
            return $injector.get('DialogsFactory');
        };
    }));

    describe('#alertDialog', function () {

        it('executes and calls a callback', function () {
            var Dialog = new DialogsFactory();
            Dialog.alertDialog('hello', 'hello', function () {
                expect(Dialog).to.exist;
            });
        });
        
        it('executes without a callback', function () {
            var Dialog = new DialogsFactory();
            Dialog.alertDialog('hello', 'hello');
            expect(Dialog).to.exist;
        });
    });
    
    describe('#confirmDialog', function () {

        it('executes and calls a callback', function () {
            var Dialog = new DialogsFactory();
            var cancel = sinon.spy();
            var okLabel = sinon.spy();
            var cancelLabel = sinon.spy();
            Dialog.confirmDialog('hello', 'hello', function () {
                expect(Dialog).to.exist;
                expect(cancel.calledOnce);
                expect(okLabel.calledOnce);
                expect(cancelLabel.calledOnce);
            }, cancel, okLabel, cancelLabel);
        });
    });
});
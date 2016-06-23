'use strict';

describe('Session', function () {

    var Session;
    beforeEach(inject(function(){
        var $injector = angular.injector(['wasabi.services']);

        Session = function () {
            return $injector.get('Session');
        };
    }));

    describe('Constructor', function () {

        it('exists and has correct properties', function () {
            var sess = new Session();
            var properties = ['create', 'destroy', 'restore'];

            properties.forEach(function(d){
                expect(sess).to.have.property(d).that.is.a('function');
            });
        });
    });
    
    var sessionInfo = {
        userID: 'angular',
        accessToken: '12345',
        tokenType: 'random',
        userRole: 'ADMIN',
        isSuperadmin: false,
        permissions: ['ADMIN'],
        switches: {}
    };
    
    describe('#create', function () {

        it('should generate subkeys on valid input', function () {
            var SessionFactory = new Session();
            var session = SessionFactory.create(sessionInfo);
            Object.keys(sessionInfo).forEach(function(d){
                expect(SessionFactory).to.have.property(d);
                expect(SessionFactory[d]).to.eql(sessionInfo[d]);
            });
            session = null;
        });
    });
    
    describe('#destroy', function () {

        it('should destroy subkeys if deleting after create', function () {
            var SessionFactory = new Session();
            var session = SessionFactory.create(sessionInfo);
            Object.keys(sessionInfo).forEach(function(d){
                expect(SessionFactory).to.have.property(d);
                expect(SessionFactory[d]).to.eql(sessionInfo[d]);
            });
            
            SessionFactory.destroy();
            
            Object.keys(sessionInfo).forEach(function(d){
                expect(SessionFactory).to.have.property(d);
                if (d === 'isSuperadmin') {
                    expect(SessionFactory[d]).to.eql(false);
                } else if (d === 'switches') {
                    expect(SessionFactory[d]).to.eql({});
                } else {
                    expect(SessionFactory[d]).to.be.null;
                }
            });
            session = null;
        });
    });
    
    describe('#destroy', function () {

        it('should destroy subkeys if deleting after create', function () {
            var SessionFactory = new Session();
            var session = SessionFactory.create(sessionInfo);
            SessionFactory.restore();
            Object.keys(sessionInfo).forEach(function(d){
                expect(SessionFactory).to.have.property(d);
                expect(SessionFactory[d]).to.eql(sessionInfo[d]);
            });
            session = null;
        });
    });
});
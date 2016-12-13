/**
 * Created by kmenzli on 12/8/16.
 */
// Bootstrap manually the ng application, since :
// - it allows to correctly reinit the ng app after a (portal) page edition. Indeed, if we use the automatic init,
//   when the (portal) page is edited and then saved, the module are not registered again, so they are not executed
// - it is a good practice if we want to allow several ng app in the same html page (which could be the case with several portlets)
require( ["SHARED/jquery", "marketPlaceControllers"], function ( $,  mkControllers)
{
    $( document ).ready(function() {
        var mpAppRoot = $('#mpAddon');
        var mpApp = angular.module('mpApp', ["xeditable"]);
        try {
            mpApp.controller('mpCtrl', mkControllers);
            angular.bootstrap(mpAppRoot, ['mpApp']);
        } catch(e) {
            console.log(e);
        }
    });
});
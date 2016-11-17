define('categManagementCommon', ['SHARED/jquery', 'SHARED/juzu-ajax'], function($) {

    var categMgmtCommon = {};

    var isDomReadyExcuted = false;
    $(document).ready(function($) {
        isDomReadyExcuted = true;
    });

    categMgmtCommon.onReady = function(callback) {
        if (isDomReadyExcuted) {
            callback();
        } else {
            $(document).ready(callback);
        }
    };
    return categMgmtCommon;

});
require(['categManagementCommon','categoryManagementAdd','SHARED/jquery', 'SHARED/juzu-ajax'], function(categMgmtCommon,categMgmtAdd, $) {

        $(document).ready(function() {
                categMgmtAdd.init(categMgmtCommon);
        });

});
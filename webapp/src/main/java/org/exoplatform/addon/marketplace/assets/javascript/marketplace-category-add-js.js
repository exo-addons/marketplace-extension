define('categoryManagementAdd', ['SHARED/jquery'], function($) {

    var categMgmtAdd = {};

    categMgmtAdd.init = function(categMgmtCommon) {
        console.log('step 1');
        var $categoryPortlet = $('.categoriesManagementPortlet');
        $categoryPortlet.on('submit', 'form.add-category-form', function(e) {
            var $form = $(e.target).closest('form');
            var $title = $form.find('input[name="textinput-0"]');
            var name = $title.val();
            var description = $form.find('textarea[name="textarea-0"]').val();

            if(name == '') {
                name = $title.attr('placeholder');
            }

            var createURL = $form.jzURL('CategoryManagement.addCategory');
            $.ajax({
                type: 'POST',
                url: createURL,
                data: {name: name, description: description},
                success: function(data) {
                    // Reload project tree;
                    console.log(data.id);
                },
                error: function(xhr) {
                    if (xhr.status >= 400) {
                        console.log(xhr.responseText);
                    } else {
                        console.log('error while create new category. Please try again.');
                    }
                }
            });
            return false;
        });
    }

    return categMgmtAdd;

});
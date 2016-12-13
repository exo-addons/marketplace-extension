define("marketPlaceControllers", [ "SHARED/jquery", "SHARED/juzu-ajax" ], function(
    $) {
  var mpCtrl = function($scope, $q, $timeout, $http, $filter) {
    var mpContainer = $('#mpAddon');
    var deferred = $q.defer();

    //$scope.typeNames = ["JVMType", "OSType", "ASType", "DBType", "EnvType", "SSOType"];
    $scope.categories = [];

    $scope.loadBundles = function() {
      $http({
        method : 'GET',
        url : mpContainer.jzURL('CategoryManagement.getBundle') + "&locale=" + eXo.env.portal.language
      }).then(function successCallback(data) {
        $scope.i18n = data.data;
        deferred.resolve(data);
      }, function errorCallback(data) {
        $scope.setResultMessage(data, "error");
      });
    }

    // function which set the result message with the given style
    $scope.setResultMessage = function(text, type) {
      $scope.resultMessageClass = "alert-" + type;
      $scope.resultMessageClassExt = "uiIcon" + type.charAt(0).toUpperCase()+ type.slice(1);
      $scope.resultMessage = text;
    }

    $scope.refreshController = function() {
      try {
        $scope.$digest()
      } catch (excep) {
        // No need to display errors in console
      }
    };

    $scope.loadCategories = function() {
      $http({
        method : 'GET',
        url : mpContainer.jzURL('CategoryManagement.getCategories')
      }).then(function successCallback(data) {
        $scope.categories = data.data;
        deferred.resolve(data);
      }, function errorCallback(data) {
        $scope.setResultMessage(data, "error");
      });
    }

    $scope.saveCategory = function(data, id) {
      //newType.name = typeData.name;
      angular.extend(data, {id: id});
      $http({
        data : data,
        method : 'POST',
        headers : {
          'Content-Type' : 'application/json'
        },
        url : mpContainer.jzURL('CategoryManagement.saveCategory')
      }).then(function successCallback(data) {
        $scope.setResultMessage($scope.i18n.typeSaved, "success");
        newType.id = data.data.id;

        $timeout(function() {
          $scope.setResultMessage("", "info")
        }, 3000);
      }, function errorCallback(data) {
        $scope.setResultMessage($scope.i18n.error, "error");
      });
    };

    $scope.removeCategory = function(index) {
      category = $scope.categories[index];
      if(category && category.id) {
        $http({
          data : category,
          method : 'POST',
          headers : {
            'Content-Type' : 'application/json'
          },
          url : mpContainer.jzURL('CategoryManagement.deleteCategory')
        }).then(function successCallback(data) {
          $scope.setResultMessage($scope.i18n.typeDeleted, "success");
          category = $scope.categories.splice(index, 1);

          $timeout(function() {
            $scope.setResultMessage("", "info")
          }, 3000);
        }, function errorCallback(data) {
          $scope.setResultMessage($scope.i18n.typeDeleteError, "error");
        });
      }
    };

    $scope.addCategory = function() {
      $scope.inserted = {
        id: $scope.categories.length+1,
        label: '',
        description: ''
      };
      $scope.categories.push($scope.inserted);
    };

    $scope.loadBundles();
    $scope.loadCategories();
    $('#mpAddon').css('visibility', 'visible');
    $(".mpLoadingBar").remove();
  };
  return mpCtrl;
});
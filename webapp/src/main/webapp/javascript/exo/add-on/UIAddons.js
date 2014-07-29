(function() {
	function UIAddons() {
		
	};
	
	UIAddons.prototype.init = function() {
		try {
		  
		  if (CKEDITOR && typeof CKEDITOR == "object") {
		    for ( var name in CKEDITOR.instances ) {
		      var oEditor ;
		      try {
		        oEditor = CKEDITOR.instances[name] ;
		        if (oEditor && document.getElementById(name)) {
		            var rendered = jQuery(document.getElementById(name)).nextAll('span:first')[0].id.indexOf('cke');
		            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
	
		        }
		      } catch(e) {
		        continue ;
		      }
		    }
		  }
		 } catch(e) {}
	
	
	
		eXo.webui.UIForm.submitForm = function(formId, action, useAjax, callback) {
		  if(action.toLowerCase() == "changetype" || action.toLowerCase() == "close" || action.toLowerCase() == "back") {
		  	if (eXo.ecm.ECMUtils) {
		      eXo.ecm.ECMUtils.editFullScreen = false;
		    }
		    if (b_changed) {      
				  var answer = null;
				  if (action.toLowerCase() == "changetype") {
				  	answer = confirm(document.getElementById("ChangeTypeConfirmationMsg").innerHTML);
				  } else {
				  	answer = confirm(document.getElementById("CloseConfirmationMsg").innerHTML);
				  }  
		      if (answer) {
		      	b_changed = false;
		      }	else {
		      	return;
		      }
		    }
		  }    
		 
		 if (!callback) callback = null;
		 var form = this.getFormElemt(formId) ;
		 //TODO need review try-cactch block for form doesn't use FCK
		 try {
		  if (FCKeditorAPI && typeof FCKeditorAPI == "object") {
		    for ( var name in FCKeditorAPI.__Instances ) {
		      var oEditor ;
		      try {
		        oEditor = FCKeditorAPI.__Instances[name] ;
		        if (oEditor && oEditor.GetParentForm && oEditor.GetParentForm() == form ) {
		          oEditor.UpdateLinkedField() ;
		        }
		      } catch(e) {
		        continue ;
		      }
		    }
		  }
		 } catch(e) {}
		
		 try {
		  
		  if (CKEDITOR && typeof CKEDITOR == "object") {
		    for ( var name in CKEDITOR.instances ) {
		      var oEditor ;
		      try {
		        oEditor = CKEDITOR.instances[name] ;
		        if (oEditor && document.getElementById(name)) {
		            var rendered = jQuery(document.getElementById(name)).nextAll('span:first')[0].id.indexOf('cke');
		            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
	
		        }
		      } catch(e) {
		        continue ;
		      }
		    }
		  }
		 } catch(e) {}
		 if(form)	
		  form.elements['formOp'].value = action ;
		  
	
		 if (navigator.appName == 'Microsoft Internet Explorer')
		 {
		   if ((action.toLowerCase() == "save" || action.toLowerCase() == "saveandclose" || action.toLowerCase() == "close") && window.popup_opened == true) {
		     window.onbeforeunload = null;
		     useAjax=false;
		     window.popup_opened = false;
		   }
		  }
	
		  if(useAjax) {
		    b_changed = false;
		    this.ajaxPost(form, callback) ;
		  } else {
		    form.submit();
		  }
		} ;
	}
	eXo.UIAddons = new UIAddons();
	
	return eXo.UIAddons;
	//-------------------------------------------------------------------------//
})();

	
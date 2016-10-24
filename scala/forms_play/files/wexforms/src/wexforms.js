(function () {
  'use strict';
  class WexForm {

    constructor() {
      // Hi
      Object.assign(this, {
        sfFormUri: encodeURIComponent('http://deductions-software.com/ontologies/forms#personForm'),
        ajaxActionsData: {
          formDisplay: {
            page: 'form',
            query: 'displayuri',
          },
          formCreate: {
            page: 'create',
            query: 'uri',
          },
          formEdit: {
            page: 'form',
            query: 'displayuri',
          },
        }
      });
    }

    formDisplay(uri, success) {
      this.ajaxFormQuery('formDisplay', uri, success);
    }

    formCreate(uri, success) {
      this.ajaxFormQuery('formCreate', uri, success);
    }

    formEdit(uri, success) {
      this.ajaxFormQuery('formEdit', uri, success);
    }

    urlBuild(actionName, uri) {
      var data = this.ajaxActionsData[actionName];
      return '/' + data.page + '?' +
        // Query string change regarding action type.
        data.query + '=' + encodeURIComponent(uri) +
        // Append form definition.
        '&formuri=' + this.sfFormUri
    }

    ajaxFormQuery(name, uri, success) {
      this.ajax({
        url: this.urlBuild(name, uri),
        success: success
      });
    }

    /**
     * Simple AJAX request
     * @param {Object} options Contain various ajax options.
     */
    ajax(options) {
      var xhr = new window.XMLHttpRequest(),
        data = options.data ? this.param(options.data) : undefined,
        method = options.method || 'GET', success = options.success,
        url = options.url;
      // Create xhr.
      xhr.open(method,
        // On GET mode append data as query strings.
        method === 'GET' && data ? url + '?' + data : url,
        // Async by default.
        options.async !== undefined ? options.async : true);
      // Define callback.
      xhr.onreadystatechange = function () {
        // Process complete.
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
            // Callback function specified.
            if (success && typeof success === 'function') {
              success(xhr);
            }
          }
          else if (options.error) {
            options.error(xhr);
          }
        }
      };
      // Requested headers.
      if (method === 'POST') {
        xhr.setRequestHeader('Content-type',
          'application/x-www-form-urlencoded');
      }
      // Lets go.
      xhr.send(data);
    }
  }

  window.WexForm = WexForm;
}());
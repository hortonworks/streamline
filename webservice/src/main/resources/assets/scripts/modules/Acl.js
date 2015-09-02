/**
 * Module for managing access control on UI.
 *
 */
define(['require',
  'rest/SessionREST',
  'models/VAppState'
], function(require, SessionREST, VAppState) {
  'use strict';

  var MSAcl = {};


  /**************************************************************
   *          Access List segregated modulewise
   **************************************************************/

  /*
   * Default = read access to all
   */

  var AccessList = {};
  AccessList.menu = {
    roles: {
      '*': 'read',
    },
    _chl: {
      "dashboard": {
        policy: 'reset',
        roles: {
          ROLE_SYS_ADMIN: "read",
          ROLE_ADMIN: "read"
        }
      }
    }
  };

  /***************************************************
   *          Implementation
   ***************************************************/

  /*
   * Algorithm used :
   * 1] go to the leaf node
   * 2] if the leaf node has given "role : operation" return true.
   * 3] if the leaf node does not have perm AND has 'policy:reset' return false.
   * 4] if the leaf node does not have perm AND does not have 'policy:reset' traverse to parent.
   */

  /*
   * Iterate over the resArr and return only the roles/policy in retArr.
   */
  function getNodeRoles(resArr, currNode, retArr) {
    if (resArr.length === 0) {
      return;
    }
    var head = resArr.splice(0, 1);
    if (!currNode) {
      currNode = AccessList;
    }
    var nodeName = head[0];
    var nodeProps = currNode[nodeName];
    if (_.isUndefined(nodeProps)) {
      console.log(nodeName + " is undefined");
      return false;
    }
    if (_.has(nodeProps, 'roles')) {
      retArr.push(_.pick(nodeProps, 'roles', 'policy'));
    } else {
      //If nothing is specified, default is read for all
      retArr.push({
        '*': 'read'
      });
    }
    currNode = currNode[nodeName]._chl;
    return getNodeRoles(resArr, currNode, retArr);
  }

  /*
   * Main function called by the canXYZ()
   * @resource  : string. The node to check. Eg: 'menu.account.users'
   * @roles   : array.  User provided roles. If non provided, take the loggedIn users role.
   * @operation : string. The operation to perform. Valid values: create/read/update/delete
   */
  function canPerfromAction(resource, roles, operation) {
      var allRoles = (_.isArray(roles)) ? roles : [];
      if (!roles) {

        // take roles from the global account id
        // if user has system admin role(userRoleList) add to the list
        var userRoles = SessionREST.getUserProfile().get('userRoleList');
        if (SessionREST.userInRole('ROLE_SYS_ADMIN')) {
          allRoles.push('ROLE_SYS_ADMIN');
        } else {
          var selAcc = MAppState.get('currentAccount');
          if (selAcc) {
            var accountUser = MAppState.getAccountUser().done(function(accountUser) {
              var roleList = accountUser.get('roleList');
              if (_.isString(roleList)) {
                allRoles.push(roleList);
              } else if (_.isArray(roleList)) {
                _.each(roleList, function(r) {
                  allRoles.push(r);
                });
              }
            });
            MAppState.set('currentUserRoles', allRoles);
          }
        }

      }
      var resourceArr = [];
      // split the requested resource
      if ($.type(resource) === 'string') {
        resourceArr = resource.split('.');
      }

      var nodeAttrsList = [];
      getNodeRoles(resourceArr, null, nodeAttrsList);

      // Start from the bottom and go up until we find "policy: reset".
      for (var i = nodeAttrsList.length; i > 0; i--) {
        var ret = checkPerm(nodeAttrsList[i - 1], allRoles, operation);
        // If checkPerm returns true/false return it.
        if (ret !== null) {
          console.log("ACL=> resource: " + resource + " op:" + operation + " ret: " + ret);
          return ret;
        }
        // else move to parent node..
      }
      console.log("ACL=> resource: " + resource + " op:" + operation + " ret: false");
      return false;

    }
    /*
     * 1] if the leaf node has given "role : operation" return true.
     * 2] if the leaf node does not have perm AND has 'policy:reset' return false.
     * 3] if the leaf node does not have perm AND does not have 'policy:reset' traverse to parent.
     */

  function checkPerm(nodeAttrs, userRoles, operation) {
    var hasPerm = null;
    var nodeRolesObj = nodeAttrs.roles;
    var policyReset = _.has(nodeAttrs, 'policy');

    if (!_.isArray(userRoles)) {
      userRoles = [userRoles];
    }

    // Check against all the user roles
    for (var i = 0; i < userRoles.length; i++) {
      if (checkForRole(nodeRolesObj, userRoles[i], operation)) {
        return true;
      }
      if (policyReset) {
        hasPerm = false;
      }
    }
    return hasPerm;
  }

  function checkForRole(nodeRolesObj, userRole, operation) {
    if (!(_.has(nodeRolesObj, userRole) || _.has(nodeRolesObj, '*'))) {
      return false;
    }

    var allowedOperation = '';
    if (_.has(nodeRolesObj, userRole)) {
      allowedOperation = nodeRolesObj[userRole];
    } else if (_.has(nodeRolesObj, '*')) {
      allowedOperation = 'read';
    } else {
      return false;
    }

    switch (allowedOperation) {
      case 'create':
        return _.contains(['create', 'read', 'update'], operation) === true ? true : false;
      case 'read':
        return _.contains(['read'], operation) === true ? true : false;
      case 'update':
        return _.contains(['read', 'update'], operation) === true ? true : false;
      case 'delete':
        return _.contains(['create', 'read', 'update', 'delete'], operation) === true ? true : false;
      default:
        return false;
    }
    return false;
  }


  /***************************************************
   *          Public APIs
   ***************************************************/

  MSAcl.canRead = function(resource, roles) {
    return canPerfromAction(resource, roles, 'read');
  };

  MSAcl.canCreate = function(resource, roles) {
    return canPerfromAction(resource, roles, 'create');
  };

  MSAcl.canUpdate = function(resource, roles) {
    return canPerfromAction(resource, roles, 'update');
  };

  MSAcl.canDelete = function(resource, roles) {
    return canPerfromAction(resource, roles, 'delete');
  };

  window.MSAcl = MSAcl;
  return MSAcl;

});
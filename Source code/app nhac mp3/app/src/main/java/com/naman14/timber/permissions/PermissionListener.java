

package com.naman14.timber.permissions;

public interface PermissionListener {

    void permissionsChanged(String permissionChanged);


    void permissionsGranted(String permissionGranted);


    void permissionsRemoved(String permissionRemoved);
}
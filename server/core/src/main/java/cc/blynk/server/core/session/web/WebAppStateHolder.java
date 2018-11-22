package cc.blynk.server.core.session.web;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.permissions.Role;
import cc.blynk.server.core.session.mobile.MobileStateHolder;
import cc.blynk.server.core.session.mobile.Version;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class WebAppStateHolder extends MobileStateHolder {

    private static final int NO_DEVICE = -1;

    public int selectedDeviceId;
    public final Role role;

    public WebAppStateHolder(int orgId, User user, Role role, Version version) {
        super(orgId, user, version);
        this.role = role;
        this.selectedDeviceId = NO_DEVICE;
    }

    public boolean isSelected(int deviceId) {
        return selectedDeviceId == deviceId;
    }

}

package cc.blynk.server.core.reporting.raw;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.GraphPinRequest;

import java.io.Serializable;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.07.17.
 */
public final class BaseReportingKey implements Serializable {

    public final String email;
    public final int orgId;
    public final int deviceId;
    public final PinType pinType;
    public final short pin;

    public BaseReportingKey(User user, GraphPinRequest graphPinRequest) {
        this(user.email, user.orgId,
             graphPinRequest.deviceId,
             graphPinRequest.pinType, graphPinRequest.pin);
    }

    public BaseReportingKey(String email, int orgId, int deviceId, PinType pinType, short pin) {
        this.email = email;
        this.orgId = orgId;
        this.deviceId = deviceId;
        this.pinType = pinType;
        this.pin = pin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseReportingKey)) {
            return false;
        }

        BaseReportingKey that = (BaseReportingKey) o;

        if (deviceId != that.deviceId) {
            return false;
        }
        if (pin != that.pin) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (orgId != that.orgId) {
            return false;
        }
        return pinType == that.pinType;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + orgId;
        result = 31 * result + deviceId;
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        result = 31 * result + (int) pin;
        return result;
    }
}

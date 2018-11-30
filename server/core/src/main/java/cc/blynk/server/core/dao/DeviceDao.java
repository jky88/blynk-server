package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.exceptions.DeviceNotFoundException;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.product.Product;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cc.blynk.utils.StringUtils.truncateFileName;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.17.
 */
public class DeviceDao {

    private static final Logger log = LogManager.getLogger(DeviceDao.class);

    public final ConcurrentMap<Integer, Device> devices;
    private final AtomicInteger deviceSequence;
    private final DeviceTokenManager deviceTokenManager;

    public DeviceDao(Collection<Organization> orgs, DeviceTokenManager deviceTokenManager) {
        devices = new ConcurrentHashMap<>();

        int maxDeviceId = 0;
        for (Organization org : orgs) {
            for (Product product : org.products) {
                for (Device device : product.devices) {
                    maxDeviceId = Math.max(maxDeviceId, device.id);
                    devices.put(device.id, device);
                }
            }
        }

        this.deviceSequence = new AtomicInteger(maxDeviceId);
        this.deviceTokenManager = deviceTokenManager;
        log.info("Devices count is {}, sequence is {}", devices.size(), deviceSequence.get());
    }

    public int getId() {
        return deviceSequence.incrementAndGet();
    }

    public void createWithPredefinedIdAndToken(int orgId, String email, Product product, Device device) {
        devices.put(device.id, device);
        deviceTokenManager.assignNewToken(orgId, email, product, device, device.token);
    }

    public Device createWithPredefinedId(int orgId, String email, Product product, Device device) {
        devices.put(device.id, device);
        deviceTokenManager.assignNewToken(orgId, email, product, device);
        return device;
    }

    public Device create(int orgId, String email, Product product, Device device) {
        device.id = deviceSequence.incrementAndGet();
        return createWithPredefinedId(orgId, email, product, device);
    }

    public Device delete(int deviceId) {
        Device device = devices.remove(deviceId);
        //also removes deivce from the product
        deviceTokenManager.deleteDevice(device);
        return device;
    }

    public Device getById(int deviceId) {
        return devices.get(deviceId);
    }

    public Device getByIdOrThrow(int deviceId) {
        Device device = devices.get(deviceId);
        if (device == null) {
            log.error("Device with id {} not found.", deviceId);
            throw new DeviceNotFoundException("Requested device not exists.");
        }
        return device;
    }

    public Collection<Device> getAll() {
        return devices.values();
    }

    public boolean productHasDevices(int productId) {
        for (var deviceEntry : devices.entrySet()) {
            Device device = deviceEntry.getValue();
            if (device.productId == productId) {
                return true;
            }
        }
        return false;
    }

    public List<Device> getAllByProductId(int productId) {
        List<Device> result = new ArrayList<>();
        for (var deviceEntry : devices.entrySet()) {
            Device device = deviceEntry.getValue();
            if (device.productId == productId) {
                result.add(device);
            }
        }
        return result;
    }

    public List<Device> getByProductIdAndFilter(int productId, int[] deviceIds) {
        List<Device> result = new ArrayList<>();
        for (var deviceEntry : devices.entrySet()) {
            Device device = deviceEntry.getValue();
            if (device.productId == productId && ArrayUtil.contains(deviceIds, device.id)) {
                result.add(device);
            }
        }
        return result;
    }

    public String getDeviceName(int deviceId) {
        Device device = getById(deviceId);
        if (device != null) {
            return truncateFileName(device.name);
        }
        return "";
    }

    public String getCSVDeviceName(int deviceId) {
        Device device = getById(deviceId);
        if (device == null) {
            return String.valueOf(deviceId);
        }

        String deviceName = device.name;
        if (deviceName == null || deviceName.isEmpty()) {
            return String.valueOf(deviceId);
        }

        return StringUtils.escapeCSV(deviceName);
    }

    public List<Device> getDevicesOwnedByUser(String ownerEmail) {
        List<Device> result = new ArrayList<>();
        for (Device device : devices.values()) {
            if (device.hasOwner(ownerEmail)) {
                result.add(device);
            }
        }
        return result;
    }

    public DeviceValue getDeviceTokenValue(String token) {
        return deviceTokenManager.getTokenValueByToken(token);
    }

    public void deleteAllTokensForOrg(int orgId) {
        deviceTokenManager.cache.entrySet().removeIf(entry -> entry.getValue().belongsToOrg(orgId));
    }

    public boolean clearTemporaryTokens() {
        long now = System.currentTimeMillis();
        return deviceTokenManager.cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    public String assignNewToken(int orgId, String email, Product product, Device device) {
        return deviceTokenManager.assignNewToken(orgId, email, product, device);
    }

    public void assignNewToken(int orgId, String email, Product product, Device device, String newToken) {
        deviceTokenManager.assignNewToken(orgId, email, product, device, newToken);
    }

    public void assignTempToken(int orgId, User user, Device tempDevice) {
        deviceTokenManager.assignTempToken(new ProvisionTokenValue(orgId, user, tempDevice));
    }
}

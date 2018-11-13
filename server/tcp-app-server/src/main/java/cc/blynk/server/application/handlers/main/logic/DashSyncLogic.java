package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.mobile.MobileStateHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * Request state sync info for dashboard.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class DashSyncLogic {

    private DashSyncLogic() {
    }

    public static void messageReceived(Holder holder,
                                       ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        int dashId = Integer.parseInt(message.body);

        ctx.write(ok(message.id), ctx.voidPromise());
        Channel appChannel = ctx.channel();

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        //todo not very optimal, but ok for now
        List<Device> devices = holder.deviceDao.getDevicesOwnedByUser(state.user.email);
        for (Device device : devices) {
            if (dash.hasWidgetsByDeviceId(device.id)) {
                device.sendPinStorageSyncs(appChannel);
            }
        }

        ctx.flush();
    }

}

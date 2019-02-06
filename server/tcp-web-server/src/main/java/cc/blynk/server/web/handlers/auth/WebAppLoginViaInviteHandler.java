package cc.blynk.server.web.handlers.auth;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.auth.MobileGetServerHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.permissions.Role;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.protocol.model.messages.web.WebLoginViaInviteMessage;
import cc.blynk.server.core.session.mobile.OsType;
import cc.blynk.server.core.session.mobile.Version;
import cc.blynk.server.core.session.web.WebAppStateHolder;
import cc.blynk.server.internal.ReregisterChannelUtil;
import cc.blynk.server.internal.token.InviteToken;
import cc.blynk.server.web.handlers.WebAppHandler;
import cc.blynk.server.web.handlers.WebAppLogicHolder;
import cc.blynk.utils.IPUtils;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.handlers.DefaultExceptionHandler.handleGeneralException;
import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.server.internal.WebByteBufUtil.json;


/**
 * Handler responsible for managing apps login messages.
 * Initializes netty channel with a state tied with user.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
@ChannelHandler.Sharable
public final class WebAppLoginViaInviteHandler extends SimpleChannelInboundHandler<WebLoginViaInviteMessage> {

    private static final Logger log = LogManager.getLogger(WebAppLoginViaInviteHandler.class);

    private final Holder holder;
    private final WebAppLogicHolder webAppLogicHolder;

    public WebAppLoginViaInviteHandler(Holder holder, WebAppLogicHolder webAppLogicHolder) {
        this.holder = holder;
        this.webAppLogicHolder = webAppLogicHolder;
    }

    private static void cleanPipeline(DefaultChannelPipeline pipeline) {
        pipeline.removeIfExists(WebAppLoginHandler.class);
        pipeline.removeIfExists(UserNotLoggedHandler.class);
        pipeline.removeIfExists(MobileGetServerHandler.class);
        pipeline.removeIfExists(WebAppLoginViaInviteHandler.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebLoginViaInviteMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 2) {
            log.error("Wrong income message format.");
            ctx.writeAndFlush(json(message.id, "Wrong income message format."), ctx.voidPromise());
            return;
        }

        String token = messageParts[0];
        String password = messageParts[1];

        if (token == null || password == null) {
            log.error("Empty token or password field.");
            ctx.writeAndFlush(json(message.id, "Empty token or password field."), ctx.voidPromise());
            return;
        }

        InviteToken inviteToken = holder.tokensPool.getInviteToken(token);

        if (inviteToken == null) {
            log.error("Invitation expired or was used already for {}.", message.body);
            ctx.writeAndFlush(json(message.id, "Invitation expired or was used already."), ctx.voidPromise());
            return;
        }

        User user = holder.userDao.getByName(inviteToken.email);
        if (user == null) {
            log.error("User {} not found.", inviteToken);
            ctx.writeAndFlush(json(message.id, "User not found."), ctx.voidPromise());
            return;
        }

        user.pass = password;
        Organization org = holder.organizationDao.getOrgById(user.orgId);
        org.isActive = true;

        Version version = messageParts.length > 3
                ? new Version(messageParts[2], messageParts[3])
                : Version.UNKNOWN_VERSION;

        holder.userDao.createProjectForExportedApp(holder.timerWorker, user, inviteToken.appName);

        Role role = org.getRoleByIdOrThrow(user.roleId);
        login(ctx, message.id, user, role, version, token);
    }

    private void login(ChannelHandlerContext ctx, int messageId,
                       User user, Role role, Version version, String token) {
        DefaultChannelPipeline pipeline = (DefaultChannelPipeline) ctx.pipeline();
        cleanPipeline(pipeline);

        WebAppStateHolder appStateHolder = new WebAppStateHolder(user, role, new Version(OsType.WEB_SOCKET, 41));
        pipeline.addLast("AWebAppHandler", new WebAppHandler(holder.stats, webAppLogicHolder, appStateHolder));

        Channel channel = ctx.channel();

        Session session = holder.sessionDao.getOrCreateSessionForOrg(user.orgId, channel.eventLoop());
        if (session.initialEventLoop != channel.eventLoop()) {
            log.debug("Re registering websocket app channel. {}", ctx.channel());
            ReregisterChannelUtil.reRegisterChannel(ctx, session, channelFuture ->
                    completeLogin(channelFuture.channel(), session, user, messageId, version, token));
        } else {
            completeLogin(channel, session, user, messageId, version, token);
        }
    }

    private void completeLogin(Channel channel, Session session, User user, int msgId, Version version, String token) {
        user.lastLoggedIP = IPUtils.getIp(channel.remoteAddress());
        user.lastLoggedAt = System.currentTimeMillis();

        session.addWebChannel(channel);
        channel.writeAndFlush(ok(msgId), channel.voidPromise());
        holder.tokensPool.removeToken(token);

        log.info("{} orgId={} ({}) joined via invite.", user.email, user.orgId, version);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        handleGeneralException(ctx, cause);
    }

}

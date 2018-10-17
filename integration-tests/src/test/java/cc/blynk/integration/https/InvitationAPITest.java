package cc.blynk.integration.https;

import cc.blynk.integration.APIBaseTest;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.web.UserInviteDTO;
import cc.blynk.utils.SHA256Util;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static cc.blynk.integration.TestUtil.consumeText;
import static cc.blynk.integration.TestUtil.getDefaultHttpsClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
@Deprecated
@Ignore
public class InvitationAPITest extends APIBaseTest {

    @Test
    public void sendInvitationNotAuthorized() throws Exception {
        String email = "dmitriy@blynk.cc";
        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/1/invite");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("email", email));
        nvps.add(new BasicNameValuePair("name", "Dmitriy"));
        nvps.add(new BasicNameValuePair("role", "SUPER_ADMIN"));
        inviteReq.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void sendInvitationForNonExistingOrganization() throws Exception {
        login(admin.email, admin.pass);

        String email = "dmitriy@blynk.cc";
        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/100/invite");
        String data = new UserInviteDTO(email, "Dmitriy", 2).toString();
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"Cannot find organization with passed id.\"}}", consumeText(response));
        }
    }

    @Test
    public void userCantSendInvitation() throws Exception {
        String name = "user2@blynk.cc";
        String pass = "user2";
        User simpleUser = new User(name, SHA256Util.makeHash(pass, name), 1, "local", "127.0.0.1", false, 4);
        holder.userDao.add(simpleUser);

        login(simpleUser.email, simpleUser.pass);

        String email = "dmitriy@blynk.cc";
        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/1/invite");
        String data = new UserInviteDTO(email, "Dmitriy", 4).toString();
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"You are not allowed to perform this action.\"}}", consumeText(response));
        }
    }

    @Test
    public void userSendInvitationToExistingUser() throws Exception {
        login(admin.email, admin.pass);

        String email = "user@blynk.cc";
        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/1/invite");
        String data = new UserInviteDTO(email, "Dmitriy", 4).toString();
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"This user already exists.\"}}", consumeText(response));
        }
    }

    @Test
    public void sendInvitationFromSuperUser() throws Exception {
        login(admin.email, admin.pass);

        String email = "dmitriy@blynk.cc";
        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/1/invite");
        String data = new UserInviteDTO(email, "Dmitriy", 2).toString();
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(holder.mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), contains("/dashboard/invite?token="));
    }

    @Test
    public void invitationLandingWorks() throws Exception {
        HttpGet inviteGet = new HttpGet("https://localhost:" + properties.getHttpsPort() + "/dashboard" + "/invite?token=123");

        try (CloseableHttpResponse response = httpclient.execute(inviteGet)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void invitationFullFlow() throws Exception {
        login(admin.email, admin.pass);

        String email = "dmitriy@blynk.cc";
        String name = "Dmitriy";

        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/1/invite");
        String data = new UserInviteDTO(email, name, 2).toString();
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        ArgumentCaptor<String> bodyArgumentCapture = ArgumentCaptor.forClass(String.class);
        verify(holder.mailWrapper, timeout(1000).times(1)).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), bodyArgumentCapture.capture());
        String body = bodyArgumentCapture.getValue();

        String token = body.substring(body.indexOf("token=") + 6, body.indexOf("&"));
        assertEquals(32, token.length());

        verify(holder.mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), contains("/dashboard/invite?token="));

        HttpGet inviteGet = new HttpGet("https://localhost:" + properties.getHttpsPort() + "/dashboard" + "/invite?token=" + token);

        //we don't need cookie from initial login here
        CloseableHttpClient newHttpClient = getDefaultHttpsClient();

        try (CloseableHttpResponse response = newHttpClient.execute(inviteGet)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpPost loginRequest = new HttpPost(httpsAdminServerUrl + "/invite");
        List <NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("token", token));
        nvps.add(new BasicNameValuePair("password", "123"));
        loginRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = newHttpClient.execute(loginRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Header cookieHeader = response.getFirstHeader("set-cookie");
            assertNotNull(cookieHeader);
            assertTrue(cookieHeader.getValue().startsWith("session="));
            User user = JsonParser.parseUserFromString(consumeText(response));
            assertNotNull(user);
            assertEquals(email, user.email);
            assertEquals(name, user.name);
            assertEquals(2, user.roleId);
            assertEquals(1, user.orgId);
        }

        loginRequest = new HttpPost(httpsAdminServerUrl + "/invite");
        nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("token", token));
        nvps.add(new BasicNameValuePair("password", "123"));
        loginRequest.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse response = newHttpClient.execute(loginRequest)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"Your invitation expired or was used already. Please request new one.\"}}", consumeText(response));
        }
    }


}

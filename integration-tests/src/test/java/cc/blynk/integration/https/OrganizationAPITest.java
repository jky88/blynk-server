package cc.blynk.integration.https;

import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.auth.UserStatus;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.UserInvite;
import cc.blynk.server.http.web.model.WebEmail;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.SHA256Util;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizationAPITest extends APIBaseTest {

    @Test
    public void getOrgNotAuthorized() throws Exception {
        HttpGet getOwnProfile = new HttpGet(httpsAdminServerUrl + "/organization/1");
        try (CloseableHttpResponse response = httpclient.execute(getOwnProfile)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getAllOrganizationsForSuperAdmin() throws Exception {
        login(admin.email, admin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(1, orgs.length);
        }
    }

    @Test
    public void getAllOrganizationsForSuperAdmin2() throws Exception {
        login(admin.email, admin.pass);

        holder.organizationDao.create(new Organization("Blynk Inc. 2", "Europe/Kiev", "/static/logo2.png", true, 1));

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(2, orgs.length);
        }
    }

    @Test
    public void getOrganizationWithRegularUser() throws Exception {
        login(regularUser.email, regularUser.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals("Blynk Inc.", fromApi.name);
            assertEquals("Europe/Kiev", fromApi.tzName);
        }
    }

    @Test
    public void getOrganization() throws Exception {
        login(admin.email, admin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals("Blynk Inc.", fromApi.name);
            assertEquals("Europe/Kiev", fromApi.tzName);
        }
    }

    @Test
    public void canInviteUser() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/canInviteUser");
        req.setEntity(new StringEntity(new WebEmail("xxx@gmail.com").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        req = new HttpPost(httpsAdminServerUrl + "/organization/canInviteUser");
        req.setEntity(new StringEntity(new WebEmail("user@blynk.cc").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void updateOrganizationNotAllowedForRegularUser() throws Exception {
        login(regularUser.email, regularUser.pass);

        Organization organization = new Organization("1", "2", "/static/logo.png", false, 1);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createOrganizationAllowedForRegularAdmin() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false, 1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getOrganizationRestrictedForSpecificAdmin() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false, -1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
        }


        String email = "dmitriy@blynk.cc";
        String name = "Dmitriy";
        Role role = Role.ADMIN;

        HttpPost inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/2/invite");
        String data = JsonParser.mapper.writeValueAsString(new UserInvite(email, name, role));
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        ArgumentCaptor<String> bodyArgumentCapture = ArgumentCaptor.forClass(String.class);
        verify(mailWrapper, timeout(1000).times(1)).sendHtml(eq(email), eq("Invitation to Blynk dashboard."), bodyArgumentCapture.capture());
        String body = bodyArgumentCapture.getValue();

        String token = body.substring(body.indexOf("token=") + 6, body.indexOf("&"));
        assertEquals(32, token.length());

        verify(mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk dashboard."), contains(rootPath + "#/invite?token="));

        HttpGet inviteGet = new HttpGet("https://localhost:" + httpsPort + rootPath + "#/invite?token=" + token);

        //we don't need cookie from initial login here
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(initUnsecuredSSLContext(), new MyHostVerifier());
        CloseableHttpClient newHttpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        try (CloseableHttpResponse response = newHttpClient.execute(inviteGet)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpPost loginRequest = new HttpPost(httpsAdminServerUrl + "/invite");
        List<NameValuePair> nvps = new ArrayList<>();
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
            assertEquals(role, user.role);
            assertEquals(2, user.orgId);
        }

        HttpGet getOrgs = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = newHttpClient.execute(getOrgs)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(1, orgs.length);
            assertEquals(2, orgs[0].id);
            assertEquals(1, orgs[0].parentId);
        }
    }

    @Test
    public void organizationNotAllowedToCreateSubOrgs() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false, 1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
        }

        User regularAdmin = new User("new@hgmail.com", SHA256Util.makeHash("123", "new@hgmail.com"), AppName.BLYNK, "local", false, Role.ADMIN);
        regularAdmin.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        regularAdmin.status = UserStatus.Active;
        regularAdmin.orgId = 2;
        holder.userDao.add(regularAdmin);

        login(regularAdmin.email, regularAdmin.pass);

        organization = new Organization("My Org2", "Some TimeZone", "/static/logo.png", false, 1);

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void deleteOrganizationNotAllowedForRegularAdmin() throws Exception {
        holder.organizationDao.create(new Organization("Blynk Inc.", "Europe/Kiev", "/static/logo.png", false, 1));

        login(regularAdmin.email, regularAdmin.pass);

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false, 1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }
    }

    @Test
    public void updateOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("1", "2", "/static/logo.png", false, 1);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }
    }

    @Test
    public void deleteOrganization() throws Exception {
        createOrganization();

        HttpDelete req = new HttpDelete(httpsAdminServerUrl + "/organization/1");

        //do not allow to delete initial org
        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void regularAdminCantDeleteOtherOrganization() throws Exception {
        createOrganization();

        //we don't need cookie from initial login here
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(initUnsecuredSSLContext(), new MyHostVerifier());
        CloseableHttpClient newHttpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        login(newHttpClient,  httpsAdminServerUrl, regularAdmin.email, regularAdmin.pass);

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = newHttpClient.execute(req2)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getUsersFromOrg() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(2, fromApi.length);
            for (User user : fromApi) {
                assertNotNull(user);
                assertNull(user.pass);
                assertNotEquals(regularAdmin.email, user.email);
            }
        }
    }

    @Test
    public void deleteRegularUserFromOrg() throws Exception {
        login(regularAdmin.email, regularAdmin.pass);


        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
            for (User user : fromApi) {
                assertNotEquals("user@blynk.cc", user.email);
                assertNotEquals(regularAdmin.email, user.email);
            }
        }
    }

    @Test
    public void cantDeleteSuperAdmin() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{admin.email});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void regularUserCantDelete() throws Exception {
        login(regularUser.email, regularUser.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.mapper.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

    }

    @Test
    public void updateAccountRole() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/update");
        String body = JsonParser.mapper.writeValueAsString(new UserInvite("user@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.mapper.readValue(consumeText(response), User[].class);
            assertNotNull(fromApi);
            assertEquals(3, fromApi.length);
            for (User user : fromApi) {
                if (user.email.equals("user@blynk.cc")) {
                    assertEquals(Role.ADMIN, user.role);
                }
            }
        }
    }

    @Test
    public void updateAccountRoleForNonExistingUser() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/update");
        String body = JsonParser.mapper.writeValueAsString(new UserInvite("userzzz@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

}

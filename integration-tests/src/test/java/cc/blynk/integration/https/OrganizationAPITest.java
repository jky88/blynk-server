package cc.blynk.integration.https;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.auth.UserStatus;
import cc.blynk.server.core.model.device.ConnectionType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.UserInvite;
import cc.blynk.server.core.model.web.product.MetaField;
import cc.blynk.server.core.model.web.product.Product;
import cc.blynk.server.core.model.web.product.WebDashboard;
import cc.blynk.server.core.model.web.product.metafields.AddressMetaField;
import cc.blynk.server.core.model.web.product.metafields.ContactMetaField;
import cc.blynk.server.core.model.web.product.metafields.CoordinatesMetaField;
import cc.blynk.server.core.model.web.product.metafields.CostMetaField;
import cc.blynk.server.core.model.web.product.metafields.MeasurementUnit;
import cc.blynk.server.core.model.web.product.metafields.MeasurementUnitMetaField;
import cc.blynk.server.core.model.web.product.metafields.NumberMetaField;
import cc.blynk.server.core.model.web.product.metafields.RangeTimeMetaField;
import cc.blynk.server.core.model.web.product.metafields.SwitchMetaField;
import cc.blynk.server.core.model.web.product.metafields.TextMetaField;
import cc.blynk.server.core.model.web.product.metafields.TimeMetaField;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.web.WebBarGraph;
import cc.blynk.server.http.web.dto.EmailDTO;
import cc.blynk.server.http.web.dto.ProductAndOrgIdDTO;
import cc.blynk.utils.SHA256Util;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static cc.blynk.utils.AppNameUtil.BLYNK;
import static java.time.LocalTime.ofSecondOfDay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
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
            assertEquals(0, orgs.length);
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
            assertEquals(1, orgs.length);
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

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/canInviteUser");
        req.setEntity(new StringEntity(new EmailDTO("xxx@gmail.com").toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        req = new HttpPost(httpsAdminServerUrl + "/organization/1/canInviteUser");
        req.setEntity(new StringEntity(new EmailDTO("user@blynk.cc").toString(), ContentType.APPLICATION_JSON));

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
        String data = JsonParser.MAPPER.writeValueAsString(new UserInvite(email, name, role));
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        ArgumentCaptor<String> bodyArgumentCapture = ArgumentCaptor.forClass(String.class);
        verify(mailWrapper, timeout(1000).times(1)).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), bodyArgumentCapture.capture());
        String body = bodyArgumentCapture.getValue();

        String token = body.substring(body.indexOf("token=") + 6, body.indexOf("&"));
        assertEquals(32, token.length());

        verify(mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), contains("/dashboard" + "/invite?token="));

        HttpGet inviteGet = new HttpGet("https://localhost:" + httpsPort + "/dashboard" + "/invite?token=" + token);

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
            assertEquals(0, orgs.length);
        }
    }

    @Test
    public void createOrgInviteUserAndRemoveOrg() throws Exception {
        login(admin.email, admin.pass);

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
        String data = JsonParser.MAPPER.writeValueAsString(new UserInvite(email, name, role));
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        ArgumentCaptor<String> bodyArgumentCapture = ArgumentCaptor.forClass(String.class);
        verify(mailWrapper, timeout(1000).times(1)).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), bodyArgumentCapture.capture());
        String body = bodyArgumentCapture.getValue();

        String token = body.substring(body.indexOf("token=") + 6, body.indexOf("&"));
        assertEquals(32, token.length());

        verify(mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), contains("/dashboard" + "/invite?token="));
        reset(mailWrapper);

        HttpGet inviteGet = new HttpGet("https://localhost:" + httpsPort + "/dashboard" + "/invite?token=" + token);

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

        HttpDelete deleteOrg = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(deleteOrg)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }



        Organization organization3 = new Organization("My Org", "Some TimeZone", "/static/logo.png", false, -1);

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(3, fromApi.id);
            assertEquals(1, fromApi.parentId);
        }

        inviteReq = new HttpPost(httpsAdminServerUrl + "/organization/3/invite");
        data = JsonParser.MAPPER.writeValueAsString(new UserInvite(email, name, role));
        inviteReq.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(inviteReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        bodyArgumentCapture = ArgumentCaptor.forClass(String.class);
        verify(mailWrapper, timeout(1000).times(1)).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), bodyArgumentCapture.capture());
        body = bodyArgumentCapture.getValue();

        token = body.substring(body.indexOf("token=") + 6, body.indexOf("&"));
        assertEquals(32, token.length());

        verify(mailWrapper).sendHtml(eq(email), eq("Invitation to Blynk Inc. dashboard."), contains("/dashboard" + "/invite?token="));

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

        User regularAdmin = new User("new@hgmail.com", SHA256Util.makeHash("123", "new@hgmail.com"), BLYNK, "local", "127.0.0.1", false, Role.ADMIN);
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
        holder.organizationDao.create(new Organization("Blynk Inc. 2", "Europe/Kiev", "/static/logo.png", false, 1));

        login(regularAdmin.email, regularAdmin.pass);

        HttpDelete req2 = new HttpDelete(httpsAdminServerUrl + "/organization/2");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);

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
    public void organizationListReturnsOnlySubOrganizations() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);

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

        HttpGet getOrgs = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = httpclient.execute(getOrgs)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(1, orgs.length);
            Organization fromApi = orgs[0];
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }
    }

    @Test
    public void organizationListReturnsOnlySubOrganizations2() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);

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

        HttpGet getOrgs = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = httpclient.execute(getOrgs)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(1, orgs.length);
            Organization fromApi = orgs[0];
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
        }

        String name = "newadmin@blynk.cc";
        String pass = "admin";
        User newadmin = new User(name, SHA256Util.makeHash(pass, name), BLYNK, "local", "127.0.0.1", false, Role.SUPER_ADMIN);
        newadmin.orgId = 2;
        newadmin.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        newadmin.status = UserStatus.Active;
        holder.userDao.add(newadmin);

        CloseableHttpClient newHttpClient = newHttpClient();

        login(newHttpClient, httpsAdminServerUrl, newadmin.email, newadmin.pass);

        getOrgs = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = newHttpClient.execute(getOrgs)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(0, orgs.length);
        }
    }

    @Test
    public void doNotAllowCreateOrganizationForSubOrgThatDoesntSupportThis() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);
        organization.canCreateOrgs = false;

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertEquals(organization.canCreateOrgs, fromApi.canCreateOrgs);
        }

        String name = "newadmin@blynk.cc";
        String pass = "admin";
        User newadmin = new User(name, SHA256Util.makeHash(pass, name), BLYNK, "local", "127.0.0.1", false, Role.SUPER_ADMIN);
        newadmin.orgId = 2;
        newadmin.profile.dashBoards = new DashBoard[] {
                new DashBoard()
        };
        newadmin.status = UserStatus.Active;
        holder.userDao.add(newadmin);

        CloseableHttpClient newHttpClient = newHttpClient();

        login(newHttpClient, httpsAdminServerUrl, newadmin.email, newadmin.pass);

        organization = new Organization("My Org 2 ", "Some TimeZone", "/static/logo.png", false);

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = newHttpClient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"This organization cannot have sub organizations.\"}}", consumeText(response));
        }
    }

    @Test
    public void createOrganizationWithSameNameNotAllowed() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("Blynk Inc.", "Some TimeZone", "/static/logo.png", false, 1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"Organization with this name already exists.\"}}", consumeText(response));
        }
    }

    @Test
    public void updateOrganizationWithSameNameNotAllowed() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("Blynk Inc. 2", "Some TimeZone", "/static/logo.png", false, 1);

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            organization = JsonParser.parseOrganization(consumeText(response));
        }

        HttpPost post = new HttpPost(httpsAdminServerUrl + "/organization/2");
        post.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        organization.name = "Blynk Inc.";

        post = new HttpPost(httpsAdminServerUrl + "/organization/2");
        post.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(post)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
            assertEquals("{\"error\":{\"message\":\"Organization with this name already exists.\"}}", consumeText(response));
        }
    }

    @Test
    public void createOrganizationWithProductsAssigned() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";

        product.metaFields = new MetaField[] {
                new TextMetaField(1, "My Farm", Role.ADMIN, false, "Farm of Smith"),
                new SwitchMetaField(1, "My Farm", Role.ADMIN, false, "0", "1", "Farm of Smith"),
                new RangeTimeMetaField(2, "Farm of Smith", Role.ADMIN, false, ofSecondOfDay(60), ofSecondOfDay(120)),
                new NumberMetaField(3, "Farm of Smith", Role.ADMIN, false, 10.222),
                new MeasurementUnitMetaField(4, "Farm of Smith", Role.ADMIN, false, MeasurementUnit.Celsius, "36"),
                new CostMetaField(5, "Farm of Smith", Role.ADMIN, false, Currency.getInstance("USD"), 9.99, 1, MeasurementUnit.Gallon),
                new ContactMetaField(6, "Farm of Smith", Role.ADMIN, false, "Tech Support",
                        "Dmitriy", false, "Dumanskiy", false, "dmitriy@blynk.cc", false,
                        "+38063673333",  false, "My street", false,
                        "Ukraine", false,
                        "Kyiv", false, "Ukraine", false, "03322", false, false),
                new AddressMetaField(7, "Farm of Smith", Role.ADMIN, false, "My street", false,
                        "San Diego", false, "CA", false, "03322", false, "US", false, false),
                new CoordinatesMetaField(8, "Farm Location", Role.ADMIN, false, 22.222, 23.333),
                new TimeMetaField(9, "Some Time", Role.ADMIN, false, new Date())
        };

        product.dataStreams = new DataStream[] {
                new DataStream(0, (byte) 100, false, false, PinType.VIRTUAL, null, 0, 50, "Temperature", MeasurementUnit.Celsius)
        };

        product.webDashboard = new WebDashboard(new Widget[]{
                new WebBarGraph()
        });

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals(product.name, fromApi.name);
            assertEquals(product.description, fromApi.description);
            assertEquals(product.boardType, fromApi.boardType);
            assertEquals(product.connectionType, fromApi.connectionType);
            assertEquals(product.logoUrl, fromApi.logoUrl);
            assertEquals(0, fromApi.version);
            assertNotEquals(0, fromApi.lastModifiedTs);
            assertNotNull(fromApi.dataStreams);
            assertEquals(1, fromApi.dataStreams.length);
            assertEquals(100, fromApi.dataStreams[0].pin);
            assertNotNull(fromApi.metaFields);
            assertEquals(10, fromApi.metaFields.length);
            assertNotNull(fromApi.webDashboard);
        }

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);
        organization.selectedProducts = new int[]{1};

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);

            Product productFromApi = fromApi.products[0];
            assertEquals(2, productFromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertArrayEquals(product.dataStreams, productFromApi.dataStreams);
            assertEquals(1, productFromApi.dataStreams.length);
            assertEquals(100, productFromApi.dataStreams[0].pin);
            assertNotNull(productFromApi.metaFields);
            assertEquals(10, productFromApi.metaFields.length);
            assertNotNull(productFromApi.webDashboard);
            assertNotNull(productFromApi.webDashboard.widgets);
            assertEquals(1, productFromApi.webDashboard.widgets.length);
            assertArrayEquals(product.webDashboard.widgets, productFromApi.webDashboard.widgets);
        }

        Organization organization2 = new Organization("My Org2", "Some TimeZone", "/static/logo.png", false);
        organization2.selectedProducts = new int[]{1};

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization2.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(3, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals("My Org2", fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, fromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(10, productFromApi.metaFields.length);
        }
    }

    @Test
    public void createOrganizationWithProductsAssignedAddNewProduct() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";
        product.metaFields = new MetaField[] {
                new TextMetaField(1, "My Farm", Role.ADMIN, false, "Farm of Smith")
        };
        product.dataStreams = new DataStream[] {
                new DataStream(0, (byte) 0, false, false, PinType.VIRTUAL, null, 0, 50, "Temperature", MeasurementUnit.Celsius)
        };

        HttpPut createProductReq = new HttpPut(httpsAdminServerUrl + "/product");
        createProductReq.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(createProductReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        Product product2 = new Product();
        product2.name = "My product2";
        product2.description = "Description";
        product2.boardType = "ESP8266";
        product2.connectionType = ConnectionType.WI_FI;
        product2.logoUrl = "/static/logo.png";
        product2.metaFields = new MetaField[] {
                new TextMetaField(1, "My Farm", Role.ADMIN, false, "Farm of Smith")
        };
        product2.dataStreams = new DataStream[] {
                new DataStream(0, (byte) 0, false, false, PinType.VIRTUAL, null, 0, 50, "Temperature", MeasurementUnit.Celsius)
        };

        HttpPut req2 = new HttpPut(httpsAdminServerUrl + "/product");
        req2.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product2).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
        }

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);
        organization.selectedProducts = new int[]{1};

        HttpPut createOrgReq = new HttpPut(httpsAdminServerUrl + "/organization");
        createOrgReq.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(createOrgReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);
            assertArrayEquals(new int[] {1}, fromApi.selectedProducts);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, productFromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(1, productFromApi.metaFields.length);
            organization = fromApi;
        }

        organization.selectedProducts = new int[]{1, 2};

        HttpPost updateOrgReq = new HttpPost(httpsAdminServerUrl + "/organization/2");
        updateOrgReq.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(updateOrgReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(2, fromApi.products.length);
            assertArrayEquals(new int[] {1, 2}, fromApi.selectedProducts);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, productFromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(1, productFromApi.metaFields.length);

            Product productFromApi2 = fromApi.products[1];
            assertEquals(4, productFromApi2.id);
            assertEquals(product2.name, productFromApi2.name);
            assertEquals(product2.description, productFromApi2.description);
            assertEquals(product2.boardType, productFromApi2.boardType);
            assertEquals(product2.connectionType, productFromApi2.connectionType);
            assertEquals(product2.logoUrl, productFromApi2.logoUrl);
            assertEquals(0, productFromApi2.version);
            assertNotEquals(0, productFromApi2.lastModifiedTs);
            assertNotNull(productFromApi2.dataStreams);
            assertNotNull(productFromApi2.metaFields);
            assertEquals(1, productFromApi2.metaFields.length);
        }

    }

    @Test
    public void createOrganizationWithProductsAssignedAndRemoveProduct() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";
        product.metaFields = new MetaField[] {
                new TextMetaField(1, "My Farm", Role.ADMIN, false, "Farm of Smith")
        };
        product.dataStreams = new DataStream[] {
                new DataStream(0, (byte) 0, false, false, PinType.VIRTUAL, null, 0, 50, "Temperature", MeasurementUnit.Celsius)
        };

        HttpPut createProductReq = new HttpPut(httpsAdminServerUrl + "/product");
        createProductReq.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(createProductReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        Product product2 = new Product();
        product2.name = "My product2";
        product2.description = "Description";
        product2.boardType = "ESP8266";
        product2.connectionType = ConnectionType.WI_FI;
        product2.logoUrl = "/static/logo.png";
        product2.metaFields = new MetaField[] {
                new TextMetaField(1, "My Farm", Role.ADMIN, false, "Farm of Smith")
        };
        product2.dataStreams = new DataStream[] {
                new DataStream(0, (byte) 0, false, false, PinType.VIRTUAL, null, 0, 50, "Temperature", MeasurementUnit.Celsius)
        };

        HttpPut req2 = new HttpPut(httpsAdminServerUrl + "/product");
        req2.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product2).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
        }

        Organization organization = new Organization("My Org", "Some TimeZone", "/static/logo.png", false);
        organization.selectedProducts = new int[]{1, 2};

        HttpPut createOrgReq = new HttpPut(httpsAdminServerUrl + "/organization");
        createOrgReq.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(createOrgReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(2, fromApi.products.length);
            assertArrayEquals(new int[] {1, 2}, fromApi.selectedProducts);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, productFromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(1, productFromApi.parentId);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(1, productFromApi.metaFields.length);

            productFromApi = fromApi.products[1];
            assertEquals(4, productFromApi.id);
            assertEquals("My product2", productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(2, productFromApi.parentId);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(1, productFromApi.metaFields.length);

            organization = fromApi;
        }

        organization.selectedProducts = new int[]{1};

        HttpPost updateOrgReq = new HttpPost(httpsAdminServerUrl + "/organization/2");
        updateOrgReq.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(updateOrgReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertEquals(organization.name, fromApi.name);
            assertEquals(organization.tzName, fromApi.tzName);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);
            assertArrayEquals(new int[] {1}, fromApi.selectedProducts);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, productFromApi.id);
            assertEquals(product.name, productFromApi.name);
            assertEquals(product.description, productFromApi.description);
            assertEquals(product.boardType, productFromApi.boardType);
            assertEquals(product.connectionType, productFromApi.connectionType);
            assertEquals(product.logoUrl, productFromApi.logoUrl);
            assertEquals(1, productFromApi.parentId);
            assertEquals(0, productFromApi.version);
            assertNotEquals(0, productFromApi.lastModifiedTs);
            assertNotNull(productFromApi.dataStreams);
            assertNotNull(productFromApi.metaFields);
            assertEquals(1, productFromApi.metaFields.length);
        }
    }

    @Test
    public void createSubOrganizationWithProductsAssignedAndDeviceCountIsCorrect() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        Organization organization = new Organization("Sub Org", "Some TimeZone", "/static/logo.png", false);
        organization.selectedProducts = new int[]{1};

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);

            Product productFromApi = fromApi.products[0];
            assertEquals(2, productFromApi.id);
        }

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = 2;

        HttpPut httpPut = new HttpPut(httpsAdminServerUrl + "/devices/2");
        httpPut.setEntity(new StringEntity(newDevice.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPut)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet getProducts = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(getProducts)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
            product = fromApi[0];
            assertNotNull(product);
            assertEquals(1, product.id);
            assertEquals(1, product.deviceCount);
        }
    }

    @Test
    public void createSubOrganizationWithProductsAssignedAndDeviceCountIsCorrectWithMultipleOrgs() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        Organization organization = new Organization("Sub Org", "Some TimeZone", "/static/logo.png", false);
        organization.selectedProducts = new int[]{1};

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);

            Product productFromApi = fromApi.products[0];
            assertEquals(2, productFromApi.id);
        }

        Organization organization2 = new Organization("Sub Org 2", "Some TimeZone", "/static/logo.png", false);
        organization2.selectedProducts = new int[]{1};

        req = new HttpPut(httpsAdminServerUrl + "/organization");
        req.setEntity(new StringEntity(organization2.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization fromApi = JsonParser.parseOrganization(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(3, fromApi.id);
            assertEquals(1, fromApi.parentId);
            assertNotNull(fromApi.products);
            assertEquals(1, fromApi.products.length);

            Product productFromApi = fromApi.products[0];
            assertEquals(3, productFromApi.id);
        }

        Device newDevice = new Device();
        newDevice.name = "My New Device";
        newDevice.productId = 2;

        HttpPut httpPut = new HttpPut(httpsAdminServerUrl + "/devices/2");
        httpPut.setEntity(new StringEntity(newDevice.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPut)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        Device newDevice2 = new Device();
        newDevice2.name = "My New Device";
        newDevice2.productId = 3;

        httpPut = new HttpPut(httpsAdminServerUrl + "/devices/3");
        httpPut.setEntity(new StringEntity(newDevice2.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPut)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        Device newDevice3 = new Device();
        newDevice3.name = "My New Device";
        newDevice3.productId = 3;

        httpPut = new HttpPut(httpsAdminServerUrl + "/devices/3");
        httpPut.setEntity(new StringEntity(newDevice3.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(httpPut)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }


        HttpGet getProducts = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(getProducts)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
            product = fromApi[0];
            assertNotNull(product);
            assertEquals(1, product.id);
            assertEquals(3, product.deviceCount);
        }

        HttpGet getOrgs = new HttpGet(httpsAdminServerUrl + "/organization");

        try (CloseableHttpResponse response = httpclient.execute(getOrgs)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Organization[] orgs = JsonParser.readAny(consumeText(response), Organization[].class);
            assertNotNull(orgs);
            assertEquals(2, orgs.length);

            Organization org2 = orgs[0];
            assertNotNull(org2);
            assertEquals(2, org2.id);
            assertEquals(1, org2.products[0].deviceCount);

            Organization org3 = orgs[1];
            assertNotNull(org3);
            assertEquals(3, org3.id);
            assertEquals(2, org3.products[0].deviceCount);
        }
    }

    @Test
    public void getExistingLocationsForOrganization() throws Exception {
        login(admin.email, admin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/organization/1/locations");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String[] fromApi = JsonParser.MAPPER.readValue(consumeText(response), String[].class);
            assertNotNull(fromApi);
            assertEquals(0, fromApi.length);
        }

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";
        product.metaFields = new MetaField[] {
                new TextMetaField(1, "Location Name", Role.ADMIN, true, "Kyiv")
        };

        HttpPut createProductReq = new HttpPut(httpsAdminServerUrl + "/product");
        createProductReq.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(createProductReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        Product product2 = new Product();
        product2.name = "My product2";
        product2.description = "Description";
        product2.boardType = "ESP8266";
        product2.connectionType = ConnectionType.WI_FI;
        product2.logoUrl = "/static/logo.png";
        product2.metaFields = new MetaField[] {
                new TextMetaField(1, "Location Name", Role.ADMIN, true, "Kyiv 2")
        };

        HttpPut req2 = new HttpPut(httpsAdminServerUrl + "/product");
        req2.setEntity(new StringEntity(new ProductAndOrgIdDTO(1, product2).toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
        }

        req = new HttpGet(httpsAdminServerUrl + "/organization/1/locations");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String responseString = consumeText(response);
            String[] fromApi = JsonParser.MAPPER.readValue(responseString, String[].class);
            assertNotNull(fromApi);
            assertEquals(2, fromApi.length);
            assertEquals("Kyiv", fromApi[0]);
            assertEquals("Kyiv 2", fromApi[1]);
        }
    }

    @Test
    public void updateOrganization() throws Exception {
        login(admin.email, admin.pass);

        Organization organization = new Organization("1", "2", "/static/logo.png", false, 1);
        organization.id = 1;

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

        CloseableHttpClient newHttpClient = newHttpClient();

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
            User[] fromApi = JsonParser.MAPPER.readValue(consumeText(response), User[].class);
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
        String body = JsonParser.MAPPER.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.MAPPER.readValue(consumeText(response), User[].class);
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
        String body = JsonParser.MAPPER.writeValueAsString(new String[]{admin.email});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void regularUserCantDelete() throws Exception {
        login(regularUser.email, regularUser.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/delete");
        String body = JsonParser.MAPPER.writeValueAsString(new String[]{"user@blynk.cc"});
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(403, response.getStatusLine().getStatusCode());
        }

    }

    @Test
    public void updateAccountRole() throws Exception {
        login(admin.email, admin.pass);

        HttpPost req = new HttpPost(httpsAdminServerUrl + "/organization/1/users/update");
        String body = JsonParser.MAPPER.writeValueAsString(new UserInvite("user@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/organization/1/users");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            User[] fromApi = JsonParser.MAPPER.readValue(consumeText(response), User[].class);
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
        String body = JsonParser.MAPPER.writeValueAsString(new UserInvite("userzzz@blynk.cc", "123", Role.ADMIN));
        req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));


        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

}

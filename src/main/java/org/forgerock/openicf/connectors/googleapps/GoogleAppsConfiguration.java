/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/CDDL-1.0
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://opensource.org/licenses/CDDL-1.0
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openicf.connectors.googleapps;

import java.security.GeneralSecurityException;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.licensing.Licensing;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.identityconnectors.common.logging.Log;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the GoogleApps Connector.
 *
 */
public class GoogleAppsConfiguration extends AbstractConfiguration implements StatefulConfiguration {

    private String domain = null;
    private String productId = null;
    private String skuId = null;
    private Boolean autoAddLicense = false;
    /**
     * Client identifier issued to the client during the registration process.
     */
    private String clientId;
    /**
     * Client secret or {@code null} for none.
     */
    private GuardedString clientSecret = null;
    private GuardedString refreshToken = null;
    private static final Log logger = Log.getLog(GoogleAppsConfiguration.class);

    /**
     * Constructor.
     */
    public GoogleAppsConfiguration() {
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "domain.display",
    groupMessageKey = "basic.group", helpMessageKey = "domain.help", required = true,
    confidential = false)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

   @ConfigurationProperty(order = 2, displayMessageKey = "productid.display",
    groupMessageKey = "basic.group", helpMessageKey = "productid.help", required = true,
    confidential = false)
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

   @ConfigurationProperty(order = 3, displayMessageKey = "skuid.display",
    groupMessageKey = "basic.group", helpMessageKey = "skuid.help", required = true,
    confidential = false)
    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

  @ConfigurationProperty(order = 4, displayMessageKey = "audoadd.display",
    groupMessageKey = "basic.group", helpMessageKey = "audoadd.help", required = true,
    confidential = false)
    public Boolean getAutoAddLicense() {
        return autoAddLicense;
    }

    public void setAutoAddLicense(Boolean autoAddLicense) {
        this.autoAddLicense = autoAddLicense;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "clientid.display",
    groupMessageKey = "basic.group", helpMessageKey = "clientid.help", required = true,
    confidential = false)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "clientsecret.display",
    groupMessageKey = "basic.group", helpMessageKey = "clientsecret.help", required = true,
    confidential = true)
    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }

    @ConfigurationProperty(order = 7, displayMessageKey = "refreshtoken.display",
    groupMessageKey = "basic.group", helpMessageKey = "refreshtoken.help", required = true,
    confidential = true)
    public GuardedString getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(GuardedString refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtil.isBlank(domain)) {
            throw new IllegalArgumentException("Domain cannot be null or empty.");
        }
        if (StringUtil.isBlank(productId)) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        if (StringUtil.isBlank(skuId)) {
            throw new IllegalArgumentException("SKU ID cannot be null or empty.");
        }
        if (StringUtil.isBlank(clientId)) {
            throw new IllegalArgumentException("Client Id cannot be null or empty.");
        }
        if (null == clientSecret) {
            throw new IllegalArgumentException("Client Secret cannot be null or empty.");
        }
        if (null == refreshToken) {
            throw new IllegalArgumentException("Refresh Token cannot be null or empty.");
        }
    }
    private Credential credential = null;

    public Credential getGoogleCredential() {
        if (null == credential) {
            synchronized (this) {
                if (null == credential) {
                    System.setProperty("https.protocols", "TLSv1.2");
                    credential =
                            new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                            .setTransport(HTTP_TRANSPORT)
                            .setJsonFactory(JSON_FACTORY)
                            .setTokenServerEncodedUrl(GoogleOAuthConstants.TOKEN_SERVER_URL)
                            .setClientAuthentication(
                            new ClientParametersAuthentication(getClientId(),
                            SecurityUtil.decrypt(getClientSecret())))
                            .build();
                    try {
                        credential.setRefreshToken(SecurityUtil.decrypt(getRefreshToken())).refreshToken();
                    } catch (IOException ex) {
                        logger.error("Token refresh error: ", ex);
                    }

                    getRefreshToken().access(new GuardedString.Accessor() {
                        @Override
                        public void access(char[] chars) {
                            credential.setRefreshToken(new String(chars));
                        }
                    });
                    directory =
                            new Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                            .setApplicationName("GoogleAppsConnector").build();
                    licensing =
                            new Licensing.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                            .setApplicationName("GoogleAppsConnector").build();

                }
            }
        }
        return credential;
    }

    @Override
    public void release() {
    }
    /**
     * Global instance of the HTTP transport.
     */
    private static final HttpTransport HTTP_TRANSPORT;
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    public Directory getDirectory() {
        getGoogleCredential();
        return directory;
    }

    public Licensing getLicensing() {
        getGoogleCredential();
        if (null == licensing) {
            throw new ConnectorException("Licensing is not enabled");
        }
        return licensing;
    }
    private Directory directory;
    private Licensing licensing;

    static {
        HttpTransport t = null;
        try {
            t = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            try {
                t = new NetHttpTransport.Builder().doNotValidateCertificate().build();
            } catch (GeneralSecurityException e1) {
            }
        }
        HTTP_TRANSPORT = t;
    }
}

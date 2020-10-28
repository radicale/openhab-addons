/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.openmotics.internal.api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link OpenMoticsApi} provides the API interface
 *
 * @author Alessandro Radicati - Initial contribution
 */

@NonNullByDefault
public class OpenMoticsApi {
    private ApiClient client;
    private DefaultApi api;
    private @Nullable String token;

    private HttpRequest.Builder authInterceptor(HttpRequest.Builder builder) {
        if (token == null) {
            return builder;
        } else {
            return builder.header("Authorization", "Bearer " + token);
        }
    }

    public OpenMoticsApi() {
        this((String) null);
    }

    public OpenMoticsApi(@Nullable String ipaddress) {
        client = new ApiClient();

        if (ipaddress != null) {
            client.setHost(ipaddress);
        }

        TrustManager[] tm = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate @Nullable [] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate @Nullable [] certs,
                    @Nullable String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate @Nullable [] certs,
                    @Nullable String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tm, new java.security.SecureRandom());
            SSLParameters sp = new SSLParameters();
            sp.setProtocols(new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" });
            sp.setEndpointIdentificationAlgorithm(null);
            client.setHttpClientBuilder(HttpClient.newBuilder().sslContext(sc).sslParameters(sp));
        } catch (Exception e) {
        }

        // Disable host name check
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        // Install authentication hook
        client.setRequestInterceptor(builder -> authInterceptor(builder));

        api = new DefaultApi(client);
    }

    public DefaultApi getApi() {
        return api;
    }

    public void setApiKey(@Nullable String token) {
        this.token = token;
    }
}

package com.qmetric.penfold.client.app.support;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class ClientFactory
{
    private static final int CONNECT_TIMEOUT = 60000;

    private static final int READ_TIMEOUT = 60000;

    public static HttpClient createHttpClient(Credentials credentials)
    {
        final PlainConnectionSocketFactory socketFactory = PlainConnectionSocketFactory.getSocketFactory();
        final SSLConnectionSocketFactory secureSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createDefault());

        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register("http", socketFactory);
        registryBuilder.register("https", secureSocketFactory);

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registryBuilder.build());
        connectionManager.setDefaultMaxPerRoute(10);

        final HttpClientBuilder clientBuilder = HttpClients.custom();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .build();

        clientBuilder.setDefaultRequestConfig(requestConfig);

        //Don't always need credentials for example when doing a ping check
        if (credentials != null)
        {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(credentials.username, credentials.password));
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        return clientBuilder.build();
    }
}

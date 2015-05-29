package com.qmetric.penfold.client.app.support;

import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class ClientFactory
{
    public static Client create()
    {
        return ClientBuilder.newClient()
                .property(ClientProperties.READ_TIMEOUT, 60000)
                .property(ClientProperties.CONNECT_TIMEOUT, 60000);
    }
}

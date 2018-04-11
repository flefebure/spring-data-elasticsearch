/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Mohsin Husen
 */
public class Utils {
    final static String port = "9300";

    public static Client getNodeClient() {
        try {
            return new PreBuiltTransportClient(Settings.builder()
                    .build()).addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), Integer.valueOf(port)));
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to connect to localhost cluster ar port " + port);
        }
    }
}

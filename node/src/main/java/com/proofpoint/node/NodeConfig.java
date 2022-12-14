/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.node;

import com.google.common.net.InetAddresses;
import com.proofpoint.configuration.Config;
import com.proofpoint.configuration.DefunctConfig;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.net.InetAddress;

@DefunctConfig({"http-server.ip", "jetty.ip", "node.binary-spec", "node.config-spec"})
public class NodeConfig
{
    public static final String ID_REGEXP = "[A-Za-z0-9][_A-Za-z0-9-]*";
    public static final String ENV_REGEXP = "[a-z0-9][_a-z0-9]*";
    public static final String HOSTNAME_REGEXP = "[a-z0-9][-_a-z0-9]*(?:\\.[a-z0-9][-_a-z0-9]*)+";
    public static final String POOL_REGEXP = "[a-zA-Z0-9][-_a-zA-Z0-9]*";

    private String environment;
    private String pool = "general";
    private String nodeId;
    private String location;
    private InetAddress nodeInternalIp;
    private String nodeInternalHostname;
    private String nodeExternalAddress;
    private InetAddress nodeBindIp;

    @NotNull
    @Pattern(regexp = ENV_REGEXP, message = "is malformed")
    public String getEnvironment()
    {
        return environment;
    }

    @Config("node.environment")
    public NodeConfig setEnvironment(String environment)
    {
        this.environment = environment;
        return this;
    }

    @NotNull
    @Pattern(regexp = POOL_REGEXP, message = "is malformed")
    public String getPool()
    {
        return pool;
    }

    @Config("node.pool")
    public NodeConfig setPool(String pool)
    {
        this.pool = pool;
        return this;
    }

    @Pattern(regexp = ID_REGEXP, message = "is malformed")
    public String getNodeId()
    {
        return nodeId;
    }

    @Config("node.id")
    public NodeConfig setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
        return this;
    }

    public String getLocation()
    {
        return location;
    }

    @Config("node.location")
    public NodeConfig setLocation(String location)
    {
        this.location = location;
        return this;
    }

    public InetAddress getNodeInternalIp()
    {
        return nodeInternalIp;
    }

    public NodeConfig setNodeInternalIp(InetAddress nodeInternalIp)
    {
        this.nodeInternalIp = nodeInternalIp;
        return this;
    }

    @Config("node.ip")
    public NodeConfig setNodeInternalIp(String nodeInternalIp)
    {
        if (nodeInternalIp != null) {
            this.nodeInternalIp = InetAddresses.forString(nodeInternalIp);
        }
        return this;
    }

    @Pattern(regexp = HOSTNAME_REGEXP, message = "is malformed")
    public String getNodeInternalHostname()
    {
        return nodeInternalHostname;
    }

    @Config("node.hostname")
    public NodeConfig setNodeInternalHostname(String nodeInternalHostname)
    {
        this.nodeInternalHostname = nodeInternalHostname;
        return this;
    }

    public String getNodeExternalAddress()
    {
        return nodeExternalAddress;
    }

    @Config("node.external-address")
    public NodeConfig setNodeExternalAddress(String nodeExternalAddress)
    {
        this.nodeExternalAddress = nodeExternalAddress;
        return this;
    }

    public InetAddress getNodeBindIp()
    {
        return nodeBindIp;
    }

    public NodeConfig setNodeBindIp(InetAddress nodeBindIp)
    {
        this.nodeBindIp = nodeBindIp;
        return this;
    }

    @Config("node.bind-ip")
    public NodeConfig setNodeBindIp(String nodeBindIp)
    {
        if (nodeBindIp != null) {
            this.nodeBindIp = InetAddresses.forString(nodeBindIp);
        }
        return this;
    }
}

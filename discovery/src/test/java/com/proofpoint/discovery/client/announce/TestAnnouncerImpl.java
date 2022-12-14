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
package com.proofpoint.discovery.client.announce;

import com.google.common.collect.ImmutableMap;
import com.proofpoint.discovery.client.DiscoveryException;
import com.proofpoint.discovery.client.ServiceDescriptor;
import com.proofpoint.discovery.client.ServiceDescriptors;
import com.proofpoint.discovery.client.testing.InMemoryDiscoveryClient;
import com.proofpoint.http.client.ServiceType;
import com.proofpoint.node.NodeConfig;
import com.proofpoint.node.NodeInfo;
import com.proofpoint.units.Duration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.proofpoint.concurrent.MoreFutures.getFutureValue;
import static com.proofpoint.http.client.ServiceTypes.serviceType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class TestAnnouncerImpl
{
    public static final Duration MAX_AGE = new Duration(1, TimeUnit.MILLISECONDS);
    private final ServiceType serviceType = serviceType("foo");
    private Announcer announcer;
    private InMemoryDiscoveryClient discoveryClient;
    private ServiceAnnouncement serviceAnnouncement;
    private NodeInfo nodeInfo;

    @BeforeMethod
    protected void setUp()
    {
        nodeInfo = new NodeInfo("test-application", new NodeConfig().setEnvironment("test").setPool("pool"));
        discoveryClient = new InMemoryDiscoveryClient(nodeInfo, MAX_AGE);
        serviceAnnouncement = ServiceAnnouncement.serviceAnnouncement(serviceType.value()).addProperty("a", "apple").build();
        announcer = new AnnouncerImpl(discoveryClient, Set.of(serviceAnnouncement));
    }

    @AfterMethod
    public void tearDown()
    {
        announcer.destroy();
        assertAnnounced();
    }

    @Test
    public void testBasic()
    {
        assertAnnounced();

        announcer.start();

        assertAnnounced(serviceAnnouncement);
    }

    @Test
    public void startAfterDestroy()
    {
        announcer.start();
        announcer.destroy();

        try {
            announcer.start();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void idempotentStart()
    {
        announcer.start();
        announcer.start();
        announcer.start();
    }


    @Test
    public void idempotentDestroy()
    {
        announcer.start();
        announcer.destroy();
        announcer.destroy();
        announcer.destroy();
    }

    @Test
    public void destroyNoStart()
    {
        discoveryClient = spy(discoveryClient);
        announcer = new AnnouncerImpl(discoveryClient, Set.of(serviceAnnouncement));

        announcer.destroy();

        verifyNoMoreInteractions(discoveryClient);
    }

    @Test
    public void addAnnouncementAfterStart()
            throws Exception
    {
        assertAnnounced();

        announcer.start();

        ServiceAnnouncement newAnnouncement = ServiceAnnouncement.serviceAnnouncement(serviceType.value()).addProperty("a", "apple").build();
        announcer.addServiceAnnouncement(newAnnouncement);

        Thread.sleep(100);
        assertAnnounced(serviceAnnouncement, newAnnouncement);
    }

    @Test
    public void removeAnnouncementAfterStart()
            throws Exception
    {
        assertAnnounced();

        announcer.start();

        announcer.removeServiceAnnouncement(serviceAnnouncement.getId());

        Thread.sleep(100);
        assertAnnounced();
    }

    private void assertAnnounced(ServiceAnnouncement... serviceAnnouncements)
    {
        Future<ServiceDescriptors> future = discoveryClient.getServices(serviceType.value(), "pool");
        ServiceDescriptors serviceDescriptors = getFutureValue(future, DiscoveryException.class);

        assertEquals(serviceDescriptors.getType(), serviceType.value());
        assertEquals(serviceDescriptors.getPool(), "pool");
        assertNotNull(serviceDescriptors.getETag());
        assertEquals(serviceDescriptors.getMaxAge(), MAX_AGE);

        List<ServiceDescriptor> descriptors = serviceDescriptors.getServiceDescriptors();
        assertEquals(descriptors.size(), serviceAnnouncements.length);

        ImmutableMap.Builder<UUID, ServiceDescriptor> builder = ImmutableMap.builder();
        for (ServiceDescriptor descriptor : descriptors) {
            builder.put(descriptor.getId(), descriptor);
        }
        Map<UUID, ServiceDescriptor> descriptorMap = builder.build();

        for (ServiceAnnouncement serviceAnnouncement : serviceAnnouncements) {
            ServiceDescriptor serviceDescriptor = descriptorMap.get(serviceAnnouncement.getId());
            assertNotNull(serviceDescriptor, "No descriptor for announcement " + serviceAnnouncement.getId());
            assertEquals(serviceDescriptor.getType(), serviceType.value());
            assertEquals(serviceDescriptor.getPool(), "pool");
            assertEquals(serviceDescriptor.getId(), serviceAnnouncement.getId());
            assertEquals(serviceDescriptor.getProperties(), serviceAnnouncement.getProperties());
            assertEquals(serviceDescriptor.getNodeId(), nodeInfo.getNodeId());
        }
    }
}

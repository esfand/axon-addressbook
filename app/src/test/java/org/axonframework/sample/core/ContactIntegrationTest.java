/*
 * Copyright (c) 2010. Axon Framework
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

package org.axonframework.sample.core;

import org.axonframework.core.AggregateNotFoundException;
import org.axonframework.core.DomainEvent;
import org.axonframework.core.Event;
import org.axonframework.core.eventhandler.EventBus;
import org.axonframework.core.eventhandler.annotation.EventHandler;
import org.axonframework.core.repository.Repository;
import org.axonframework.core.repository.eventsourcing.XStreamFileSystemEventStore;
import org.axonframework.sample.core.command.ContactCommandHandler;
import org.junit.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author Allard Buijze
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/META-INF/spring/application-context.xml",
        "classpath:/META-INF/spring/database-context.xml"})
@Transactional(readOnly = false)
public class ContactIntegrationTest {

    @Autowired
    private ContactCommandHandler commandHandler;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private XStreamFileSystemEventStore eventStore;

    @Autowired
    private Repository repository;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private List<Event> dispatchedEvents = new ArrayList<Event>();

    @Before
    public void setUp() throws IOException {
        FileSystemResource resource = new FileSystemResource("target/");
        eventStore.setBaseDir(resource);
    }

    @Test
//(timeout = 10000)
public void testApplicationContext() throws InterruptedException {
        assertNotNull(commandHandler);
        UUID contactId = commandHandler.createContact("Allard");

        commandHandler.registerAddress(contactId, AddressType.PRIVATE, address("Street 123", "90210", "City"));
        commandHandler.registerAddress(contactId, AddressType.PRIVATE, address("Street 321", "90210", "City"));
        commandHandler.removeAddress(contactId, AddressType.PRIVATE);
        commandHandler.removeAddress(contactId, AddressType.PRIVATE);
        commandHandler.deleteContact(contactId);

        try {
            commandHandler.registerAddress(contactId, AddressType.PRIVATE, address("Street 321", "90210", "City"));
            fail("Excepted exception");
        } catch (AggregateNotFoundException e) {
            // we got 'm
        }

        // the event bus is asynchronous. Let's wait for the task executor to finish all tasks
        while (taskExecutor.getActiveCount() > 0) {
            Thread.sleep(10);
        }

        assertEquals(5, dispatchedEvents.size());

        assertEquals(ContactCreatedEvent.class, dispatchedEvents.get(0).getClass());
        assertEquals(AddressAddedEvent.class, dispatchedEvents.get(1).getClass());
        assertEquals(AddressChangedEvent.class, dispatchedEvents.get(2).getClass());
        assertEquals(AddressRemovedEvent.class, dispatchedEvents.get(3).getClass());
    }

    private Address address(String streetAndNumber, String zipCode, String city) {
        return new Address(streetAndNumber, zipCode, city);
    }

    @EventHandler
    public void registerEvent(DomainEvent event) {
        dispatchedEvents.add(event);
    }
}

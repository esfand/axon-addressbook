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

package org.axonframework.sample.core.query;

import org.axonframework.core.eventhandler.SequentialPerAggregatePolicy;
import org.axonframework.core.eventhandler.TransactionAware;
import org.axonframework.core.eventhandler.TransactionStatus;
import org.axonframework.core.eventhandler.annotation.ConcurrentEventListener;
import org.axonframework.core.eventhandler.annotation.EventHandler;
import org.axonframework.sample.core.AddressAddedEvent;
import org.axonframework.sample.core.ContactCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Allard Buijze
 */
@ConcurrentEventListener(sequencingPolicyClass = SequentialPerAggregatePolicy.class)
public class AddressTableUpdater extends AbstractTransactionalJpaEventListener implements TransactionAware {

    private static final Logger logger = LoggerFactory.getLogger(AddressTableUpdater.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void beforeTransaction(TransactionStatus transactionStatus) {
        super.beforeTransaction(transactionStatus);
    }

    @Override
    public void afterTransaction(TransactionStatus transactionStatus) {
        super.afterTransaction(transactionStatus);
    }

    @EventHandler
    public void handleContactCreatedEvent(ContactCreatedEvent event) {
        ContactEntry entry = new ContactEntry();
        entry.setIdentifier(event.getAggregateIdentifier());
        entry.setName(event.getName());
        entityManager.persist(entry);
    }

    @EventHandler
    public void handleAddressAddedEvent(AddressAddedEvent event) {
        ContactEntry contact = (ContactEntry)
                entityManager.createQuery("SELECT e from ContactEntry e WHERE e.identifier = :id")
                        .setParameter("id", event.getAggregateIdentifier())
                        .getSingleResult();
        AddressEntry entry = new AddressEntry();
        entry.setIdentifier(event.getAggregateIdentifier());
        entry.setName(contact.getName());
        entry.setAddressType(event.getType());
        entry.setStreetAndNumber(event.getAddress().getStreetAndNumber());
        entry.setCity(event.getAddress().getCity());
        entry.setZipCode(event.getAddress().getZipCode());
        entityManager.persist(entry);
    }

}

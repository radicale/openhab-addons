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
package org.openhab.binding.openmotics.internal;

import static org.openhab.binding.openmotics.internal.OpenMoticsBindingConstants.THING_TYPE_GATEWAY;

import java.util.Collections;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link OpenMoticsBridgeDiscovery} is responsible for discovering OpenMotics gateways
 *
 * @author Alessandro Radicati - Initial contribution
 */
@Component(service = MDNSDiscoveryParticipant.class)
@NonNullByDefault
public class OpenMoticsBridgeDiscovery implements MDNSDiscoveryParticipant {

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(THING_TYPE_GATEWAY);
    }

    @Override
    public String getServiceType() {
        return "_workstation._tcp.local.";
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo service) {
        ThingUID uid = getThingUID(service);

        if (uid != null) {
            String[] hostAddresses = service.getHostAddresses();
            if (hostAddresses != null && hostAddresses.length > 0) {
                return DiscoveryResultBuilder.create(uid).withProperty("ipaddress", hostAddresses[0])
                        // .withRepresentationProperty(uid.getId()()
                        .withLabel("OpenMotics Gateway").build();
            }
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        String name = service.getName();
        if (name.contains("OpenMotics")) {
            String[] splitName = name.split(" ");
            if (splitName.length > 1) {
                return new ThingUID(THING_TYPE_GATEWAY, splitName[1].replaceAll("[^A-Za-z0-9_-]", ""));
            }
        }
        return null;
    }
}

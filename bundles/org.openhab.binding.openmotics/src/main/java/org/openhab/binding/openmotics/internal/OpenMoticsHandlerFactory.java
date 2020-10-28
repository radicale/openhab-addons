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

import static org.openhab.binding.openmotics.internal.OpenMoticsBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openmotics.handler.OpenMoticsBridgeHandler;
import org.openhab.binding.openmotics.handler.OpenMoticsOutputHandler;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link OpenMoticsHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alessandro Radicati - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.openmotics", service = ThingHandlerFactory.class)
public class OpenMoticsHandlerFactory extends BaseThingHandlerFactory {

    Map<ThingUID, ServiceRegistration<?>> servRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        if (!SUPPORTED_THINGS.contains(thingTypeUID)) {
            if (!SUPPORTED_BRIDGES.contains(thingTypeUID)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_BRIDGES.contains(thingTypeUID)) {
            OpenMoticsBridgeHandler bridgeHandler = new OpenMoticsBridgeHandler((Bridge) thing);
            OpenMoticsItemDiscovery itemDiscovery = new OpenMoticsItemDiscovery(bridgeHandler);
            servRegs.put(thing.getUID(),
                    bundleContext.registerService(DiscoveryService.class.getName(), itemDiscovery, null));
            return bridgeHandler;
        } else if (thingTypeUID.equals(THING_TYPE_OUTPUT) || thingTypeUID.equals(THING_TYPE_DIMMER)
                || thingTypeUID.equals(THING_TYPE_GROUPACTION)) {
            OpenMoticsOutputHandler thingHandler = new OpenMoticsOutputHandler(thing);
            return thingHandler;
        }

        return null;
    }
}

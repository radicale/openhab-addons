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
import org.openhab.binding.openmotics.internal.api.ApiException;
import org.openhab.binding.openmotics.internal.api.DefaultApi;
import org.openhab.binding.openmotics.internal.api.model.GetGroupActionConfigsResponse;
import org.openhab.binding.openmotics.internal.api.model.GetInputConfigsResponse;
import org.openhab.binding.openmotics.internal.api.model.GetOutputConfigsResponse;
import org.openhab.binding.openmotics.internal.api.model.GrpActConf;
import org.openhab.binding.openmotics.internal.api.model.InConf;
import org.openhab.binding.openmotics.internal.api.model.OutConf;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link OpenMoticsItemsDiscovery} is responsible for discovering OpenMotics things
 *
 * @author Alessandro Radicati - Initial contribution
 */
@NonNullByDefault
public class OpenMoticsItemDiscovery extends AbstractDiscoveryService {
    private static final int SEARCH_TIME = 30;
    private OpenMoticsBridgeHandler bridgeHandler;

    public OpenMoticsItemDiscovery(OpenMoticsBridgeHandler bridgeHandler) {
        super(SUPPORTED_THINGS, SEARCH_TIME, false);
        this.bridgeHandler = bridgeHandler;
    }

    private @Nullable ThingUID getThingUID(Object device) {
        if (device instanceof OutConf) {
            switch (((OutConf) device).getModuleType().toString()) {
                case "O":
                    return new ThingUID(THING_TYPE_OUTPUT, bridgeHandler.getThing().getUID(),
                            "oid" + String.format("%03d", ((OutConf) device).getId()));
                case "D":
                    return new ThingUID(THING_TYPE_DIMMER, bridgeHandler.getThing().getUID(),
                            "oid" + String.format("%03d", ((OutConf) device).getId()));
            }
        } else if (device instanceof GrpActConf) {
            return new ThingUID(THING_TYPE_GROUPACTION, bridgeHandler.getThing().getUID(),
                    "aid" + String.format("%03d", ((GrpActConf) device).getId()));
        } else if (device instanceof InConf) {
            return new ThingUID(THING_TYPE_INPUT, bridgeHandler.getThing().getUID(),
                    "iid" + String.format("%03d", ((InConf) device).getId()));
        }
        return null;
    }

    @Override
    public void startScan() {
        DefaultApi api = bridgeHandler.getApi();
        Object r = null;

        // Search for outputs
        try {
            r = api.getOutputConfigurations(null);
        } catch (ApiException e) {
        }

        if (r != null) {
            for (OutConf output : ((GetOutputConfigsResponse) r).getConfig()) {
                if (output.getName().contentEquals("NOT_IN_USE")) {
                    continue;
                }

                Map<String, Object> props = new HashMap<>();
                ThingUID uid = getThingUID(output);

                if (uid != null) {
                    props.put("id", output.getId());
                    props.put("name", output.getName());
                    props.put("floor", output.getFloor());
                    props.put("timer", output.getTimer());
                    props.put("type", output.getType());

                    thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeHandler.getThing().getUID())
                            .withProperties(props).withRepresentationProperty(uid.getId()).withLabel(output.getName())
                            .build());
                }
            }
        }

        // Search for group actions
        try {
            r = api.getGroupActionConfigurations(null);
        } catch (ApiException e) {
            // logger.error("API Call failed: {}", e);
        }
        if (r != null) {
            for (GrpActConf output : ((GetGroupActionConfigsResponse) r).getConfig()) {
                if (output.getName().isEmpty()) {
                    continue;
                }

                Map<String, Object> props = new HashMap<>();
                ThingUID uid = getThingUID(output);

                if (uid != null) {
                    props.put("id", output.getId());
                    props.put("name", output.getName());
                    props.put("actions", output.getActions());

                    thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeHandler.getThing().getUID())
                            .withProperties(props).withRepresentationProperty(uid.getId()).withLabel(output.getName())
                            .build());
                }
            }
        }

        // Search for inputs
        try {
            r = api.getInputConfigurations(null);
        } catch (ApiException e) {
        }

        if (r != null) {
            for (InConf input : ((GetInputConfigsResponse) r).getConfig()) {

                Map<String, Object> props = new HashMap<>();
                ThingUID uid = getThingUID(input);

                if (uid != null) {
                    String name = input.getName();

                    if (name.isEmpty()) {
                        name = uid.getId();
                    }

                    props.put("name", name);
                    props.put("id", input.getId());

                    props.put("basic_actions", input.getBasicActions());
                    props.put("action", input.getAction());
                    props.put("invert", input.getInvert());

                    thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeHandler.getThing().getUID())
                            .withProperties(props).withRepresentationProperty(uid.getId()).withLabel(name).build());
                }
            }
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }
}

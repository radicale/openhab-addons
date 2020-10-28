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
package org.openhab.binding.openmotics.handler;

import static org.openhab.binding.openmotics.internal.OpenMoticsBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.openmotics.internal.api.ApiException;
import org.openhab.binding.openmotics.internal.api.DefaultApi;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;

/**
 * The {@link OpenMoticsOutputHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alessandro Radicati - Initial contribution
 */
@NonNullByDefault
public class OpenMoticsOutputHandler extends BaseThingHandler {
    public OpenMoticsOutputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        OpenMoticsBridgeHandler bridgeHandler = null;
        Bridge b = getBridge();
        if (b != null) {
            bridgeHandler = (OpenMoticsBridgeHandler) b.getHandler();
            if (bridgeHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_MISSING_ERROR,
                        "Failed to obtain bridge handler");
            } else if (getThing().getStatus().equals(ThingStatus.OFFLINE) && b.getStatus().equals(ThingStatus.ONLINE)) {
                updateStatus(ThingStatus.ONLINE);
            }
        }

        if (bridgeHandler == null) {
            return;
        }

        DefaultApi api = bridgeHandler.getApi();
        Integer id = Double.valueOf(thing.getProperties().get("id")).intValue();
        boolean retry;

        // Handle Group Actions
        if (thing.getThingTypeUID().equals(THING_TYPE_GROUPACTION)) {
            if (command.equals(ACTION_RUN)) {
                retry = true;
                for (int i = 1; retry && i >= 0; i--) {
                    retry = false;
                    try {
                        api.doGroupAction(id);
                    } catch (ApiException e) {
                        retry = bridgeHandler.apiExceptionCatch(e, i > 0);
                    }
                }
            }

            // Handle Outputs
        } else if (thing.getThingTypeUID().equals(THING_TYPE_OUTPUT)
                || thing.getThingTypeUID().equals(THING_TYPE_DIMMER)) {
            if (command instanceof RefreshType) {
                if (!bridgeHandler.outputStateContainsId(id)) {
                    return;
                }

                switch (channelUID.getId()) {
                    case CHANNEL_RELAY:
                        updateState(channelUID, bridgeHandler.getOutputState(id) == 1 ? OnOffType.ON : OnOffType.OFF);
                        break;
                    case CHANNEL_DIMMER:
                        if (bridgeHandler.getOutputState(id) == 0) {
                            updateState(channelUID, new PercentType(0));
                        } else {
                            updateState(channelUID, new PercentType(bridgeHandler.getOutputDimmer(id)));
                        }
                        break;
                }
            } else if (command instanceof OnOffType) {
                retry = true;
                for (int i = 1; retry && i >= 0; i--) {
                    retry = false;
                    try {
                        api.setOutput(id, command == OnOffType.ON ? "true" : "false", null, null);
                        updateState(channelUID, (OnOffType) command);
                    } catch (ApiException e) {
                        retry = bridgeHandler.apiExceptionCatch(e, i > 0);
                    }
                }
            } else if (command instanceof PercentType) {
                int dimVal = ((PercentType) command).intValue();
                retry = true;
                for (int i = 1; retry && i >= 0; i--) {
                    retry = false;
                    try {
                        if (dimVal == 0) {
                            api.setOutput(id, "false", null, null);
                        } else {
                            api.setOutput(id, "true", null, dimVal);
                        }
                        updateState(channelUID, (PercentType) command);
                    } catch (ApiException e) {
                        retry = bridgeHandler.apiExceptionCatch(e, i > 0);
                    }
                }
            }
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }
}

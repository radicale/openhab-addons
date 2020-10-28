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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openmotics.internal.api.ApiException;
import org.openhab.binding.openmotics.internal.api.DefaultApi;
import org.openhab.binding.openmotics.internal.api.OpenMoticsApi;
import org.openhab.binding.openmotics.internal.api.model.GetOutputStatusModelStatus;
import org.openhab.binding.openmotics.internal.api.model.GetOutputStatusResponse;
import org.openhab.binding.openmotics.internal.api.model.LoginResponse;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenMoticsBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alessandro Radicati - Initial contribution
 */
@NonNullByDefault
public class OpenMoticsBridgeHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(OpenMoticsBridgeHandler.class);
    private @Nullable OpenMoticsApi omApi = null;
    private Map<Integer, OutputStatus> outputStates = new HashMap<>();
    private Map<Integer, OutputStatus> prevOutputStates = new HashMap<>();
    // private Set<Integer> actionState = new HashSet<Integer>();
    private @Nullable ScheduledFuture<?> refreshHandle;

    private class OutputStatus {
        public int status;
        public int dimmer;
        public int ctimer;

        public OutputStatus(int status, int dimmer, int ctimer) {
            this.status = status;
            this.dimmer = dimmer;
            this.ctimer = ctimer;
        }

        @Override
        public boolean equals(@Nullable Object that) {
            if (that instanceof OutputStatus) {
                return this.status == ((OutputStatus) that).status && this.dimmer == ((OutputStatus) that).dimmer
                        && this.ctimer == ((OutputStatus) that).ctimer;
            }
            return false;
        }
    }

    public OpenMoticsBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {
        stopRefreshJob();
        super.dispose();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.core.thing.binding.ThingHandler#handleCommand(org.openhab.core.thing.ChannelUID,
     * org.openhab.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateOutputStatus();
        }
    }

    public @Nullable DefaultApi getApi() {
        return omApi != null ? omApi.getApi() : null;
    }

    public boolean apiExceptionCatch(ApiException e, boolean reAuthenticate) {
        // Authentication failure - try to authenticate again
        if (e.getCode() == 401) {
            logger.debug("Authorization denied");
            if (reAuthenticate) {
                logger.debug("Attempting to acquire new authetincation token.");
                if (authenticate()) {
                    return true;
                } else {
                    logger.error("Login failed");
                }
            } else {
                logger.debug("Giving up.");
            }
        } else {
            logger.error("Gateway API call failed: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void initialize() {
        // Disable host name check
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        omApi = new OpenMoticsApi((String) thing.getConfiguration().get(CONFIG_IPADDRESS));

        if (authenticate()) {
            // updateOutputStatus();
            updateStatus(ThingStatus.ONLINE);
            startRefreshJob();
        }
    }

    private boolean authenticate() {
        // Null apiKey removes the query parameter - otherwise call would fail
        if (omApi == null) {
            return false;
        }

        DefaultApi api = omApi.getApi();
        LoginResponse r = null;

        omApi.setApiKey(null);

        try {
            r = api.login((String) thing.getConfiguration().get(CONFIG_USERNAME),
                    (String) thing.getConfiguration().get(CONFIG_PASSWORD), true, 3600);
        } catch (ApiException e) {
            logger.error("Gateway API call failed: {}", e.getMessage());
        }

        if (r != null && r.getSuccess() == true) {
            omApi.setApiKey(r.getToken());
            return true;
        }

        logger.error("Login failed");
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "Authentication failed");
        stopRefreshJob();

        return false;
    }

    public boolean outputStateContainsId(Integer id) {
        return outputStates.containsKey(id);
    }

    public int getOutputState(Integer id) {
        return outputStates.get(id).status;
    }

    public int getOutputDimmer(Integer id) {
        return outputStates.get(id).dimmer;
    }

    private void updateOutputStatus() {
        if (omApi == null) {
            return;
        }

        DefaultApi api = omApi.getApi();

        prevOutputStates.clear();
        prevOutputStates.putAll(outputStates);
        GetOutputStatusResponse r = null;

        boolean retry = true;
        for (int i = 1; retry && i >= 0; i--) {
            retry = false;
            try {
                r = api.getOutputStatus();
            } catch (ApiException e1) {
                retry = apiExceptionCatch(e1, i > 0);
            }
        }

        if (r != null) {
            outputStates.clear();
            for (GetOutputStatusModelStatus status : r.getStatus()) {
                outputStates.put(status.getId(),
                        new OutputStatus(status.getStatus(), status.getDimmer(), status.getCtimer()));
            }
        }
    }

    private void startRefreshJob() {
        if (refreshHandle != null) {
            refreshHandle.cancel(true);
        }

        final Runnable refresher = new Runnable() {

            @Override
            public void run() {
                updateOutputStatus();

                Set<Integer> outputChanges = new TreeSet<>();

                outputStates.forEach((k, v) -> {
                    if (!prevOutputStates.containsKey(k) || !prevOutputStates.get(k).equals(v)) {
                        outputChanges.add(k);
                    }
                });

                for (Thing t : getThing().getThings()) {
                    if (t.getThingTypeUID().equals(THING_TYPE_OUTPUT)
                            || t.getThingTypeUID().equals(THING_TYPE_DIMMER)) {
                        if (outputChanges.contains(Double.valueOf(t.getProperties().get("id")).intValue())) {
                            for (Channel c : t.getChannels()) {
                                ThingHandler h = t.getHandler();
                                if (h != null) {
                                    h.handleCommand(c.getUID(), RefreshType.REFRESH);
                                }
                            }
                        }
                    }
                }
            }
        };

        refreshHandle = scheduler.scheduleWithFixedDelay(refresher, 5, 5, TimeUnit.SECONDS);
    }

    private void stopRefreshJob() {
        if (refreshHandle != null) {
            refreshHandle.cancel(true);
        }
    }
}

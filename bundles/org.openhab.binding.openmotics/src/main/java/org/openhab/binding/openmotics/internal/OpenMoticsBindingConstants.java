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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link OpenMoticsBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alessandro Radicati - Initial contribution
 */
@NonNullByDefault
public class OpenMoticsBindingConstants {

    private static final String BINDING_ID = "openmotics";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_GATEWAY = new ThingTypeUID(BINDING_ID, "OMGateway");
    public static final ThingTypeUID THING_TYPE_MASTER = new ThingTypeUID(BINDING_ID, "OMMaster");
    public static final ThingTypeUID THING_TYPE_OUTPUT = new ThingTypeUID(BINDING_ID, "OMOutput");
    public static final ThingTypeUID THING_TYPE_DIMMER = new ThingTypeUID(BINDING_ID, "OMDimmer");
    public static final ThingTypeUID THING_TYPE_INPUT = new ThingTypeUID(BINDING_ID, "OMInput");
    public static final ThingTypeUID THING_TYPE_SENSOR = new ThingTypeUID(BINDING_ID, "OMSensor");
    public static final ThingTypeUID THING_TYPE_POWERMETER = new ThingTypeUID(BINDING_ID, "OMPowerMeter");
    public static final ThingTypeUID THING_TYPE_GROUPACTION = new ThingTypeUID(BINDING_ID, "OMGroupAction");

    public static final Set<ThingTypeUID> SUPPORTED_THINGS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(THING_TYPE_OUTPUT, THING_TYPE_DIMMER, THING_TYPE_GROUPACTION,
                    THING_TYPE_MASTER, THING_TYPE_INPUT, THING_TYPE_SENSOR, THING_TYPE_POWERMETER)));

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGES = Collections.singleton(THING_TYPE_GATEWAY);
    // public static final Set<ThingTypeUID> SUPPORTED_ALL_THINGS = Sets.union(SUPPORTED_BRIDGES, SUPPORTED_THINGS);

    // List if Configuration fields
    public static final String CONFIG_USERNAME = "username";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_IPADDRESS = "ipaddress";
    public static final String CONFIG_FLOOR = "floor";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_TIMER = "timer";

    // List of all Channel ids
    public static final String CHANNEL_RELAY = "relay";
    public static final String CHANNEL_ACTION = "action";
    public static final String CHANNEL_DIMMER = "dimmer";
    public static final String CHANNEL_VERSION = "version";

    // List of channel commands
    public static final StringType ACTION_RUN = new StringType("RUN");

    // Authentication constants
    public static final String AUTH_METHOD = "query";
    public static final String AUTH_NAME = "token";
    public static final int AUTH_FAIL_CODE = 401;
}

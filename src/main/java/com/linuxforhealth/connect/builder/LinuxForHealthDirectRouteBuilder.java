/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.linuxforhealth.connect.builder;

import com.linuxforhealth.connect.processor.FormatErrorProcessor;
import com.linuxforhealth.connect.processor.FormatMessageProcessor;
import com.linuxforhealth.connect.processor.FormatNotificationProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the Linux for Health direct routes for data storage, notification, and error handling
 */
public class LinuxForHealthDirectRouteBuilder extends RouteBuilder {

    public final static String STORE_AND_NOTIFY_CONSUMER_URI = "direct:storeAndNotify";
    public final static String STORE_CONSUMER_URI = "direct:store";
    public final static String NOTIFY_CONSUMER_URI = "direct:notify";
    public final static String ERROR_CONSUMER_URI = "direct:error";

    public final static String STORE_AND_NOTIFY_ROUTE_ID = "store-and-notify";
    public final static String STORE_ROUTE_ID = "store";
    public final static String NOTIFY_ROUTE_ID = "notify";
    public final static String ERROR_ROUTE_ID = "error";

    private final Logger logger = LoggerFactory.getLogger(LinuxForHealthDirectRouteBuilder.class);

    @Override
    public void configure() {
        // Store results in the data store and send a notification message
        from(STORE_AND_NOTIFY_CONSUMER_URI)
                .routeId(STORE_AND_NOTIFY_ROUTE_ID)
                .doTry()
                    .setHeader(KafkaConstants.KEY, constant("Camel"))
                    .process(new FormatMessageProcessor())
                    .toD("${exchangeProperty[dataStoreUri]}")
                    .to("direct:notify")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(ERROR_CONSUMER_URI)
                .end();

        // Store results in the data store
        from(STORE_CONSUMER_URI)
                .routeId(STORE_ROUTE_ID)
                .doTry()
                    .process(new FormatMessageProcessor())
                    .toD("${exchangeProperty[dataStoreUri]}")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(ERROR_CONSUMER_URI)
                .end();

        // Send a notification message based on the data storage results
        from(NOTIFY_CONSUMER_URI)
                .routeId(NOTIFY_ROUTE_ID)
                .doTry()
                    .process(new FormatNotificationProcessor())
                    .to("{{lfh.connect.messaging.uri}}")
                .doCatch(Exception.class)
                    .setProperty("errorMessage", simple(exceptionMessage().toString()))
                    .to(ERROR_CONSUMER_URI)
                .end();

        // Send an error notification message
        from(ERROR_CONSUMER_URI)
                .routeId(ERROR_ROUTE_ID)
                .doTry()
                    .process(new FormatErrorProcessor())
                    .log(LoggingLevel.ERROR, logger, "${exchangeProperty[errorMessage]}")
                    .to("{{lfh.connect.messaging.uri}}")
                .doCatch(Exception.class)
                    .log(LoggingLevel.ERROR, logger, exceptionMessage().toString())
                .end();
    }
}

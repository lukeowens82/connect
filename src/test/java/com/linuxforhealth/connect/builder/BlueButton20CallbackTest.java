package com.linuxforhealth.connect.builder;

import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Tests {@link BlueButton20RestRouteBuilder#CALLBACK_ROUTE_ID}
 */
public class BlueButton20CallbackTest extends RouteTestSupport {

    private MockEndpoint mockResult;

    @Override
    protected RoutesBuilder createRouteBuilder()  {
        return new BlueButton20RestRouteBuilder();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = super.useOverridePropertiesWithPropertiesComponent();
        props.setProperty("lfh.connect.bluebutton_20.cms.tokenUri", "https://sandbox.bluebutton.cms.gov/v1/o/token/");
        props.setProperty("lfh.connect.bluebutton_20.cms.clientId", "client-id");
        props.setProperty("lfh.connect.bluebutton_20.cms.clientSecret", "client-secret");
        return props;
    }

    @BeforeEach
    @Override
    protected void configureContext() throws Exception {
        mockProducerEndpoint(BlueButton20RestRouteBuilder.CALLBACK_ROUTE_ID,
                "https://sandbox.bluebutton.cms.gov/v1/o/token/",
                "mock:result");
        super.configureContext();
        mockResult = MockEndpoint.resolve(context, "mock:result");
    }

    /**
     * Validates that Blue Button 20 Callback exchange headers and body are properly formed.
      * @throws InterruptedException
     */
    @Test
    void testRoute() throws InterruptedException {

        mockResult.expectedMessageCount(1);

        String expectedBody = "code=auth-code&grant_type=authorization_code";
        mockResult.expectedBodiesReceived(expectedBody);

        mockResult.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        String expectedAuthHeader = Base64.getEncoder().encodeToString("client-id:client-secret".getBytes(StandardCharsets.UTF_8));
        mockResult.expectedHeaderReceived("Authorization", expectedAuthHeader);

        mockResult.expectedHeaderReceived("Content-Type", "application/x-www-form-urlencoded");
        mockResult.expectedHeaderReceived("Content-Length", expectedBody.length());

        fluentTemplate.to("{{lfh.connect.bluebutton_20.handlerUri}}")
                .withHeader("code", "auth-code")
                .send();

        mockResult.assertIsSatisfied();
    }
}

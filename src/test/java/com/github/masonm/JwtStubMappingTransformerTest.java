package com.github.masonm;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.matching.CustomMatcherDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class JwtStubMappingTransformerTest {
    private static final JwtStubMappingTransformer TRANSFORMER = new JwtStubMappingTransformer();

    @Test
    public void returnsMappingUnmodifiedWithMissingParameters() {
        StubMapping testMapping = WireMock.get("/").build();
        assertUnmodifiedOnTransform(testMapping, Parameters.empty());
    }

    @Test
    public void returnsMappingUnmodifiedWhenRequestHasCustomMatcher() {
        StubMapping testMapping = WireMock.get("/").build();
        testMapping.setRequest(WireMock.requestMadeFor(RequestPattern.ANYTHING).build());
        assertUnmodifiedOnTransform(testMapping, getParams("foo"));
    }

    @Test
    public void returnsMappingUnmodifiedWhenNoAuthorizationHeader() {
        StubMapping testMapping = WireMock.get("/").build();
        assertUnmodifiedOnTransform(testMapping, getParams("foo"));
    }

    @Test
    public void returnsMappingUnmodifiedWhenAuthorizationHeaderTypeIsNotBearer() {
        StubMapping testMapping = WireMock.get("/").withBasicAuth("foo", "bar").build();
        assertUnmodifiedOnTransform(testMapping, getParams("foo"));
    }

    @Test
    public void returnsMappingUnmodifiedWhenAuthorizationHeaderIsInvalid() {
        StubMapping testMapping = WireMock
            .get("/")
            .withHeader("Authorization", WireMock.equalTo("Bearer f00"))
            .build();
        assertUnmodifiedOnTransform(testMapping, getParams("foo"));
    }

    @Test
    public void returnsModifiedMappingWhenMatchingValidPayloadField() {
        final TestAuthHeader testAuthHeader = new TestAuthHeader("doesnt_matter", "matched");
        StubMapping testMapping = WireMock
            .get("/")
            .withHeader("Authorization", WireMock.equalTo(testAuthHeader.toString()))
            .build();
        final Parameters payloadMatchParams = Parameters.one(
            JwtStubMappingTransformer.PAYLOAD_FIELDS,
            Arrays.asList("matched_key")
        );

        StubMapping transformedMapping = TRANSFORMER.transform(testMapping, null, payloadMatchParams);

        final CustomMatcherDefinition jwtMatcher  = transformedMapping.getRequest().getCustomMatcher();
        assertThat(jwtMatcher, is(notNullValue()));
        assertThat(jwtMatcher.getName(), is(JwtMatcherExtension.NAME));

        final Map<String, Object> expectedParameters = new HashMap<String, Object>() {{
            put(JwtMatcherExtension.PARAM_NAME_PAYLOAD, new HashMap<String, String>() {{
                put("matched_key", "matched_value");
            }});
            put(JwtMatcherExtension.PARAM_NAME_REQUEST, new HashMap<String, String>() {{
                put("url", "/");
                put("method", "GET");
            }});
        }};
        assertThat(jwtMatcher.getParameters(), is(expectedParameters));
    }

    private Parameters getParams(String ...payloadFields) {
       return Parameters.one(JwtStubMappingTransformer.PAYLOAD_FIELDS, Arrays.asList(payloadFields));
    }

    private void assertUnmodifiedOnTransform(StubMapping testMapping, Parameters parameters) {
        String json = testMapping.toString();
        TRANSFORMER.transform(testMapping, null, parameters);
        assertThat(testMapping.toString(), is(json));
    }
}

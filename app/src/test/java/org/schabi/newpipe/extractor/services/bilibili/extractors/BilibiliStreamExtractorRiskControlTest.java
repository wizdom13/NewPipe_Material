package org.schabi.newpipe.extractor.services.bilibili.extractors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.grack.nanojson.JsonObject;

import org.junit.Test;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ServiceTemporaryBlockedException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class BilibiliStreamExtractorRiskControlTest {

    private static final String URL = "https://api.bilibili.com/test";

    @Test
    public void recognizesHttp412WithoutTryingToReadVideoData() {
        final String body = "<!DOCTYPE html><html>ordinary response</html>";

        final ServiceTemporaryBlockedException exception = assertThrows(
                ServiceTemporaryBlockedException.class,
                () -> BillibiliStreamExtractor.parseBilibiliResponse(response(412, body))
        );

        assertEquals("BiliBili temporarily blocked requests from this network",
                exception.getMessage());
        assertFalse(exception.getMessage().contains(body));
    }

    @Test
    public void recognizesJsonRiskControlCodeWithoutLeakingResponseData() {
        final String body = "{\"code\":-352,\"message\":\"risk control\","
                + "\"data\":{\"v_voucher\":\"private-value\"}}";

        final ServiceTemporaryBlockedException exception = assertThrows(
                ServiceTemporaryBlockedException.class,
                () -> BillibiliStreamExtractor.parseBilibiliResponse(response(200, body))
        );

        assertFalse(exception.getMessage().contains("private-value"));
    }

    @Test
    public void returnsSuccessfulJsonResponse() throws Exception {
        final JsonObject response = BillibiliStreamExtractor.parseBilibiliResponse(
                response(200, "{\"code\":0,\"data\":{\"title\":\"ok\"}}")
        );

        assertEquals("ok", response.getObject("data").getString("title"));
    }

    @Test
    public void sanitizesUnexpectedNonJsonResponse() {
        final String body = "<html>not a BiliBili security response</html>";

        final ParsingException exception = assertThrows(
                ParsingException.class,
                () -> BillibiliStreamExtractor.parseBilibiliResponse(response(500, body))
        );

        assertEquals("BiliBili returned an invalid response (HTTP 500)",
                exception.getMessage());
        assertFalse(exception.getMessage().contains(body));
    }

    private static Response response(final int responseCode, final String body) {
        return new Response(
                responseCode,
                "response",
                Collections.emptyMap(),
                body,
                body.getBytes(StandardCharsets.UTF_8),
                URL
        );
    }
}

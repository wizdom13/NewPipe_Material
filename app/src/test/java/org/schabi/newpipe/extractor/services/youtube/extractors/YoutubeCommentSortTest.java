package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.comments.CommentSortOrder;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeCommentSortTest {
    @Test
    void choosesTheRequestedServerContinuationRegardlessOfTranslatedTitle() throws Exception {
        final JsonObject response = JsonParser.object().from("""
                {"onResponseReceivedEndpoints":[{}, {"reloadContinuationItemsCommand":{
                  "continuationItems":[{}, {"commentsHeaderRenderer":{"sortMenu":{
                    "sortFilterSubMenuRenderer":{"subMenuItems":[
                      {"title":"Principales","serviceEndpoint":{
                        "continuationCommand":{"token":"top-token"}}},
                      {"title":"Recientes","serviceEndpoint":{
                        "continuationCommand":{"token":"newest-token"}}}
                    ]}
                  }}}]
                }}]}
                """);
        assertEquals("top-token", YoutubeCommentSort.continuation(response, CommentSortOrder.TOP));
        assertEquals("newest-token",
                YoutubeCommentSort.continuation(response, CommentSortOrder.NEWEST));
    }

    @Test
    void unavailableSortDoesNotSilentlyReturnTheDefaultOrder() {
        assertThrows(ParsingException.class,
                () -> YoutubeCommentSort.continuation(new JsonObject(), CommentSortOrder.NEWEST));
    }
}

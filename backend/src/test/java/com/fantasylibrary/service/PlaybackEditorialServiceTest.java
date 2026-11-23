package com.fantasylibrary.service;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.playback.PlaybackEditorialModels.SourceDraft;
import com.fantasylibrary.playback.PlaybackModels.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaybackEditorialServiceTest {
    @Test
    void keepsExternalHttpsUrlAsGiven() {
        String external = "https://cdn.example/video.mp4?expires=123&token=abc#t=30";
        var saved = PlaybackEditorialService.normalize(new SourceDraft(" 外部源 ", " HD ", external, Format.MP4));

        assertEquals(external, saved.url());
        assertEquals("外部源", saved.label());
        assertEquals("HD", saved.quality());
    }

    @Test
    void matchesInternalPathAndVideoFormat() {
        String internal = "/api/media/playback/836acf99.mp4";
        assertEquals(internal, PlaybackEditorialService.normalize(new SourceDraft("主源", "1080p", internal, Format.MP4)).url());
        assertThrows(AccountProblem.class,
                () -> PlaybackEditorialService.normalize(new SourceDraft("主源", "1080p", internal, Format.WEBM)));
    }

    @Test
    void rejectsUnsafeSourceAddresses() {
        for (String url : new String[]{"//example.com/a.mp4", "https://user@example.com/a.mp4", "file:///D:/video.mp4",
                "/api/media/playback/../private.mp4", "/api/media/playback/a.mp4?changed=true"}) {
            assertThrows(AccountProblem.class,
                    () -> PlaybackEditorialService.normalize(new SourceDraft("主源", "1080p", url, Format.MP4)), url);
        }
    }
}

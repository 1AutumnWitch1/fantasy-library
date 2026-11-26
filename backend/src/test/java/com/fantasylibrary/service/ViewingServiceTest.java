package com.fantasylibrary.service;

import com.fantasylibrary.playback.ViewingModels.Sample;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViewingServiceTest {
    @Test
    void acceptsTimedPlaybackAndRejectsSeeksAndImmediateUpdates() {
        var playing = new Sample(8_000, 20_000, true, false);
        assertThat(ViewingService.validAdvance(0, 8_000, 4_000, playing)).isEqualTo(8_000);
        assertThat(ViewingService.validAdvance(0, 8_000, 500, playing)).isZero();
        assertThat(ViewingService.validAdvance(7_900, 8_000, 0, playing)).isZero();
        assertThat(ViewingService.validAdvance(0, 8_000, 60_000, playing)).isZero();
        assertThat(ViewingService.validAdvance(0, 8_000, 4_000,
                new Sample(8_000, 20_000, true, true))).isZero();
        assertThat(ViewingService.validAdvance(0, 8_000, 4_000,
                new Sample(8_000, 20_000, false, false))).isZero();
    }

    @Test
    void uniqueCoverageAcrossSessionsCompletesButAnEndSeekOrReplayOfOnePartDoesNot() {
        var tail = ViewingService.combine("", 17_000, 20_000, 20_000);
        assertThat(ViewingService.completed(tail, 20_000, 20_000)).isFalse();
        var repeated = ViewingService.combine("0:5000,17000:20000", 1_000, 4_000, 20_000);
        assertThat(ViewingService.completed(repeated, 20_000, 20_000)).isFalse();

        var full = ViewingService.combine("0:10000,18000:20000", 9_950, 18_050, 20_000);
        assertThat(full).containsExactly(new ViewingService.Span(0, 20_000));
        assertThat(ViewingService.completed(full, 20_000, 20_000)).isTrue();
    }
}

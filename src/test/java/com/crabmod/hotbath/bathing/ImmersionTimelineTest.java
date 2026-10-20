package com.crabmod.hotbath.bathing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImmersionTimelineTest {
    @Test void repeated_callbacks_do_not_advance_time_or_entries() {
        var timeline=new ImmersionTimeline();var first=timeline.contact("milk",100,10);
        assertTrue(first.isFirstEnter());assertEquals(0,first.stayedTicks());
        for(int i=0;i<50;i++)assertFalse(timeline.contact("milk",100,10).shouldProcess());
        var next=timeline.contact("milk",101,10);assertEquals(1,next.stayedTicks());assertEquals(1,next.totalEnterCount());
    }
    @Test void switching_exact_liquid_identity_restarts_delay() {
        var timeline=new ImmersionTimeline();
        for(int tick=100;tick<160;tick++)timeline.contact("pack:first",tick,10);
        var changed=timeline.contact("pack:second",160,10);
        assertTrue(changed.isFirstEnter());assertEquals(0,changed.stayedTicks());
        assertEquals(1,timeline.enterCount("pack:first"));assertEquals(1,timeline.enterCount("pack:second"));
        var returned=timeline.contact("pack:first",161,10);assertEquals(0,returned.stayedTicks());assertEquals(2,returned.totalEnterCount());
    }
    @Test void pose_and_source_changes_do_not_restart_the_same_liquid() {
        var timeline=new ImmersionTimeline();timeline.contact("hotbath:milk",100,10);
        assertFalse(timeline.contact("hotbath:milk",101,10).isFirstEnter());
        assertFalse(timeline.contact("hotbath:milk",105,10).isFirstEnter());
        assertEquals(1,timeline.enterCount("hotbath:milk"));
    }
    @Test void real_exit_and_clock_reset_start_a_new_visit() {
        var timeline=new ImmersionTimeline();timeline.contact("hotbath:milk",100,10);
        assertTrue(timeline.contact("hotbath:milk",111,10).isFirstEnter());
        assertTrue(timeline.contact("hotbath:milk",1,10).isFirstEnter());assertEquals(3,timeline.enterCount("hotbath:milk"));
    }
    @Test void first_tick_zero_is_not_mistaken_for_a_duplicate() {
        assertTrue(new ImmersionTimeline().contact("water",0,10).shouldProcess());
    }
    @Test void adjacent_liquids_cannot_apply_two_effect_passes_in_one_tick() {
        var timeline=new ImmersionTimeline();timeline.contact("first",100,10);
        assertFalse(timeline.contact("second",100,10).shouldProcess());
        assertEquals(0,timeline.enterCount("second"));assertTrue(timeline.contact("second",101,10).isFirstEnter());
    }

    @Test void long_running_world_clocks_do_not_overflow_into_another_visit() {
        var timeline=new ImmersionTimeline();long time=1L<<40;
        timeline.contact("milk",time,10);assertEquals(1,timeline.contact("milk",time+1,10).stayedTicks());
    }
    @Test void entry_rewards_count_real_visits_per_liquid() {
        var timeline=new ImmersionTimeline();
        for(int visit=1;visit<=100;visit++) {
            var contact=timeline.contact("hotbath:hot_water_fluid",visit*11L,10);
            assertTrue(contact.isFirstEnter());assertEquals(visit,contact.totalEnterCount());
        }
        assertEquals(0,timeline.enterCount("hotbath:peony_bath_fluid"));
    }
}

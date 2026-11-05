package com.crosscheck.infrastructure.openai;

import com.crosscheck.domain.analysis.AnalysisCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SportsAgentConfigurationTest {
    @Test
    void categorySelectsIndependentInstructionsWithSameStructuredOutputContract() {
        var political = UnifiedAgentConfiguration.value(AnalysisCategory.POLITICAL_ANALYSIS);
        var sports = UnifiedAgentConfiguration.value(AnalysisCategory.SPORTS_ANALYSIS);
        assertTrue(((String) sports.get("instructions")).startsWith("You are CrossCheck's sports analysis agent"));
        assertTrue(((String) political.get("instructions")).contains("CrossCheck's political analysis agent"));
        assertNotEquals(political.get("instructions"), sports.get("instructions"));
        assertEquals(political.get("text"), sports.get("text"));
        assertEquals(political.get("tools"), sports.get("tools"));
        assertEquals(political.get("model"), sports.get("model"));
    }
    @Test
    void consumptionUsesIndependentInstructionsAndSharedResponseContract() {
        var consumption = UnifiedAgentConfiguration.value(AnalysisCategory.CONSUMPTION_ANALYSIS);
        var political = UnifiedAgentConfiguration.value(AnalysisCategory.POLITICAL_ANALYSIS);
        assertTrue(((String) consumption.get("instructions")).startsWith("You are CrossCheck's consumption"));
        assertNotEquals(political.get("instructions"), consumption.get("instructions"));
        assertEquals(political.get("text"), consumption.get("text"));
        assertEquals(political.get("tools"), consumption.get("tools"));
    }
}

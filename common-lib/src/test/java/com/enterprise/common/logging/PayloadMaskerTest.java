package com.enterprise.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PayloadMaskerTest {

    private static PayloadMasker enabled() {
        var cfg = new LoggingProperties.Masking();
        return new PayloadMasker(cfg);
    }

    @Test
    void masks_json_string_value() {
        String out = enabled().mask("{\"username\":\"a\",\"password\":\"super\"}");
        assertThat(out).contains("\"password\":\"***\"")
                .contains("\"username\":\"a\"");
    }

    @Test
    void masks_form_value() {
        String out = enabled().mask("username=a&password=super&x=1");
        assertThat(out).contains("password=***").contains("username=a");
    }

    @Test
    void masks_bearer_token_in_header_string() {
        String out = enabled().mask("Authorization: Bearer eyJabc.def_ghi-123");
        assertThat(out).isEqualTo("Authorization: Bearer ***");
    }

    @Test
    void masking_disabled_returns_original() {
        var cfg = new LoggingProperties.Masking();
        cfg.setEnabled(false);
        var m = new PayloadMasker(cfg);
        String s = "{\"password\":\"x\"}";
        assertThat(m.mask(s)).isEqualTo(s);
    }

    @Test
    void null_and_empty_payload_are_safe() {
        assertThat(enabled().mask(null)).isNull();
        assertThat(enabled().mask("")).isEmpty();
    }

    @Test
    void masks_numeric_and_boolean_json_values() {
        String out = enabled().mask("{\"pin\":1234,\"enabled\":true}");
        assertThat(out).contains("\"pin\":\"***\"");
    }
}

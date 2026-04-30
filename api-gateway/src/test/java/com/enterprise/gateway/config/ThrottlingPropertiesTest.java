package com.enterprise.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ThrottlingProperties Tests")
class ThrottlingPropertiesTest {

  @Nested
  @DisplayName("Tier Validation")
  class TierValidation {

    @Test
    @DisplayName("Should create valid tier with positive values")
    void shouldCreateValidTier() {
      var tier = new ThrottlingProperties.Tier(50, 100);

      assertThat(tier.replenishRate()).isEqualTo(50);
      assertThat(tier.burstCapacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should reject negative replenish rate")
    void shouldRejectNegativeReplenishRate() {
      assertThatThrownBy(() -> new ThrottlingProperties.Tier(-1, 100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("replenishRate must be positive");
    }

    @Test
    @DisplayName("Should reject zero replenish rate")
    void shouldRejectZeroReplenishRate() {
      assertThatThrownBy(() -> new ThrottlingProperties.Tier(0, 100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("replenishRate must be positive");
    }

    @Test
    @DisplayName("Should reject negative burst capacity")
    void shouldRejectNegativeBurstCapacity() {
      assertThatThrownBy(() -> new ThrottlingProperties.Tier(50, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }

    @Test
    @DisplayName("Should reject zero burst capacity")
    void shouldRejectZeroBurstCapacity() {
      assertThatThrownBy(() -> new ThrottlingProperties.Tier(50, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }

    @Test
    @DisplayName("Should reject burst capacity less than replenish rate")
    void shouldRejectBurstLessThanReplenish() {
      assertThatThrownBy(() -> new ThrottlingProperties.Tier(100, 50))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity should be >= replenishRate");
    }

    @Test
    @DisplayName("Should accept burst capacity equal to replenish rate")
    void shouldAcceptEqualBurstAndReplenish() {
      var tier = new ThrottlingProperties.Tier(50, 50);

      assertThat(tier.replenishRate()).isEqualTo(50);
      assertThat(tier.burstCapacity()).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("Default Values")
  class DefaultValues {

    @Test
    @DisplayName("Should provide default critical tier when null")
    void shouldProvideDefaultCritical() {
      var props = new ThrottlingProperties(null, null, null, null, null);

      assertThat(props.critical()).isNotNull();
      assertThat(props.critical().replenishRate()).isEqualTo(5);
      assertThat(props.critical().burstCapacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should provide default sensitive tier when null")
    void shouldProvideDefaultSensitive() {
      var props = new ThrottlingProperties(null, null, null, null, null);

      assertThat(props.sensitive()).isNotNull();
      assertThat(props.sensitive().replenishRate()).isEqualTo(30);
      assertThat(props.sensitive().burstCapacity()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should provide default standard tier when null")
    void shouldProvideDefaultStandard() {
      var props = new ThrottlingProperties(null, null, null, null, null);

      assertThat(props.standard()).isNotNull();
      assertThat(props.standard().replenishRate()).isEqualTo(100);
      assertThat(props.standard().burstCapacity()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should provide default graphql tier when null")
    void shouldProvideDefaultGraphql() {
      var props = new ThrottlingProperties(null, null, null, null, null);

      assertThat(props.graphql()).isNotNull();
      assertThat(props.graphql().replenishRate()).isEqualTo(20);
      assertThat(props.graphql().burstCapacity()).isEqualTo(40);
    }

    @Test
    @DisplayName("Should provide default publicApi tier when null")
    void shouldProvideDefaultPublicApi() {
      var props = new ThrottlingProperties(null, null, null, null, null);

      assertThat(props.publicApi()).isNotNull();
      assertThat(props.publicApi().replenishRate()).isEqualTo(50);
      assertThat(props.publicApi().burstCapacity()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Custom Tiers")
  class CustomTiers {

    @Test
    @DisplayName("Should use custom tier values when provided")
    void shouldUseCustomValues() {
      var critical = new ThrottlingProperties.Tier(10, 20);
      var sensitive = new ThrottlingProperties.Tier(50, 100);
      var standard = new ThrottlingProperties.Tier(200, 400);
      var graphql = new ThrottlingProperties.Tier(30, 60);
      var publicApi = new ThrottlingProperties.Tier(75, 150);

      var props = new ThrottlingProperties(critical, sensitive, standard, graphql, publicApi);

      assertThat(props.critical().replenishRate()).isEqualTo(10);
      assertThat(props.critical().burstCapacity()).isEqualTo(20);

      assertThat(props.sensitive().replenishRate()).isEqualTo(50);
      assertThat(props.sensitive().burstCapacity()).isEqualTo(100);

      assertThat(props.standard().replenishRate()).isEqualTo(200);
      assertThat(props.standard().burstCapacity()).isEqualTo(400);

      assertThat(props.graphql().replenishRate()).isEqualTo(30);
      assertThat(props.graphql().burstCapacity()).isEqualTo(60);

      assertThat(props.publicApi().replenishRate()).isEqualTo(75);
      assertThat(props.publicApi().burstCapacity()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should mix custom and default tiers")
    void shouldMixCustomAndDefault() {
      var customCritical = new ThrottlingProperties.Tier(15, 30);

      var props = new ThrottlingProperties(customCritical, null, null, null, null);

      // Custom tier
      assertThat(props.critical().replenishRate()).isEqualTo(15);
      assertThat(props.critical().burstCapacity()).isEqualTo(30);

      // Default tiers
      assertThat(props.sensitive().replenishRate()).isEqualTo(30);
      assertThat(props.standard().replenishRate()).isEqualTo(100);
      assertThat(props.graphql().replenishRate()).isEqualTo(20);
      assertThat(props.publicApi().replenishRate()).isEqualTo(50);
    }
  }
}

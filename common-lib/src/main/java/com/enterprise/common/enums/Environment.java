package com.enterprise.common.enums;

/** Deployment environment identifier. Used by {@code /actuator/info} and metrics tags. */
public enum Environment {
  LOCAL,
  DEV,
  QA,
  STAGING,
  PROD
}

package com.enterprise.common.validation;

import com.enterprise.common.util.ValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Implementation of {@link PhoneNumber}. Empty/null values are considered valid. */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || value.isBlank() || ValidationUtils.isPhoneE164(value);
  }
}

package com.enterprise.common.validation;

import com.enterprise.common.util.ValidationUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates an IBAN (country code + check digits + BBAN). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Iban.Validator.class)
public @interface Iban {
  String message() default "must be a valid IBAN";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** Implementation kept nested for easy discovery. */
  class Validator implements ConstraintValidator<Iban, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      return value == null || value.isBlank() || ValidationUtils.isIban(value);
    }
  }
}

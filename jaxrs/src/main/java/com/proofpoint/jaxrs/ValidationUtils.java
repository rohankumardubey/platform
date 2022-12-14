/*
 * Copyright 2014 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.jaxrs;

import com.google.common.reflect.TypeToken;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidationUtils
{
    private static final Validator VALIDATOR = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory().getValidator();

    private ValidationUtils() {}

    public static void validateObject(Type genericType, Object object)
            throws BeanValidationException
    {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(object);

        if (violations.isEmpty() && List.class.isAssignableFrom(TypeToken.of(genericType).getRawType())) {
            violations = VALIDATOR.validate(new ValidatableList((List<?>) object));
        }
        else if (violations.isEmpty() && Collection.class.isAssignableFrom(TypeToken.of(genericType).getRawType())) {
            violations = VALIDATOR.validate(new ValidatableCollection((Collection<?>) object));
        }

        if (violations.isEmpty() && Map.class.isAssignableFrom(TypeToken.of(genericType).getRawType())) {
            violations = VALIDATOR.validate(new ValidatableMap((Map<?, ?>) object));
        }

        if (!violations.isEmpty()) {
            throw new BeanValidationException(violations);
        }
    }

    private static class ValidatableList
    {
        @Valid
        final private List<?> list;

        @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        ValidatableList(List<?> list)
        {
            this.list = list;
        }
    }

    private static class ValidatableCollection
    {
        @Valid
        final private Collection<?> collection;

        @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        ValidatableCollection(Collection<?> collection)
        {
            this.collection = collection;
        }
    }

    private static class ValidatableMap
    {
        @Valid
        final private Map<?, ?> map;

        @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        ValidatableMap(Map<?, ?> map)
        {
            this.map = map;
        }
    }
}

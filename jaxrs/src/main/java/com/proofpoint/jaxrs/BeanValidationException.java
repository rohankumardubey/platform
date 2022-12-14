/*
 * Copyright 2012 Proofpoint, Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.validation.ConstraintViolation;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;

import java.util.List;
import java.util.Set;

/**
 * Thrown when bean validation has errors.
 */
@SuppressModernizer
public class BeanValidationException extends ParsingException
{
    private final ImmutableSet<ConstraintViolation<Object>> violations;

    public BeanValidationException(Set<ConstraintViolation<Object>> violations)
    {
        super(messagesFor(violations).toString());
        this.violations = ImmutableSet.copyOf(violations);
    }

    /**
     * Returns the bean validation error messages.
     *
     * @return validation error messages
     */
    public List<String> getErrorMessages()
    {
        return messagesFor(violations);
    }

    /**
     * Returns the set of bean validation violations.
     *
     * @return set of bean validation violations
     */
    public Set<ConstraintViolation<Object>> getViolations()
    {
        return violations;
    }

    private static List<String> messagesFor(Set<ConstraintViolation<Object>> violations)
    {
        ImmutableList.Builder<String> messages = new ImmutableList.Builder<>();
        for (ConstraintViolation<?> violation : violations) {
            messages.add(violation.getPropertyPath().toString() + " " + violation.getMessage());
        }

        return messages.build();
    }
}
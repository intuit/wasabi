/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.experimentobjects;

import com.google.common.base.Preconditions;
import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The context of an experiment. Usually differentiates between an experiment in PROD(uction) and QA to identify
 * 'real' experiments from testings of configuration.
 */
public class Context {

    @ApiModelProperty(value = "the context of the experiment, eg \"PROD\", \"QA\"", dataType = "String", required = true)
    private String ctx = "PROD"; //default value for context

    private Context(String ctx) {
        this.ctx = Preconditions.checkNotNull(ctx);

        if (ctx.trim().isEmpty()) {
            throw new IllegalArgumentException("Context cannot be " +
                    "an empty string");
        }

        if (!ctx.matches("^[_\\-$A-Za-z][_\\-$A-Za-z0-9]*")) {
            throw new InvalidIdentifierException("Context \"" +
                    ctx + "\" must begin with a letter, -, dollar sign, or " +
                    "underscore, and must not contain any spaces");
        }
    }

    public static Context valueOf(String value) {
        return new Context(value);
    }

    protected Context() {
        super();
    }


    public static Builder newInstance(String ctx) {
        return new Builder(ctx);
    }

    public static Builder from(Context ctx) {
        return new Builder(ctx);
    }

    public static class Builder {

        private Builder(String ctx) {
            instance = new Context(ctx);
        }

        private Builder(Context other) {
            this(other.ctx);
        }

        public Context build() {
            Context result = instance;
            instance = null;
            return result;
        }

        private Context instance;
    }


    public String getContext() {
        return ctx;
    }

    public void setContext(String value) {
        this.ctx = value;
    }


    @Override
    public String toString() {
        return ctx;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}

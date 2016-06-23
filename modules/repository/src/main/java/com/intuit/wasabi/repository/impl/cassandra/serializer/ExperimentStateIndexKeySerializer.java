/*******************************************************************************
 * Copyright 2016 Intuit
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
 *******************************************************************************/
package com.intuit.wasabi.repository.impl.cassandra.serializer;

import com.intuit.wasabi.repository.impl.cassandra.ExperimentsKeyspace;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;

/**
 * Astyanax serializer for
 * @see ExperimentsKeyspace.ExperimentStateIndexKey
 */
public class ExperimentStateIndexKeySerializer
        extends AbstractSerializer<ExperimentsKeyspace.ExperimentStateIndexKey> {

	/**
	 * Singleton instance
	 */
    private static final ExperimentStateIndexKeySerializer INSTANCE =
            new ExperimentStateIndexKeySerializer();

    /**
     * Constructor
     */
    public ExperimentStateIndexKeySerializer() {
        super();
    }

    /**
     * Get instance of serializer
     * @return instance
     */
    public static ExperimentStateIndexKeySerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(
            ExperimentsKeyspace.ExperimentStateIndexKey value) {
        if (value == null) {
            return null;
        }

        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public ExperimentsKeyspace.ExperimentStateIndexKey fromByteBuffer(
            ByteBuffer byteBuffer) {

        if (byteBuffer == null) {
            return null;
        }

        String value = StringSerializer.get().fromByteBuffer(byteBuffer);
        return ExperimentsKeyspace.ExperimentStateIndexKey.valueOf(value);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }
}

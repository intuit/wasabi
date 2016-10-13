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

import com.intuit.wasabi.experimentobjects.Experiment;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Astyanax serializer for
 * @see Experiment.State
 */
public class ExperimentStateSerializer extends AbstractSerializer<Experiment.State> {

	/**
	 * Singleton instance
	 */
    private static final ExperimentStateSerializer INSTANCE =
            new ExperimentStateSerializer();

    /**
     * Constructor
     */
    public ExperimentStateSerializer() {
        super();
    }

    /**
     * Get the instance
     * @return instance
     */
    public static ExperimentStateSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(Experiment.State value) {
        if (Objects.isNull(value)) {
            return null;
        }

        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public Experiment.State fromByteBuffer(ByteBuffer byteBuffer) {
        if (Objects.isNull(byteBuffer)) {
            return null;
        }

        String value = StringSerializer.get().fromByteBuffer(byteBuffer);
        return Experiment.State.valueOf(value);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }
}

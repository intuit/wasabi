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
 * @see Experiment.Label
 */
public class ExperimentLabelSerializer
        extends AbstractSerializer<Experiment.Label> {

	/**
	 * Singleton instance
	 */
    private static final ExperimentLabelSerializer INSTANCE =
            new ExperimentLabelSerializer();

    /**
     * Constructor
     */
    public ExperimentLabelSerializer() {
        super();
    }

    /**
     * Get instance of serializer
     * @return instance
     */
    public static ExperimentLabelSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(Experiment.Label value) {
        if (Objects.isNull(value)) {
            return null;
        }

        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public Experiment.Label fromByteBuffer(ByteBuffer byteBuffer) {
        if (Objects.isNull(byteBuffer)) {
            return null;
        }

        String result = StringSerializer.get().fromByteBuffer(byteBuffer);
        return Experiment.Label.valueOf(result);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }
}

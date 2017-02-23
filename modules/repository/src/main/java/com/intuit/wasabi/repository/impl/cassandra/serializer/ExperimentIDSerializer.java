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
package com.intuit.wasabi.repository.impl.cassandra.serializer;

import com.intuit.wasabi.experimentobjects.Experiment;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.UUIDSerializer;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Astyanax serializer for {@link Experiment.ID}.
 */
public class ExperimentIDSerializer extends AbstractSerializer<Experiment.ID> {

    /**
     * Singleton instance
     */
    private static final ExperimentIDSerializer INSTANCE =
            new ExperimentIDSerializer();

    /**
     * Constructor
     */
    public ExperimentIDSerializer() {
        super();
    }

    /**
     * Get the instance of serializer
     *
     * @return instance
     */
    public static ExperimentIDSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(Experiment.ID experimentID) {
        if (experimentID == null) {
            return null;
        }

        return UUIDSerializer.get().toByteBuffer(experimentID.getRawID());
    }

    @Override
    public Experiment.ID fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }

        UUID uuid = UUIDSerializer.get().fromByteBuffer(byteBuffer);

        return Experiment.ID.valueOf(uuid);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UUIDTYPE;
    }
}

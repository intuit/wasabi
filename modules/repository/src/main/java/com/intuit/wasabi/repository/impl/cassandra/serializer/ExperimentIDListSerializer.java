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
import com.netflix.astyanax.serializers.ListSerializer;
import org.apache.cassandra.db.marshal.UUIDType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Experiment id list serializer
 */
public class ExperimentIDListSerializer extends AbstractSerializer<List<Experiment.ID>> {

    /**
     * Singleton instance
     */
    private static final ExperimentIDListSerializer INSTANCE =
            new ExperimentIDListSerializer();

    private final ListSerializer<UUID> delegate = new ListSerializer(UUIDType.instance);


    /**
     * Constructor
     */
    public ExperimentIDListSerializer() {
        super();
    }

    /**
     * Get the instance of serializer
     * @return instance of serializer
     */
    public static ExperimentIDListSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(List<Experiment.ID> experimentIDs) {
        if (experimentIDs == null) {
            return null;
        }
        List<UUID> uuidList = new ArrayList<>(experimentIDs.size());
        uuidList.addAll(experimentIDs.stream().map(Experiment.ID::getRawID).collect(Collectors.toList()));
        return delegate.toByteBuffer(uuidList);
    }

    @Override
    public List<Experiment.ID> fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        List<UUID> uuidList = delegate.fromByteBuffer(byteBuffer);
        List<Experiment.ID> experimentIDs = new ArrayList<>(uuidList.size());
        experimentIDs.addAll(uuidList.stream().map(Experiment.ID::valueOf).collect(Collectors.toList()));
        return experimentIDs;
    }

    @Override
    public ComparatorType getComparatorType() {
        return delegate.getComparatorType();
    }
}

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

import com.intuit.wasabi.experimentobjects.Bucket.State;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;

/**
 * Astyanax serializer for
 *
 * @see State
 */
public class BucketStateSerializer extends AbstractSerializer<State> {

    /**
     * Singleton instance
     */
    private static final BucketStateSerializer INSTANCE = new BucketStateSerializer();

    /**
     * Constructor
     */
    public BucketStateSerializer() {
        super();
    }

    /**
     * Get instance
     *
     * @return singleton instance
     */
    public static BucketStateSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(State value) {
        if (value == null) {
            return null;
        }
        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public State fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }

        String value = StringSerializer.get().fromByteBuffer(byteBuffer);
        return State.valueOf(value);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }
}

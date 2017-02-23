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

import com.intuit.wasabi.experimentobjects.Bucket;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;

/**
 * Astyanax serializer for
 *
 * @see Bucket.Label
 */
public class BucketLabelSerializer extends AbstractSerializer<Bucket.Label> {

    /**
     * Single ton instance
     */
    private static final BucketLabelSerializer INSTANCE =
            new BucketLabelSerializer();

    public BucketLabelSerializer() {
        super();
    }

    /**
     * Get singleton instance
     *
     * @return instance
     */
    public static BucketLabelSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(Bucket.Label value) {
        if (value == null) {
            return null;
        }

        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public Bucket.Label fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }

        String result = StringSerializer.get().fromByteBuffer(byteBuffer);
        return Bucket.Label.valueOf(result);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }
}

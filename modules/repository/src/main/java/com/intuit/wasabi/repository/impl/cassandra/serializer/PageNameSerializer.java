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

import com.intuit.wasabi.experimentobjects.Page;
import com.netflix.astyanax.serializers.AbstractSerializer;
import com.netflix.astyanax.serializers.ComparatorType;
import com.netflix.astyanax.serializers.StringSerializer;

import java.nio.ByteBuffer;

/**
 * Serializer for
 * @see Page.Name
 */
public class PageNameSerializer extends AbstractSerializer<Page.Name> {

    /**
     * Singleton instance
     */
    private static final PageNameSerializer INSTANCE =
            new PageNameSerializer();

    /**
     * Constructor
     */
    public PageNameSerializer() {
        super();
    }

    /**
     * Get singleton instance
     * @return instance
     */
    public static PageNameSerializer get() {
        return INSTANCE;
    }

    @Override
    public ByteBuffer toByteBuffer(Page.Name value) {
        if (value == null) {
            return null;
        }
        return StringSerializer.get().toByteBuffer(value.toString());
    }

    @Override
    public Page.Name fromByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        String result = StringSerializer.get().fromByteBuffer(byteBuffer);
        return Page.Name.valueOf(result);
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.UTF8TYPE;
    }

}

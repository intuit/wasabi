/*
 * ******************************************************************************
 *  * Copyright 2016 Intuit
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */
package com.intuit.wasabi.tests.model;

import java.time.LocalDateTime;

/**
 * Created on 6/16/16.
 */

public class EventDateTime {
    String eventLabel;
    LocalDateTime eventDatetime;

    public EventDateTime(String eventLabel, LocalDateTime eventDatetime) {
        this.eventLabel = eventLabel;
        this.eventDatetime = eventDatetime;
    }

    public String getEventLabel() {
        return eventLabel;
    }

    public void setEventLabel(String eventLabel) {
        this.eventLabel = eventLabel;
    }

    public LocalDateTime getEventDatetime() {
        return eventDatetime;
    }

    public void setEventDatetime(LocalDateTime eventDatetime) {
        this.eventDatetime = eventDatetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventDateTime)) return false;

        EventDateTime that = (EventDateTime) o;

        if (eventLabel != null ? !eventLabel.equals(that.eventLabel) : that.eventLabel != null) return false;
        return eventDatetime != null ? eventDatetime.equals(that.eventDatetime) : that.eventDatetime == null;

    }

    @Override
    public int hashCode() {
        int result = eventLabel != null ? eventLabel.hashCode() : 0;
        result = 31 * result + (eventDatetime != null ? eventDatetime.hashCode() : 0);
        return result;
    }
}


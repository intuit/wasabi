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
package com.intuit.wasabi.email;

import static com.intuit.wasabi.email.WasabiEmailFields.APPLICATION_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.BUCKET_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.EMAIL_LINKS;
import static com.intuit.wasabi.email.WasabiEmailFields.EXPERIMENT_ID;
import static com.intuit.wasabi.email.WasabiEmailFields.EXPERIMENT_LABEL;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_AFTER;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_BEFORE;
import static com.intuit.wasabi.email.WasabiEmailFields.FIELD_NAME;
import static com.intuit.wasabi.email.WasabiEmailFields.USER_NAME;

/**
 * This class collects for now the different email types that can be send with there templates,
 * that will then be filled with live by the {@link EmailTextProcessor}
 */
public interface TextTemplates {

    String greeting = "Hi There, \n";
    String goodbye = "\nHappy testing!\n\nThis is a system generated email. Please do not reply to this email.";

    String DEFAULT = greeting + "Some state has changed in your application's experiments.\n" + goodbye;

    String EXPERIMENT_CREATED = greeting + "The experiment \"<" + EXPERIMENT_LABEL + ">\" (<" + EXPERIMENT_ID + ">) "
            + "has been created in your application \"<" + APPLICATION_NAME + ">\" by \"<" + USER_NAME + ">\".\n"
            + goodbye;

    String EXPERIMENT_CHANGED = greeting + "The experiment \"<" + EXPERIMENT_LABEL + ">\" (<" + EXPERIMENT_ID + ">) "
            + "in your application \"<" + APPLICATION_NAME + ">\" has been changed by \"<" + USER_NAME + ">\". The field \"<" + FIELD_NAME + ">\" has "
            + "been changed from \"<" + FIELD_BEFORE + ">\" to \"<" + FIELD_AFTER + ">\".\n"
            + goodbye;

    String APP_ACCESS = greeting + "The user \"<" + USER_NAME + ">\" is requesting access to your application \"<" + APPLICATION_NAME
            + ">\". In order to grant this user access, click on one of the following links (or, if they are not clickable, " +
            "copy one and paste it into the address line of a browser):\n\n<" + EMAIL_LINKS +
            ">\n\nYou will be taken to the Admin UI and required to login.  After you login, the selected access will be granted to the user for that application.\n"
            + goodbye;

    String BUCKET_CREATED = greeting + "A new bucket \"<" + BUCKET_NAME + ">\" has been added to your experiment \"<" + EXPERIMENT_LABEL + ">\" "
            + " in your application \"<" + APPLICATION_NAME + ">\" by \"<" + USER_NAME + ">\".\n"
            + goodbye;

    String BUCKET_CHANGE = greeting + "The bucket \"<" + BUCKET_NAME + ">\" in your experiment \"<" + EXPERIMENT_LABEL + ">\" "
            + "in your application \"<" + APPLICATION_NAME + ">\" has been changed by \"<" + USER_NAME + ">\". They changed the field \"<" + FIELD_NAME + ">\" from "
            + "\"<" + FIELD_BEFORE + ">\" to \"<" + FIELD_AFTER + ">\".\n"
            + goodbye;

}

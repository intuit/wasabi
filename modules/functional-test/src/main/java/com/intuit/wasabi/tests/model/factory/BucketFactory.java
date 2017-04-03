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
package com.intuit.wasabi.tests.model.factory;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.Bucket;
import com.intuit.wasabi.tests.model.Experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A factory for buckets.
 */
public class BucketFactory {

    /**
     * Only used to create unique bucket labels.
     */
    private static int internalId = 0;

    /**
     * Creates a bucket from a JSON String.
     *
     * @param json the JSON String represent the bucket
     * @return a bucket representing the JSON String. {@link Bucket}
     */
    public static Bucket createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, Bucket.class);
    }

    /**
     * Creates a simple bucket with only required values set.
     * This bucket does not belong to the control group by default.
     * The allocation percentage is 1/3.
     *
     * @param experiment the experiment this bucket shall belong to
     * @return the new bucket
     */
    public static Bucket createBucket(Experiment experiment) {
        return createBucket(experiment, false);
    }

    /**
     * Creates a simple bucket with only required values set.
     * The allocation percentage is 1/3.
     *
     * @param experiment the experiment this bucket shall belong to
     * @param isControl  control group status
     * @return the new bucket
     */
    public static Bucket createBucket(Experiment experiment, boolean isControl) {
        return new Bucket(bucketNameColors()[internalId++ % bucketNameColors().length] + internalId, experiment.id, 1.0d / 3.0d, isControl);
    }

    /**
     * Creates a bucket and sets all parameters to some default values.
     *
     * @param experiment the experiment for this bucket
     * @return the new bucket
     */
    public static Bucket createCompleteBucket(Experiment experiment) {
        String description = "A sample bucket.";
        String payload = "<p>Some <span style=\"font-weight: bold;\">heavy<span> payload.</p>";
        String state = Constants.BUCKET_STATE_OPEN;
        return createBucket(experiment).setDescription(description).setPayload(payload).setState(state);
    }

    /**
     * Creates a bucket and sets all parameters to some default values.
     *
     * @param experiment the experiment for this bucket
     * @param isControl  the control group status
     * @return the new bucket
     */
    public static Bucket createCompleteBucket(Experiment experiment, boolean isControl) {
        return createCompleteBucket(experiment).setControl(isControl);
    }

    /**
     * Creates a sample set of N minimal buckets for the given experiment.
     * The allocation percentage for each bucket is 1/N.
     * The first bucket (index 0) is the control group.
     * <p>
     * Minimal means that only the required values are set.
     *
     * @param experiment      the experiment
     * @param numberOfBuckets N, the number of buckets
     * @return a list of N buckets with equally assigned allocation percentages
     */
    public static List<Bucket> createBuckets(Experiment experiment, int numberOfBuckets) {
        double[] percentages = new double[numberOfBuckets];
        Arrays.fill(percentages, 1.0d / numberOfBuckets);
        return createBuckets(experiment, percentages);
    }

    /**
     * Creates {@code allocationPercentages.length} many minimal buckets with their percentages assigned to them.
     * The first bucket (index 0) is the control group.
     * <p>
     * Minimal means that only the required values are set.
     *
     * @param experiment            the experiment
     * @param allocationPercentages an array of allocation percentages
     * @return a list of N buckets with the assigned allocation percentages
     */
    public static List<Bucket> createBuckets(Experiment experiment, double[] allocationPercentages) {
        ArrayList<Bucket> buckets = new ArrayList<>(allocationPercentages.length);
        for (int i = 0; i < allocationPercentages.length; ++i) {
            buckets.add(i, createBucket(experiment).setAllocationPercent(allocationPercentages[i]));
        }
        buckets.get(0).setControl(true);
        return buckets;
    }

    /**
     * Creates {@code allocationPercentages.length} many minimal buckets with their percentages assigned to them.
     * The first bucket (index 0) is the control group. If there are more buckets than labels, the labels are repeated.
     * <p>
     * Minimal means that only the required values are set.
     *
     * @param experiment            the experiment
     * @param allocationPercentages an array of allocation percentages
     * @param labels                the labels for the buckets
     * @return a list of N buckets with the assigned allocation percentages
     */
    public static List<Bucket> createBuckets(Experiment experiment, double[] allocationPercentages, String[] labels) {
        List<Bucket> buckets = createBuckets(experiment, allocationPercentages);
        for (int i = 0; i < buckets.size(); ++i) {
            buckets.get(i).setLabel(labels[i % labels.length]);
        }
        return buckets;
    }

    /**
     * Creates a sample set of N complete buckets for the given experiment.
     * The allocation percentage for each bucket is 1/N.
     * The first bucket (index 0) is the control group.
     * <p>
     * Complete means all values are set to some defaults.
     *
     * @param experiment      the experiment
     * @param numberOfBuckets N, the number of buckets
     * @return a list of N buckets with equally assigned allocation percentages
     */
    public static List<Bucket> createCompleteBuckets(Experiment experiment, int numberOfBuckets) {
        double[] percentages = new double[numberOfBuckets];
        Arrays.fill(percentages, 1.0d / numberOfBuckets);
        return createBuckets(experiment, percentages);
    }

    /**
     * Creates {@code allocationPercentages.length} many complete buckets with their percentages assigned to them.
     * The first bucket (index 0) is the control group.
     * <p>
     * Complete means all values are set to some defaults.
     *
     * @param experiment            the experiment
     * @param allocationPercentages an array of allocation percentages
     * @return a list of N buckets with the assigned allocation percentages
     */
    public static List<Bucket> createCompleteBuckets(Experiment experiment, double[] allocationPercentages) {
        ArrayList<Bucket> buckets = new ArrayList<>(allocationPercentages.length);
        for (int i = 0; i < allocationPercentages.length; ++i) {
            buckets.add(i, createCompleteBucket(experiment)
                    .setAllocationPercent(allocationPercentages[i]));
        }
        buckets.get(0).setControl(true);
        return buckets;
    }

    /**
     * Creates {@code allocationPercentages.length} many complete buckets with their percentages assigned to them.
     * The first bucket (index 0) is the control group. If there are more buckets than labels, the labels are repeated.
     * <p>
     * Complete means all values are set to some defaults.
     *
     * @param experiment            the experiment
     * @param allocationPercentages an array of allocation percentages
     * @param labels                the bucket labels
     * @return a list of N buckets with the assigned allocation percentages
     */
    public static List<Bucket> createCompleteBuckets(Experiment experiment, double[] allocationPercentages, String[] labels) {
        List<Bucket> buckets = createCompleteBuckets(experiment, allocationPercentages);
        for (int i = 0; i < buckets.size(); ++i) {
            buckets.get(i).setLabel(labels[i]);
        }
        return buckets;
    }

    public static List<Bucket> createCompleteBuckets(Experiment experiment,
                                                     double[] allocationPercentages, String[] labels, boolean[] control) {
        List<Bucket> buckets = createCompleteBuckets(experiment, allocationPercentages);
        for (int i = 0; i < buckets.size(); ++i) {
            buckets.get(i).setLabel(labels[i]);
            buckets.get(i).setControl(control[i]);
        }
        return buckets;
    }

    /**
     * Returns an array of colors for easy bucket labeling. Can be used for labels. Note that this will result in up to
     * {@code 18} unique bucket names.
     *
     * @return bucket labels
     */
    public static String[] bucketNameColors() {
        return new String[]{
                "red", "green", "blue", "yellow", "white",
                "magenta", "black", "navy", "gray", "orange",
                "silver", "gold", "pink", "violet", "brown",
                "olive", "purple", "copper",
        };
    }

    /**
     * Returns an array of colors for easy bucket labeling. Can be used for labels. Note that this will result in up to
     * up to {@link #bucketNameColors() bucketNameColors().length}{@code  * 5} unique bucket names, thus the length can be
     * smaller than {@code number}.
     *
     * @param number the number of bucket labels
     * @return {@code number} bucket labels
     */
    public static String[] bucketNameColors(int number) {
        String[] colors = bucketNameColors();
        String[] modifiers = new String[]{
                "", "dark ", "bright ", "metallic ", "shy ",
        };

        int maxSize = Math.min(number, colors.length * modifiers.length);

        List<String> allColors = new ArrayList<>(maxSize);
        while (allColors.size() < maxSize) {
            for (String modifier : modifiers) {
                for (String color : colors) {
                    allColors.add(modifier + color);
                    if (allColors.size() == maxSize) {
                        break;
                    }
                }
            }
        }
        return allColors.toArray(new String[allColors.size()]);
    }
}

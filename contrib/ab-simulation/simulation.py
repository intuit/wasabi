# A Python 2.7 library to simulate AB tests and analyze results.

###############################################################################
# Copyright 2016 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################


import math, time, numpy, pandas
import scipy.stats
import matplotlib.pyplot as plt
from pandas import DataFrame, Series


### estimate and confidence interval methods for binomial distributions

def WaldEstimate(successes, trials, significance=0.05):
    z = scipy.stats.norm.ppf(1 - significance / 2.)
    value = 1.*successes / trials
    interval = z*math.sqrt(value * (1 - value) / trials)
    return value, interval

# Agresti-Coull
def ACEstimate(successes, trials, significance=0.05):
    z = scipy.stats.norm.ppf(1 - significance / 2.)
    value = 1.*(successes + z**2 / 2.) / (trials + z**2)
    interval = z*math.sqrt(value*(1 - value) / (trials + z**2))
    return value, interval


### scaling significance to account for multiple test reads
# fuction used to fit simulated results
def modifiedSignificance(numImpressions, significance, firstLook, scale):
    numIndependentReads = (numpy.log(1.*numImpressions / firstLook) /
                           numpy.log(scale)) + 1
    modSig = 1 - (1 - significance)**numIndependentReads
    return modSig


### helper methods

# if the null hypothesis is rejected
def rejectNull(estimate, rate):
    value = estimate[0]
    interval = estimate[1]
    return (rate < value - interval) or (rate > value + interval)


### core simulation
# simulates a single-proportion Z-test
# calculates type I error for a single test read (at end)
#   and cumulative error for continuous test reads (after every impression)
def simulate(rates, significances, impressions, numTrials, firstLook=None,
             estimateFunction=WaldEstimate, seed=None):
    """ simulate a single-proportion Z-test

    Args:
        rate (list): success rates
        significances (list): significance values (1 - confidence)
        impressions (int or list): maximum impressions or list of number of
            impressions
        numTrials (int): number of independent simulations to aggregate over
        firstLook (int): first impression at which experiment is evaluated for
            continuous evaluation
            (defaults to 1)
        estimateFunction (function): binomal approximation to use
            (defaults to Wald)
        seed (int, optional): seed for random number generation
            (defaults to current time)

    Returns:
       avgRejects (DataFrame): simulate single test read at end
       avgAnyRejects (DataFrame): simulate conintuous test read after every impression
       Both DataFrames contain the estimate and uncertainty on the type I error
       (incorrect rejection of null hypothesis) for each rate, significance, and
       impression value. Results are aggregated across numTrials independent
       experiments.
    """

    trials = range(numTrials)
    base = [rates, significances, trials]
    mi = pandas.MultiIndex.from_product(base, names=['rate', 'significance',
                                                     'trial'])

    if seed is None:
        numpy.random.seed(int(time.time()))
    else:
        numpy.random.seed(seed)

    if type(impressions) == int:
        points = range(1, impressions + 1)
    else:
        points = impressions

    avgRejects = None
    avgAnyRejects = None

    for n in points:
        if n <= 0:
            raise ValueError("All values in impressions must be positive.")
        draws = DataFrame(numpy.random.random([n, len(rates) *
                                                  len(significances) *
                                                  len(trials)]),
                          columns=mi)
        draws.index = range(1, n + 1)

        successes = draws.copy()
        rejects = draws.copy()

        for rate in rates:
            successes[rate] = draws[rate].applymap(lambda x: int(x < rate))
        cumSuccesses = successes.apply(numpy.core.fromnumeric.cumsum, raw=True)
        cumImpressions = successes.index.values
        for rate in rates:
            for sig in significances:
                for trial in trials:
                    vals = Series(zip(cumSuccesses.loc[:, (rate, sig, trial)].values,
                                      cumImpressions))
                    vals.index = cumImpressions
                    rejects.loc[:, (rate, sig, trial)] = vals.apply(lambda x: \
                        int(rejectNull(estimateFunction(x[0], x[1], sig), rate)))

        if firstLook is not None:
            anyRejects = rejects.ix[firstLook:].max()

        # apply binomial approximation to estimate type I error rate
        if avgRejects is None:
            avgRejects = rejects[-1:]. \
                         groupby(axis=1, level=['rate', 'significance']). \
                         sum(). \
                         applymap(lambda x: estimateFunction(x, numTrials))
        else:
            avgRejects.ix[n] = rejects[-1:]. \
                               groupby(axis=1, level=['rate', 'significance']). \
                               sum(). \
                               applymap(lambda x: estimateFunction(x, numTrials)). \
                               values[0]

        # apply binomial approximation to estimate cumulative type I error rate
        if firstLook is not None:
            if avgAnyRejects is None:
                avgAnyRejects = DataFrame(anyRejects. \
                                          groupby(level=['rate', 'significance']). \
                                          sum(). \
                                          map(lambda x: estimateFunction(x, numTrials))). \
                                transpose()
                avgAnyRejects.index = avgRejects.index.copy()
            else:
                avgAnyRejects.ix[n] = anyRejects. \
                                      groupby(level=['rate', 'significance']). \
                                      sum(). \
                                      map(lambda x: estimateFunction(x, numTrials)). \
                                      values

    return avgRejects, avgAnyRejects


### plotting

def plotRejects(avgRejects, avgAnyRejects):
    impressions = avgRejects.index
    rates = avgRejects.columns.levels[0]
    sigs = avgRejects.columns.levels[1]

    colors = [(1, 0, 0), (0, 1, 0), (0, 0, 1), (1, 1, 0), (0, 1, 1), (1, 0, 1),
              (1, 1, 1)]
    levels = [(val + 1.)/len(sigs) for val in range(len(sigs))]
    fig = plt.figure(figsize=(10, 6))
    ax = fig.add_subplot(1, 1, 1)
    rateIndex = 0
    for (rate, baseColor) in zip(rates, colors[:len(rates)]):
        for (sig, level) in zip(sigs, levels):
            color = tuple(val*level for val in baseColor)
            rejectsVals = avgRejects[rate][sig].apply(lambda x: x[0]).values
            anyRejectsVals = avgAnyRejects[rate][sig]. \
                             apply(lambda x: x[0]).values

            ax.plot(impressions, rejectsVals, color=color,
                    label="rate: %.3f; significance: %.3f" % (rate, sig))
            ax.plot(impressions, anyRejectsVals, color=color, marker='x',
                    ls='--')
    for sig in sigs:
        ax.plot(impressions, [sig]*len(impressions), color='k', ls=':')

    ax.set_xlim(0, max(impressions))
    ax.set_ylim(0, 1)
    ax.set_title('Average Reject Rate', fontsize=24)
    ax.set_xlabel('# Impressions', fontsize=20)
    ax.set_ylabel('% Rejects', fontsize=20)
    ax.legend(loc=1, fontsize=18)
    plt.tick_params(axis='both', which='major', labelsize=16)

    plt.show()

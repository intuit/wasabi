import requests
import json

def get_impression(application, experiment, user):
    """
    Records an impression for the given user and experiment.

    Args:
        application: the application the experiment runs in
        experiment:  the running experiment for which the impression should be recorded
        user:        the user who should be assigned
    """

    urlAssignment = "http://abtesting.intuit.com/api/v1/events/applications/%s/experiments/%s/users/%s" %(application, experiment, user);
    headers = {'content-type': 'application/json'}
    events = {'events':[{'name':'IMPRESSION'}]}

    r = requests.post(urlAssignment, data = json.dumps(events), headers=headers)

    if r.status_code == 201: # when the request returns 201 the impression was recorded correctly
        return True
    return False

if __name__ == "__main__":
    application = 'ApplicationName'
    experiment = 'ExperimentName'
    user = 'UserName'

    print('Impression recorded' if get_impression(application, experiment, user) else 'Impression not recorded')

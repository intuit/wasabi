package com.intuit.wasabi.assignmentlogger;

import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;


/*
* The assgnment logger interface
*/
public interface AssignmentLogger {


  /**
   *
   * @param assignmentEnvelop     the assginment evelop needed to be logged
   */
  public void logAssignemntEnvelope(AssignmentEnvelopePayload assignmentEnvelop);


}

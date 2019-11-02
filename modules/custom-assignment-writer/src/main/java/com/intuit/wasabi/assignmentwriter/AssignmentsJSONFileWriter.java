package com.intuit.wasabi.assignmentwriter;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.assignmentobjects.AssignmentEnvelopePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writer utility class responsible for serializing assignment details to JSON format and write it to a file
 */
public final class AssignmentsJSONFileWriter implements AssignmentsFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentsJSONFileWriter.class);

    private final String filePath;

    @Inject
    public AssignmentsJSONFileWriter(@Named(AssignmentWriterAnnotations.ASSIGNMENTS_FILE_PATH) String filePath) {
        if ((filePath == null) || (filePath.length() == 0))
            throw new IllegalArgumentException();

        this.filePath = filePath;
    }

    public void writeAssignmentDetails(AssignmentEnvelopePayload assignmentEnvelopePayload) {
        logger.debug("Writing the user( " + assignmentEnvelopePayload.getUserID() + " )"
                     + " assignment details to file located at: " + filePath);
        File file = new File(filePath);
        FileWriter fr = null;
        BufferedWriter br = null;
        String dataWithNewLine = assignmentEnvelopePayload.toJson() + System.getProperty("line.separator");
        try{
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);
            br.write(dataWithNewLine);
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

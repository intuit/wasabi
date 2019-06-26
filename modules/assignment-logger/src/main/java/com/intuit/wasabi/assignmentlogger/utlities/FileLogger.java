package com.intuit.wasabi.assignmentlogger.utlities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

public class FileLogger {

private static final Logger LOGGER = getLogger(FileLogger.class);



  /**
   * Use BufferedWriter when number of write operations are more
   * It uses internal buffer to reduce real IO operations and saves time
   */
  public static void writeDateToFile(String data, String filePath, String fileName) {
      LOGGER.debug("Writing into => {}",  filePath + fileName);
      File file = new File(filePath + fileName);

      FileWriter fr = null;
      BufferedWriter br = null;
      String dataWithNewLine = data + System.getProperty("line.separator");
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

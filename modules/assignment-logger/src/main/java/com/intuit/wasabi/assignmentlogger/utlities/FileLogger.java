package com.intuit.wasabi.assignmentlogger.utlities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileLogger {


  /**
   * Use BufferedWriter when number of write operations are more
   * It uses internal buffer to reduce real IO operations and saves time
   */
  public static void writeDateToFile(String data) {
      File file = new File("/Users/yassingamal/Desktop/assignment.txt");
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

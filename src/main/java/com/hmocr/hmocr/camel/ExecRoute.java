package com.hmocr.hmocr.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.*;

@Component
public class ExecRoute extends RouteBuilder {
    static Logger log = LoggerFactory.getLogger(ExecRoute.class);
    public final String TESS_EXE = "tesseract";
    public final String TESS_LANG = "-l ell";
    public final String TESS_OUTPUT = "/mnt/HMOCR/tess-ocr/output/";

    public final String CONV_DENSITY="-density 300";
    public final String CONV_DEPT="-depth 8";
    public final String CONV_OTHER_COMMANDS="-strip -background white";
    public final String CONV_OUT="/mnt/HMOCR/convert/TEMP/";


    @Override
    public void configure() throws Exception {

        //todo create paths for image processing
        from("file:/mnt/HMOCR/convert/input?noop=true")
                .process(exchange -> {
                    File fileIn = exchange.getIn().getBody(File.class);
                    File fileInPDF = exchange.getIn().getBody(File.class);

                    String tempFile= fileIn.getPath();
                    log.info("Processing file: " + String.valueOf(fileIn));
                    if (fileIn != null) {
                         tempFile= fileIn.getPath();
                        System.out.println("tempFile: "+tempFile);

                             if(tempFile.contains(".pdf") ||
                                tempFile.contains(".docx")||
                                tempFile.contains(".doc"))  {
                                 tempFile = FilenameUtils.removeExtension(tempFile);
                                 tempFile=CONV_OUT+FilenameUtils.getName(tempFile)+".tiff";
                                 System.out.println("FilenameUtils: "+ FilenameUtils.getName(tempFile));
                                 System.out.println("Check tempfile : "+ tempFile);
                        }

                        System.out.println("FILEINPDF= "+fileInPDF);
                          String command = "convert "
                                          + CONV_DENSITY            + " "
                                          + fileInPDF               + " "
                                          + CONV_DEPT               + " "
                                          + CONV_OTHER_COMMANDS     + " "
                                          + tempFile;
                        System.out.println("Command created= "+command);
                        String output = executeCommand(command);
                        System.out.println("This is the output "+output);
                    }
                });
//.*.xml
                from("file:/mnt/HMOCR/convert/TEMP?noop=true")
//                .to("file:/mnt/HMOCR/convert/tiff?noop=true");

                        .to("file:/mnt/HMOCR/convert/tiff?noop=true")
                        .process(exchange -> {
                            File fileIn = exchange.getIn().getBody(File.class);
//                    if (isCompletelyWritten(createFile(fileIn))) {
//                        System.out.println("file is being processed, wait");
//                        //todo thread wait if file is not completely written
//                    } else {
//                        System.out.println("File written");
//                    }
                        })
        .to("file:/mnt/HMOCR/tess-ocr/input?noop=true")
//        from("file:/mnt/tess-ocr/input?noop=true")
                .process(exchange -> {
                    File fileIn = exchange.getIn().getBody(File.class);
                    log.info("Processing file: " + String.valueOf(fileIn));
                    if (fileIn != null) {

                        String temp = FilenameUtils.removeExtension(fileIn.getName());
                        System.out.println("TEMP FILE "+temp);
                    System.out.println("fileIn = " + fileIn);
                    String command = "tesseract " + fileIn + " " + TESS_LANG + " " + TESS_OUTPUT+temp+" "+"pdf";
                    String output = executeCommand(command);
                    System.out.println(output);
                   // File ocrFile = new File("/mnt/HMOCR/tess-ocr/output/yeah.pdf");

                        File tempFile=new File("/mnt/HMOCR/tess-ocr/output/"+temp+".pdf");

                        System.out.println("new ocrFile= "+tempFile);
                        copyCompleted(tempFile.getPath());
                        // FileUtils.copyFile(ocrFile,tempFile);
//                        if(new File("/mnt/HMOCR/tess-ocr/output/yeah.pdf").exists())
//                        {
//                            fileIn=new File("/mnt/HMOCR/tess-ocr/yeah.pdf");
//                            System.out.println("fileIn.getAbsolutePath() "+fileIn.getAbsolutePath());
//                        }
                       // System.out.println("File created: " + createFile(fileIn));
                    }
                });
    }

    public void copyCompleted(String filePath) {
        String fileStr = filePath;
        File ff = new File(fileStr);
        boolean copyCompleted = false;
        if (ff.exists()) {
            while (true) {
                RandomAccessFile ran = null;
                try {
                    ran = new RandomAccessFile(ff, "rw");
                    copyCompleted = true;
                    break;
                } catch (Exception ex) {
                    System.out.println("  still copying ........... " + ex.getMessage());
                } finally {
                    if (ran != null) try {
                        ran.close();
                    } catch (IOException ex) {

                    }
                    ran = null;
                }
            }
            if (copyCompleted) {
                System.out.println("Copy Completed ........................");
            }
        }
    }







    private boolean isCompletelyWritten(File file) {
        RandomAccessFile stream = null;
        try {
            stream = new RandomAccessFile(file, "rw");
            return true;
        } catch (Exception e) {
            log.info("Skipping file " + file.getName() + " for this iteration due it's not completely written");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Exception during closing file " + file.getName());
                }
            }
        }
        return false;
    }

    private String executeCommand(String command) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}

package sncr.analysis.execution;

import cmd.CommandLineHandler;
import files.HFileOperations;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sncr.metadata.analysis.AnalysisExecutionHandler;
import sncr.metadata.analysis.AnalysisProvHelper;
import sncr.metadata.engine.ResponseConverter;

import java.io.OutputStream;


/**
 * Created by srya0001 on 2/23/2017.
 */
public class AnalysisExecutionRunner {

    protected static final Logger logger = LoggerFactory.getLogger(AnalysisExecutionRunner.class.getName());

    public static void main(String[] args) throws Exception {

        CommandLineHandler cli = new CommandLineHandler();
        try{
            CommandLine cl  = buildCMD(cli, args);
            String inData = HFileOperations.readFile(cl.getOptionValue('a'));
            OutputStream outStream = HFileOperations.writeToFile(cl.getOptionValue('o'));
            System.out.println("Start data processing:\n input analysis ID: " + cl.getOptionValue('a') + "\nOutput path: " + cl.getOptionValue('o'));
            ExecutionTaskHandler er = new ExecutionTaskHandler(1);
            AnalysisProvHelper exec = AnalysisProvHelper.apply(inData);
            if ( !exec.requestsParsed()){
                logger.error("The document is not parsable. Exit");
            }
            java.util.ArrayList<java.util.HashMap<String, Object>> headers =
                    ResponseConverter.convertToJavaMapList(exec.handleExecuteRequest());
            headers.forEach( h ->
            {
                String analysisId = (String) h.get("analysisId");
                String nodeId = (String) h.get("NodeId");
                if (analysisId == null || analysisId.isEmpty())
                {
                    System.err.println("Could not get analysisID from retrieved result. Skip analysisID = " + analysisId);
                    return;
                }
                AnalysisExecutionHandler aeh = new AnalysisExecutionHandler(nodeId, analysisId);
                try {
                    er.startSQLExecutor(aeh);
                    String analysisResultId = er.getPredefResultRowID(analysisId);
                    er.waitForCompletion(analysisId, aeh.getWaitTime());
                    aeh.handleResult(outStream);
                    logger.debug("Execution: AnalysisID = " + analysisId + ", Result Row ID: " + analysisResultId );
                } catch (Exception e) {
                    logger.error("Executing exception: ", e);
                }
            });
            if (outStream != null ) {
                outStream.flush();
                outStream.close();
            }
        } catch(org.apache.commons.cli.ParseException e){
            System.err.println(e.getMessage());
            cli.printUsage("MetadataUtility");
        } catch(Exception e) {
            System.err.println("ERROR: Exception: " + e.getMessage());
            System.err.println("\r\nException stack trace:");
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static CommandLine buildCMD(CommandLineHandler cli, String args[]) throws org.apache.commons.cli.ParseException {
        cli.addOptionToHandler("analysis", true,
               "Analysis ID",
               "analysis",
               "i",
                true);


        cli.addOptionToHandler("outputFile", true,
                "Full path and file name for output file",
                "output-file",
                "o",
                true);

        return cli.parse(args);
    }

}

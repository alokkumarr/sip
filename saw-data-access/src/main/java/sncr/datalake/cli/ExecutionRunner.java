package sncr.datalake.cli;

import cmd.CommandLineHandler;
import files.HFileOperations;
import org.apache.commons.cli.CommandLine;
import sncr.datalake.exceptions.ErrorCodes;
import sncr.datalake.handlers.AnalysisNodeExecution;
import sncr.metadata.analysis.AnalysisNode;
import sncr.metadata.engine.context.SelectModels;
import sncr.metadata.semantix.SemanticNode;

import java.io.OutputStream;

import static java.lang.System.exit;
import static java.lang.System.out;

/**
 * Created by srya0001 on 5/25/2017.
 */
public class ExecutionRunner {


    public static void main(String[] args) throws Exception {

        CommandLineHandler cli = new CommandLineHandler();
        try{
            CommandLine cl  = buildCMD(cli, args);
            String analysis_id = cl.getOptionValue('i');
            String semantic_id = cl.getOptionValue('s');

            int rowLimit = Integer.valueOf(cl.getOptionValue('l'));
            rowLimit = (rowLimit == 0)?100:rowLimit;
            String execResId = cl.getOptionValue('r');

            OutputStream outStream = HFileOperations.writeToFile(cl.getOptionValue('o'));
            out.println("Start data processing:\n input analysis ID: " + cl.getOptionValue('i') + "\nOutput path: " + cl.getOptionValue('o'));

            if (analysis_id != null && !analysis_id.isEmpty()) {
                AnalysisNode an = AnalysisNode.apply(analysis_id);
                AnalysisNodeExecution ane = new AnalysisNodeExecution(an, true, execResId);
                ane.executeAndSave(outStream, rowLimit);
                ane.printSample(outStream);
            }else
            if (semantic_id != null && !semantic_id.isEmpty()) {

                String sql = cl.getOptionValue('q');
                if (sql == null || sql.isEmpty()) {
                    throw new Exception(ErrorCodes.IncorrectExecutionCall().toString() + ": Semantic layer execution requires SQL");
                }
                SemanticNode sn = SemanticNode.apply(semantic_id, SelectModels.everything().id());
                sncr.datalake.handlers.SemanticNodeExecution sne = new sncr.datalake.handlers.SemanticNodeExecution(sn, true);
                sne.executeSQL(sql, rowLimit);
                String result = sne.getDataSampleAsString(sne.metric());
                if (outStream != null)
                    outStream.write(result.getBytes());
            }
            if (outStream != null ) {
                outStream.flush();
                outStream.close();
            }
        } catch(org.apache.commons.cli.ParseException e){
            System.err.println(e.getMessage());
            cli.printUsage("ExecutionUtility");
        } catch(Exception e) {
            System.err.println("\r\nException stack trace:");
            e.printStackTrace();
            exit(1);
        }
        exit(0);
    }

    private static CommandLine buildCMD(CommandLineHandler cli, String args[]) throws org.apache.commons.cli.ParseException {
        cli.addOptionToHandler("analysis", true,
                "Analysis ID",
                "analysis",
                "i",
                false);

        cli.addOptionToHandler("semantic", true,
                "Semantic ID",
                "semantic",
                "s",
                false);


        cli.addOptionToHandler("outputFile", true,
                "Full path and file name for output file",
                "output-file",
                "o",
                true);

        cli.addOptionToHandler("rowLimit", true,
                "Number of rows to preview result",
                "row-limit",
                "l",
                true);

        cli.addOptionToHandler("ExecResult", true,
                "Execution result",
                "exec-result",
                "r",
                false);

        cli.addOptionToHandler("SQL", true,
                "Semantic layer SQL",
                "sql",
                "q",
                false);

        return cli.parse(args);
    }

}

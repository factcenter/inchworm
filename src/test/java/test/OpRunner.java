package test;

import joptsimple.OptionSpec;
import org.factcenter.inchworm.app.Run;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Run a single op multiple times to test performance.
 */
public class OpRunner extends Run {

    /**
     * Number of ops to run.
     */
    int num;


    public OpRunner(String progName, PrintStream errOut) {
        super(progName, errOut);
    }

    public void parseAndSetup(String[] args) throws IOException {
        createOptions();

        OptionSpec<String> opOp = parser.accepts("op", "The op to test")
                .withRequiredArg().ofType(String.class);


        OptionSpec<Integer> wordsize = parser.accepts("word", "Word size (in bits)")
                .withRequiredArg().ofType(Integer.class).defaultsTo(8);

        OptionSpec<Integer> regptr = parser.accepts("regptr", "Register pointer size in bits (log_2 of the number of registers)")
                .withRequiredArg().ofType(Integer.class).defaultsTo(8);

        OptionSpec<Integer> ramptr = parser.accepts("ramptr", "Ram pointer size in bits (log_2 of the number of RAM words)")
                .withRequiredArg().ofType(Integer.class).defaultsTo(8);

        OptionSpec<Integer> numOpt =  parser.accepts("num", "Number of ops to run (all will be run in one instruction)")
                .withRequiredArg().ofType(Integer.class).defaultsTo(10);


        parse(args);


        party = options.has(opOp) ? 0 : 1;

        if (party == 0) {
            num = options.valueOf(numOpt);
            String op = options.valueOf(opOp);
            StringBuilder prog = new StringBuilder();

            prog.append(".header\n");
            prog.append("wordsize: ").append(options.valueOf(wordsize)).append("\n");
            prog.append("regptrsize: ").append(options.valueOf(regptr)).append("\n");
            prog.append("ramptrsize: ").append(options.valueOf(ramptr)).append("\n");
            prog.append("romptrsize: 1\n");
            prog.append("instruction: ");

            for (int i = 0; i < num; ++i)  {
                prog.append(op).append(" ");
            }
            prog.append("\n");

            // We don't need a code section; NOP and op with "real" parameters take the same time.

            logger.debug("Generated header:\n{}", prog);
            progCodeStream = new ByteArrayInputStream(prog.toString().getBytes());
        }

        setupInchworm();
    }

    public void run() throws IOException {
        // First run for startup times
        long starttime = System.currentTimeMillis();
        run(1);
        long endtime = System.currentTimeMillis();
        if (party == 0)
            logger.info("Total time (inital run) = {}; time per op = {}", endtime - starttime, (endtime - starttime)/num);

        starttime = System.currentTimeMillis();
        run(1);
        endtime = System.currentTimeMillis();

        if (party == 0)
            logger.info("Total time = {}; time per op = {}", endtime - starttime, (endtime - starttime)/num);
    }

    public static void main(String[] args) {
        OpRunner runner = new OpRunner(OpRunner.class.getCanonicalName(), System.err);


        try {
            runner.parseAndSetup(args);
            runner.run();
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            System.exit(1);
        }
    }
}

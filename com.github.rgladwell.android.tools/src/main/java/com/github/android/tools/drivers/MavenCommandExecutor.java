/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.android.tools.drivers;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MavenCommandExecutor implements CommandExecutor {

    /**
     * Instance of a plugin logger.
     */
    private Log logger;

    /**
     * Standard Out
     */
    private StreamConsumer stdOut;

    /**
     * Standard Error
     */
    private ErrorStreamConsumer stdErr;

    /**
     * Process result
     */
    private int result;

    public void setLogger(Log logger) {
        this.logger = logger;
    }

    long pid;

    private Commandline commandline;

	public void executeCommand(String executable, List<String> commands)
            throws ExecutionException {
        executeCommand(executable, commands, null, true);
    }

	public void executeCommand(String executable, List<String> commands, boolean failsOnErrorOutput)
            throws ExecutionException {
        executeCommand(executable, commands, null, failsOnErrorOutput);
    }

	public void executeCommand(String executable, List<String> commands, File workingDirectory,
                               boolean failsOnErrorOutput)
            throws ExecutionException {
        if (commands == null) {
            commands = new ArrayList<String>();
        }
        stdOut = new StreamConsumerImpl();
        stdErr = new ErrorStreamConsumer();

        commandline = new Commandline();
        commandline.setExecutable(executable);
        commandline.addArguments(commands.toArray(new String[commands.size()]));
        if (workingDirectory != null && workingDirectory.exists()) {
            commandline.setWorkingDirectory(workingDirectory.getAbsolutePath());
        }
        try {
            result = CommandLineUtils.executeCommandLine(commandline, stdOut, stdErr);
            if (logger != null) {
                logger.debug("ANDROID-040-000: Executed command: Commandline = " + commandline +
                        ", Result = " + result);
            } else {
                System.out.println("ANDROID-040-000: Executed command: Commandline = " + commandline +
                        ", Result = " + result);
            }
            if ((failsOnErrorOutput && stdErr.hasError()) || result != 0) {
                throw new ExecutionException("ANDROID-040-001: Could not execute: Command = " +
                        commandline.toString() + ", Result = " + result);
            }
        } catch (CommandLineException e) {
            throw new ExecutionException(
                    "ANDROID-040-002: Could not execute: Command = " + commandline.toString() + ", Error message = " + e.getMessage());
        }
        setPid(commandline.getPid());
    }

	public int getResult() {
        return result;
    }

	public String getStandardOut() {
        return stdOut.toString();
    }

	public String getStandardError() {
        return stdErr.toString();
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getPid() {
        return pid;
    }

    /**
     * Provides behavior for determining whether the command utility wrote anything to the Standard Error Stream.
     * NOTE: I am using this to decide whether to fail the NMaven build. If the compiler implementation chooses
     * to write warnings to the error stream, then the build will fail on warnings!!!
     */
    class ErrorStreamConsumer
            implements StreamConsumer {

        /**
         * Is true if there was anything consumed from the stream, otherwise false
         */
        private boolean error;

        /**
         * Buffer to store the stream
         */
        private StringBuffer sbe = new StringBuffer();

        public ErrorStreamConsumer() {
            if (logger == null) {
                System.out.println("ANDROID-040-003: Error Log not set: Will not output error logs");
            }
            error = false;
        }

        public void consumeLine(String line) {
            sbe.append(line);
            if (logger != null) {
                logger.info(line);
            }
            error = true;
        }

        /**
         * Returns false if the command utility wrote to the Standard Error Stream, otherwise returns true.
         *
         * @return false if the command utility wrote to the Standard Error Stream, otherwise returns true.
         */
        public boolean hasError() {
            return error;
        }

        /**
         * Returns the error stream
         *
         * @return error stream
         */
        public String toString() {
            return sbe.toString();
        }
    }

    /**
     * StreamConsumer instance that buffers the entire output
     */
    class StreamConsumerImpl
            implements StreamConsumer {

        private DefaultConsumer consumer;

        private StringBuffer sb = new StringBuffer();

        public StreamConsumerImpl() {
            consumer = new DefaultConsumer();
        }

        public void consumeLine(String line) {
            sb.append(line);
            if (logger != null) {
                consumer.consumeLine(line);
            }
        }

        /**
         * Returns the stream
         *
         * @return the stream
         */
        public String toString() {
            return sb.toString();
        }
    }

}

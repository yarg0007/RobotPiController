package com.yarg0007.robotpicontroller.ssh.commands;

import java.util.Objects;

public class CommandExpectPair {

    private String commandToExecute;
    private String expectedResult;

    /**
     * Set the command to execute and the expected value that must be contained in the output of the command execution.
     * @param commandToExecute Command to be executed.
     * @param expectedResult Expected value that the output must contain.
     */
    public CommandExpectPair(String commandToExecute, String expectedResult) {
        this.commandToExecute = commandToExecute;
        this.expectedResult = expectedResult;
    }

    /**
     * Command to execute.
     * @return Command to be executed.
     */
    public String getCommandToExecute() {
        return commandToExecute;
    }

    /**
     * Expectation of output post command execution to contain.
     * @return Expected string that must be contained in output.
     */
    public String getExpectedResult() {
        return expectedResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandExpectPair that = (CommandExpectPair) o;
        return Objects.equals(commandToExecute, that.commandToExecute) &&
                Objects.equals(expectedResult, that.expectedResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandToExecute, expectedResult);
    }
}

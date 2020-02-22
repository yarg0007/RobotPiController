package com.yarg0007.robotpicontroller.ssh;

import com.yarg0007.robotpicontroller.ssh.commands.CommandExpectPair;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SshCommandPayloadTest {

    @Test
    public void commandListOrderPreserved() {

        List<CommandExpectPair> expectedPairs = new ArrayList<>();
        expectedPairs.add(new CommandExpectPair("A", "A"));
        expectedPairs.add(new CommandExpectPair("B", "B"));
        expectedPairs.add(new CommandExpectPair("C", "C"));
        SshCommandPayload payload = new SshCommandPayload("test", expectedPairs);
        List<CommandExpectPair> actualPairs = payload.getCommands();

        Assert.assertEquals(expectedPairs, actualPairs);
    }

    @Test
    public void initializeWithoutCommandsYieldsEmptyList() {

        List<CommandExpectPair> expectedPairs = new ArrayList<>();
        SshCommandPayload payload = new SshCommandPayload("test");
        List<CommandExpectPair> actualPairs = payload.getCommands();

        Assert.assertEquals(expectedPairs, actualPairs);
    }

    @Test
    public void addCommandToEmptyListYieldsSingleItemList() {

        List<CommandExpectPair> expectedPairs = new ArrayList<>();
        expectedPairs.add(new CommandExpectPair("A", "A"));

        SshCommandPayload payload = new SshCommandPayload("test");
        payload.addCommandPair(new CommandExpectPair("A", "A"));
        List<CommandExpectPair> actualPairs = payload.getCommands();

        Assert.assertEquals(expectedPairs, actualPairs);
    }

    @Test
    public void addCommandToExistingListYieldsOrginalListPlusOne() {

        List<CommandExpectPair> expectedPairs = new ArrayList<>();
        expectedPairs.add(new CommandExpectPair("A", "A"));
        expectedPairs.add(new CommandExpectPair("B", "B"));

        SshCommandPayload payload = new SshCommandPayload("test", expectedPairs);
        payload.addCommandPair(new CommandExpectPair("C", "C"));
        List<CommandExpectPair> actualPairs = payload.getCommands();

        expectedPairs.add(new CommandExpectPair("C", "C"));

        Assert.assertEquals(expectedPairs, actualPairs);
    }
}

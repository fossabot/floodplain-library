package com.dexels.kafka.streams.testdata;

import com.dexels.kafka.streams.base.StreamRuntime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestFileOperations {

//	private StreamRuntime runtime;

    @Before
    public void setUp() throws Exception {
//		runtime = new StreamRuntime();
    }

    @Test
    public void testNameMulti() {
        Assert.assertEquals("mies", StreamRuntime.nameFromFileName("/aap/noot/mies.xml"));
    }

    @Test
    public void testNameSingle() {
        Assert.assertEquals("mies", StreamRuntime.nameFromFileName("mies.xml"));
    }

}

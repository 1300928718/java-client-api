/*
 * Copyright (c) 2012 Cosmin Stejerean.
 *
 * Distributed under the MIT license: http://opensource.org/licenses/MIT
 */

package com.offbytwo.jenkins;

import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;


public class JenkinsServerIntegration {
    private JenkinsHttpClient client;
    private JenkinsServer server;

    @Before
    public void setUp() throws Exception {
        client = new JenkinsHttpClient(new URI("http://localhost:8080"));
        server = new JenkinsServer(client);
    }

    @Test
    public void shouldReturnListOfJobs() throws Exception {
        assertEquals("Hello", server.getJobs().get(0).getName());
    }

    @Test
    public void shouldReturnBuildsForJob() throws Exception {
        JobWithDetails job = server.getJobs().get(0).details();
        assertEquals(1, job.getBuilds().get(0).getNumber());
    }

    @Test
    public void shouldReturnBuildStatusForBuild() throws Exception {
        JobWithDetails job = server.getJobs().get(0).details();
        BuildWithDetails build = job.getBuilds().get(0).details();
        assertEquals(BuildResult.SUCCESS, build.getResult());
    }
}

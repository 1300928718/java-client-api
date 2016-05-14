/*
 * Copyright (c) 2013 Rising Oak LLC.
 *
 * Distributed under the MIT license: http://opensource.org/licenses/MIT
 */

package com.offbytwo.jenkins.model;

import static com.google.common.collect.Lists.transform;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.offbytwo.jenkins.client.util.EncodingUtils;
import com.offbytwo.jenkins.helper.Range;

public class JobWithDetails extends Job {

    String displayName;

    boolean buildable;

    List<Build> builds;

    Build firstBuild;

    Build lastBuild;

    Build lastCompletedBuild;

    Build lastFailedBuild;

    Build lastStableBuild;

    Build lastSuccessfulBuild;

    Build lastUnstableBuild;

    Build lastUnsuccessfulBuild;

    int nextBuildNumber;

    boolean inQueue;

    QueueItem queueItem;

    List<Job> downstreamProjects;

    List<Job> upstreamProjects;

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBuildable() {
        return buildable;
    }

    public boolean isInQueue() {
        return inQueue;
    }

    /**
     * This method will give you back the builds of a particular job.<br/>
     * <b>Note: Jenkins limits the number of results to a maximum of 100 builds
     * which you will get back.</b>. In case you have more than 100 build you
     * won't get back all builds via this method. In such cases you need to use
     * {@link #getAllBuilds()}.
     * 
     * @return the list of {@link Build}.
     */
    public List<Build> getBuilds() {
        return transform(builds, new Function<Build, Build>() {
            @Override
            public Build apply(Build from) {
                return buildWithClient(from);
            }
        });
    }

    /**
     * This method will give you back all builds which exists independent of the
     * number. You should be aware that this can be much in some cases if you
     * have more than 100 builds which is by default limited by Jenkins
     * {@link #getBuilds()}. This method limits it to particular information
     * which can be later used to get supplemental information about a
     * particular build {@link Build#details()} to reduce the amount of data
     * which needed to be transfered.
     * 
     * @return the list of {@link Build}.
     * @throws IOException
     *             In case of failure.
     * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-30238">Jenkins
     *      Issue</a>
     */
    public List<Build> getAllBuilds() throws IOException {
        String path = "/";

        try {
            List<Build> builds = client.get(path + "job/" + EncodingUtils.encode(this.getName())
                    + "?tree=allBuilds[number[*],url[*],queueId[*]]", AllBuilds.class).getAllBuilds();

            return transform(builds, new Function<Build, Build>() {
                @Override
                public Build apply(Build from) {
                    return buildWithClient(from);
                }
            });
        } catch (HttpResponseException e) {
            // TODO: Thinks about a better handline if the job does not exist?
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                // TODO: Check this if this is necessary or a good idea?

                return null;
            }
            throw e;
        }

    }

    /**
     *
     * <ul>
     * <li>{M,N}: From the M-th element (inclusive) to the N-th element
     * (exclusive).</li>
     * <li>{M,}: From the M-th element (inclusive) to the end.</li>
     * <li>{,N}: From the first element (inclusive) to the N-th element
     * (exclusive). The same as {0,N}.</li>
     * <li>{N}: Just retrieve the N-th element. The same as {N,N+1}.</li>
     * </ul>
     * 
     * <b>Note: At the moment there seemed to be no option to get the number of
     * existing builds for a job. The only option is to get all builds via
     * {@link #getAllBuilds()}.</b>
     * 
     * @param range {@link Range}
     * @return The list of builds defined by the given range.
     * @throws IOException in case of an error.
     */
    public List<Build> getAllBuilds(Range range) throws IOException {
        String path = "/" + "job/" + EncodingUtils.encode(this.getName())
                + "?tree=allBuilds[number[*],url[*],queueId[*]]";

        try {
            List<Build> builds = client.get(path + range.getRangeString(), AllBuilds.class).getAllBuilds();

            return transform(builds, new Function<Build, Build>() {
                @Override
                public Build apply(Build from) {
                    return buildWithClient(from);
                }
            });
        } catch (HttpResponseException e) {
            // TODO: Thinks about a better handline if the job does not exist?
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                // TODO: Check this if this is necessary or a good idea?

                return null;
            }
            throw e;
        }
    }

    private Build buildWithClient(Build from) {
        Build ret = from;
        if (from != null) {
            ret = new Build(from);
            ret.setClient(client);
        }
        return ret;
    }

    public Build getFirstBuild() {
        return buildWithClient(firstBuild);
    }

    public Build getLastBuild() {
        return buildWithClient(lastBuild);
    }

    public Build getLastCompletedBuild() {
        return buildWithClient(lastCompletedBuild);
    }

    public Build getLastFailedBuild() {
        return buildWithClient(lastFailedBuild);
    }

    public Build getLastStableBuild() {
        return buildWithClient(lastStableBuild);
    }

    public Build getLastSuccessfulBuild() {
        return buildWithClient(lastSuccessfulBuild);
    }

    public Build getLastUnstableBuild() {
        return buildWithClient(lastUnstableBuild);
    }

    public Build getLastUnsuccessfulBuild() {
        return buildWithClient(lastUnsuccessfulBuild);
    }

    public int getNextBuildNumber() {
        return nextBuildNumber;
    }

    public List<Job> getDownstreamProjects() {
        return transform(downstreamProjects, new JobWithClient());
    }

    public List<Job> getUpstreamProjects() {
        return transform(upstreamProjects, new JobWithClient());
    }

    public QueueItem getQueueItem() {
        return this.queueItem;
    }

    public Build getBuildByNumber(final int buildNumber) {

        Predicate<Build> isMatchingBuildNumber = new Predicate<Build>() {

            @Override
            public boolean apply(Build input) {
                return input.getNumber() == buildNumber;
            }
        };

        Optional<Build> optionalBuild = Iterables.tryFind(builds, isMatchingBuildNumber);
        return optionalBuild.orNull() == null ? null : buildWithClient(optionalBuild.orNull());
    }

    private class JobWithClient implements Function<Job, Job> {
        @Override
        public Job apply(Job job) {
            job.setClient(client);
            return job;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (buildable ? 1231 : 1237);
        result = prime * result + ((builds == null) ? 0 : builds.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((downstreamProjects == null) ? 0 : downstreamProjects.hashCode());
        result = prime * result + ((firstBuild == null) ? 0 : firstBuild.hashCode());
        result = prime * result + (inQueue ? 1231 : 1237);
        result = prime * result + ((lastBuild == null) ? 0 : lastBuild.hashCode());
        result = prime * result + ((lastCompletedBuild == null) ? 0 : lastCompletedBuild.hashCode());
        result = prime * result + ((lastFailedBuild == null) ? 0 : lastFailedBuild.hashCode());
        result = prime * result + ((lastStableBuild == null) ? 0 : lastStableBuild.hashCode());
        result = prime * result + ((lastSuccessfulBuild == null) ? 0 : lastSuccessfulBuild.hashCode());
        result = prime * result + ((lastUnstableBuild == null) ? 0 : lastUnstableBuild.hashCode());
        result = prime * result + ((lastUnsuccessfulBuild == null) ? 0 : lastUnsuccessfulBuild.hashCode());
        result = prime * result + nextBuildNumber;
        result = prime * result + ((queueItem == null) ? 0 : queueItem.hashCode());
        result = prime * result + ((upstreamProjects == null) ? 0 : upstreamProjects.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        JobWithDetails other = (JobWithDetails) obj;
        if (buildable != other.buildable)
            return false;
        if (builds == null) {
            if (other.builds != null)
                return false;
        } else if (!builds.equals(other.builds))
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (downstreamProjects == null) {
            if (other.downstreamProjects != null)
                return false;
        } else if (!downstreamProjects.equals(other.downstreamProjects))
            return false;
        if (firstBuild == null) {
            if (other.firstBuild != null)
                return false;
        } else if (!firstBuild.equals(other.firstBuild))
            return false;
        if (inQueue != other.inQueue)
            return false;
        if (lastBuild == null) {
            if (other.lastBuild != null)
                return false;
        } else if (!lastBuild.equals(other.lastBuild))
            return false;
        if (lastCompletedBuild == null) {
            if (other.lastCompletedBuild != null)
                return false;
        } else if (!lastCompletedBuild.equals(other.lastCompletedBuild))
            return false;
        if (lastFailedBuild == null) {
            if (other.lastFailedBuild != null)
                return false;
        } else if (!lastFailedBuild.equals(other.lastFailedBuild))
            return false;
        if (lastStableBuild == null) {
            if (other.lastStableBuild != null)
                return false;
        } else if (!lastStableBuild.equals(other.lastStableBuild))
            return false;
        if (lastSuccessfulBuild == null) {
            if (other.lastSuccessfulBuild != null)
                return false;
        } else if (!lastSuccessfulBuild.equals(other.lastSuccessfulBuild))
            return false;
        if (lastUnstableBuild == null) {
            if (other.lastUnstableBuild != null)
                return false;
        } else if (!lastUnstableBuild.equals(other.lastUnstableBuild))
            return false;
        if (lastUnsuccessfulBuild == null) {
            if (other.lastUnsuccessfulBuild != null)
                return false;
        } else if (!lastUnsuccessfulBuild.equals(other.lastUnsuccessfulBuild))
            return false;
        if (nextBuildNumber != other.nextBuildNumber)
            return false;
        if (queueItem == null) {
            if (other.queueItem != null)
                return false;
        } else if (!queueItem.equals(other.queueItem))
            return false;
        if (upstreamProjects == null) {
            if (other.upstreamProjects != null)
                return false;
        } else if (!upstreamProjects.equals(other.upstreamProjects))
            return false;
        return true;
    }

}

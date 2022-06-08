package com.nwellinghoff.jenkins.plugins.multibranch.buildstrategy;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cloudbees.jenkins.plugins.bitbucket.BitbucketGitSCMRevision;
import com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMSource;
import com.cloudbees.jenkins.plugins.bitbucket.BranchSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.PullRequestSCMHead;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;

import hudson.Extension;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import org.kohsuke.stapler.DataBoundConstructor;

public class IncludeRegExMatchBranchBuildStrategy extends BranchBuildStrategy {
    
	private static final Logger logger = Logger.getLogger(IncludeRegExMatchBranchBuildStrategy.class.getName());
    private final String includedRegions;
    private final boolean ignoreBranchIfPR;

    public String getincludedRegions() {
		return includedRegions;
	}
	public boolean getignoreBranchIfPR() { return ignoreBranchIfPR; }


    @DataBoundConstructor
    public IncludeRegExMatchBranchBuildStrategy(String includedRegions, boolean ignoreBranchIfPR) {
        this.includedRegions = includedRegions;
        this.ignoreBranchIfPR = ignoreBranchIfPR;
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        public String getDisplayName() {
            return "Run build on any regex match strategy";
        }
    }

    @Override
    public boolean isAutomaticBuild(@NonNull SCMSource source, @NonNull SCMHead head, @NonNull SCMRevision currRevision, SCMRevision lastBuiltRevision, SCMRevision lastSeenRevision, @NonNull TaskListener listener) {
        BitbucketSCMSource src = (BitbucketSCMSource) source;
        String credentialsId = src.getCredentialsId();
        String owner = src.getRepoOwner();
        String repository = src.getRepository();
        String serverUrl = src.getServerUrl();
        StandardCredentials credentials = Helper.lookupScanCredentials(src.getOwner(), credentialsId, serverUrl);
        UsernamePasswordCredentialsImpl creds = (UsernamePasswordCredentialsImpl) credentials;
        //if PR parse out pull request id
        List<String> files = new ArrayList<>();
        if(currRevision.getHead() instanceof PullRequestSCMHead){
            files = Helper.getFileListForPullRequest(serverUrl,creds,owner,repository,((PullRequestSCMHead) currRevision.getHead()).getId());
        }else if (currRevision instanceof BitbucketGitSCMRevision && currRevision.getHead() instanceof BranchSCMHead){
            if(!ignoreBranchIfPR || !Helper.checkIfBranchIsAssociatedWithOpenPR(serverUrl,creds,owner,repository,currRevision.getHead().getName()))
                files = Helper.getFileListForRevision(serverUrl,creds,owner,repository,((BitbucketGitSCMRevision) currRevision).getHash());
            else
                return false;
        }else{
            logger.severe("Unsupported Git server action " + currRevision.getHead().getClass().getCanonicalName());
        }


        List<String> regExList = Arrays.stream(
                includedRegions.split("\n")).map(e -> e.trim()).collect(Collectors.toList());

        logger.info(String.format("Excluded regions: %s", regExList.toString()));
        // No regions excluded run the build
        if(regExList.isEmpty())
            return true;

        for (String regex: regExList){
            if(Helper.doesMatch(regex,files))
                return true;
        }
        return false;
    }
}

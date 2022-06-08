package com.igalg.jenkins.plugins.multibranch.buildstrategy;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketAuthenticator;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class Helper {

    private static final Logger logger = Logger.getLogger(Helper.class.getName());

    public static List<String> getFileListForPullRequest(String baseUrl, UsernamePasswordCredentialsImpl creds, String owner, String repo, String pullRequestId){
        String url = baseUrl + "/rest/api/1.0/projects/" + owner + "/repos/" + repo + "/pull-requests/" + pullRequestId + ".diff";
        String gitDiff = null;
        List<String> filePaths = new ArrayList<String>();

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()){
            HttpGet request = new HttpGet(url);

            // add request headers
            String encoding = Base64.getEncoder().encodeToString((creds.getUsername() + ":" + creds.getPassword()).getBytes());
            //request.addHeader("Authorization", "Bearer " + token);
            request.addHeader("Authorization", "Basic " + encoding);
            request.addHeader(HttpHeaders.ACCEPT, "text/plain");

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // return it as a String
                gitDiff = EntityUtils.toString(entity);
                filePaths = splitRawGitDiffIntoFilePaths(gitDiff);
            }
        }catch (Exception e){
            logger.severe(e.getMessage());
        }
        return filePaths;
    }

    public static List<String> getFileListForRevision(String baseUrl, UsernamePasswordCredentialsImpl creds, String owner, String repo, String hash){
        String url = baseUrl + "/rest/api/1.0/projects/" + owner + "/repos/" + repo + "/commits/" + hash + "/diff";
        String gitDiff = null;
        List<String> filePaths = new ArrayList<String>();

        try(CloseableHttpClient httpClient = HttpClientBuilder.create().build()){
            HttpGet request = new HttpGet(url);

            // add request headers
            String encoding = Base64.getEncoder().encodeToString((creds.getUsername() + ":" + creds.getPassword()).getBytes());
            //request.addHeader("Authorization", "Bearer " + token);
            request.addHeader("Authorization", "Basic " + encoding);
            request.addHeader(HttpHeaders.ACCEPT, "text/plain");

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // return it as a String
                gitDiff = EntityUtils.toString(entity);
                filePaths = splitRawGitDiffIntoFilePaths(gitDiff);
            }
        }catch (Exception e){
            logger.severe(e.getMessage());
        }
        return filePaths;
    }

    public static List<String> splitRawGitDiffIntoFilePaths(String rawDiff){
        List<String> filePaths = new ArrayList<>();
        String[] splitDiff = rawDiff.split("\n");
        for (String line : splitDiff) {
            if(line.startsWith("diff --git")){
                String[] filePathDiffs = line.split("src://")[1].split("dst://");
                filePaths.addAll(Arrays.asList(filePathDiffs));
            }
        }
        return filePaths;
    }

    public static boolean doesMatch(String regex, List<String> filePaths){
        for( String path: filePaths){
            if(path.matches(regex))
                return true;
        }
        return false;
    }

    public static StandardCredentials lookupScanCredentials(@CheckForNull Item context, @CheckForNull String scanCredentialsId, String serverUrl) {
        return Util.fixEmpty(scanCredentialsId) == null ? null : (StandardCredentials) CredentialsMatchers.firstOrNull(com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(StandardCredentials.class, context, context instanceof Queue.Task ? Tasks.getDefaultAuthenticationOf((Queue.Task) context) : ACL.SYSTEM, URIRequirementBuilder.fromUri(serverUrl).build()), CredentialsMatchers.allOf(new CredentialsMatcher[]{CredentialsMatchers.withId(scanCredentialsId), AuthenticationTokens.matcher(BitbucketAuthenticator.authenticationContext(serverUrl))}));
    }
}

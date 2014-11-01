package maps.hudson.plugin.xfpanel;

import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.claim.ClaimBuildAction;
import hudson.plugins.claim.ClaimTestAction;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import maps.hudson.plugin.xfpanel.XFPanelView;

/**
 * Represents a job to be shown on the panel
 * 
 * Intermediates access to data available for the given Job
 * 
 * @author jrenaut
 */
public final class XFPanelEntry extends XFPanelView {

    private Job<?, ?> job;
    private String backgroundColor;
    private String color;
    private String colorFade = "";
    private Boolean broken;
    private Boolean building = false;
    private String completionTimestampString = "";
    private Calendar completionTimestamp;

    /**
     * C'tor
     * @param job the job to be represented
     */
    public XFPanelEntry(XFPanelView view, Job<?, ?> job) {
        super(view.getDisplayName(), view.getNumColumns());
        this.job = job;
        this.findStatus();
        this.setTimes();
    }

    /**
     * @return the job
     */
    public Job<?, ?> getJob() {
        return this.job;
    }

    /**
     * @return the job's name
     */
    public String getName() {
        String label = job.getDisplayName(); //upercase removed pooja
        if (getShowDescription() == true && !job.getDescription().isEmpty()) {
            label += ": " + job.getDescription();
        }
        return label;
    }

    /**
     * @return if this job is queued for build
     */
    public Boolean getQueued() {
        return this.job.isInQueue();
    }

    /**
     * @return the job's queue number, if any
     */
    public Integer getQueueNumber() {
        // placeInQueue==null right after deserialization because it's transient
        return placeInQueue==null ? null : placeInQueue.get(this.job.getQueueItem());
    }

    public AbstractBuild<?, ?> getLastBuild() {
        Run<?, ?> run = this.job.getLastBuild();
        if (run instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> lastBuild = (AbstractBuild<?, ?>)run;
            return lastBuild;
        }
        return null;
    }

    private void setTimes() {
        Run<?, ?> run = this.job.getLastCompletedBuild();
        if (run instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> lastBuild = (AbstractBuild<?, ?>)run;
            if (lastBuild != null) {
                this.completionTimestamp = lastBuild.getTimestamp();
                this.completionTimestampString = lastBuild.getTimestampString();
            }
        }
    }

    public void setCompletionTimestamp(Calendar completionTimestamp) {
        this.completionTimestamp = completionTimestamp;
    }

    public Calendar getCompletionTimestamp() {
        return this.completionTimestamp;
    }

    public void setCompletionTimestampString(String completionTimestampString) {
        this.completionTimestampString = completionTimestampString;
    }

    public String getCompletionTimestampString() {
        return this.completionTimestampString;
    }

    /**
     * @return background color for this job
     */
    public String getBackgroundColor() {
        return this.backgroundColor;
    }

    /**
     * @return foreground color for this job
     */
    public String getColor() {
        return this.color;
    }

    /**
     * @return fadeout image name for this job
     */
    public String getColorFade() {
        return this.colorFade;
    }

    /**
     * @return true se o último build está quebrado
     */
    public Boolean getBroken() {
        return this.broken;
    }

    /**
     *  @return 1 on success
     */
    public Boolean getShowResponsibles() {
        if (BlameState == Blame.NOTATALL)
            return false;
        return true;
    }

    /**
     * @return true if this job is currently being built
     */
    public Boolean getBuilding() {
        return this.building;
    }

    /**
     * @return the URL for the last build
     */
    public String getUrl() {
        return this.job.getUrl() + "lastBuild";
        
    }
    
    /**
     * @return the URL for the last build
     * @throws IOException 
     *//*  didnt work get build parameters
    public String getBuildparam() {
		return "1. lastBuild/parameters=" + this.job.getUrl() + "lastBuild"
				+ "parameters" + "2. getACL()=" + this.job.getACL()
				+ "3. getProperty('env')="+this.job.getProperty("env")
		        + "4. getProperties.get('env')="+this.job.getProperties().get("env")
		        + "5. getPermalinks().get('env')"+this.job.getPermalinks().get("env")
		        + "6. getAbsoluteUrl="+this.job.getAbsoluteUrl()
		        + "7. getBuildStatusUrl="+this.job.getBuildStatusUrl()
		        + "8. getDescription="+this.job.getDescription()
		        + "9. getAllProperties.get(1)="+this.job.getAllProperties().get(1)
		        + "10. getLastBuild.no="+this.job.getLastBuild().getNumber();
    }
    
    public int getLastStableBuildNo() {
		return this.job.getLastStableBuild().getNumber();
    }
    
    public String getEnvfrompermalinks() {
		return this.job.getPermalinks().get("env").toString();
		      
    }*/
    
   
   public String getEnv() throws IOException {
		String pageUrl = this.job.getAbsoluteUrl()+"/lastBuild"+"/parameters/";   //getUrl() wont work because Pagesource doesnt work on partial url
		if(StringUtils.isEmpty(pageUrl))
			return "in getAbEnv, null/empty pageUrl";
		String pageSource = getPageSource(pageUrl);
		if(StringUtils.isEmpty(pageSource))
			return "in getAbEnv, null/empty pageSource";
		String regExEnv = "<td class=\"setting-name\">env<\\/td>[\\s\\S]*?value=\"([\\s\\S]*?)\"";
		String env = getStringAfterPattern(pageSource, regExEnv, 1);
		
		return env;

	}
   
  
   
   /*** Pooja -- start **/
   
   public String getCurrentTag(){
	   String pageUrl = this.job.getAbsoluteUrl()+"/lastBuild"+"/parameters/";   //getUrl() wont work because Pagesource doesnt work on partial url
	   if(StringUtils.isEmpty(pageUrl))
		   return "in curent tag, null/empty pageUrl";
	   String pageSource;
	   String tag;
	   try {
		   pageSource = getPageSource(pageUrl);

		   if(StringUtils.isEmpty(pageSource))
			   return "in curent tag, null/empty pageSource";
		   String regExEnv = "<td class=\"setting-name\">tagtotest<\\/td>[\\s\\S]*?value=\"([\\s\\S]*?)\"";
		   tag = getStringAfterPattern(pageSource, regExEnv, 1);

		   return tag;
	   } catch (Exception e) {
		   // TODO Auto-generated catch block
		   tag= e.getMessage();
	   }
	return tag;
 }

	public String getLastSucceedNo(){
		return String.valueOf(this.job.getLastStableBuild().getNumber());
		
	}
	
	public String getLastSucceedTag() throws IOException {
		int lastSuccessfulNo = this.job.getLastStableBuild().getNumber();
		if(lastSuccessfulNo<=0)
			return "no successful build";
		
			String pageUrl = this.job.getAbsoluteUrl()+ "/"
					+ this.job.getLastStableBuild().getNumber()
					+ "/parameters/";
			String pageSource = getPageSource(pageUrl);
			String regExTag = "<td class=\"setting-name\">tagtotest<\\/td>[\\s\\S]*?value=\"([\\s\\S]*?)\"";
			String tag = getStringAfterPattern(pageSource, regExTag, 1);
			
			String regExEnv = "<td class=\"setting-name\">env<\\/td>[\\s\\S]*?value=\"([\\s\\S]*?)\"";
			String env = getStringAfterPattern(pageSource, regExEnv, 1);
            tag =StringUtils.isEmpty(tag)?"":"last working tag : "+tag+" on env : "+env;
			return tag;
	}
    
    public  String getStringAfterPattern(String sentence, String pattern,
			int groupIndex) {
		String str = "";
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sentence);
		while (m.find()) {
			str = m.group(groupIndex);
			//System.out.println("found-" + str); // for try
		}
		return str.trim();
	}
    
    private static String getPageSource(String urlStr) throws IOException {
		String username = "";
        String password = "";

        URL url = new URL(urlStr);

        URLConnection conn = url.openConnection();
        
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        conn.setRequestProperty ("Authorization", basicAuth);
        
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				conn.getInputStream(), "UTF-8"));
		String inputLine;
		StringBuilder a = new StringBuilder();
		while ((inputLine = in.readLine()) != null)
			a.append(inputLine);
		in.close();

		return a.toString();
	}
    /**
     * @return the URL for the workspace
     */
    public String getWsurl() {
        return this.job.getUrl() + "ws";
        
    }

   /*** Pooja -- end **/
    
    
    
   
    /**
     * @return a list will all the currently building runs for this job.
     */
    public List<Run<? , ?>> getBuildsInProgress() {
        List<Run<?, ?>> runs = new ArrayList<Run<?, ?>>();
        Run<?, ? > run = this.job.getLastBuild();
        if (run.isBuilding()) {
            runs.add(run);
        }

        Run<?, ?> prev = run.getPreviousBuildInProgress();
        while (prev != null) {
            runs.add(prev);
            prev = prev.getPreviousBuildInProgress();
        }
        return runs;
    }

    /**
     * @return total tests executed
     */
    public int getTestCount() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getTotalCount() : 0;
        }
        return 0;
    }

    /**
     * @return total failed tests
     */
    public int getFailCount() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getFailCount() : 0;
        }
        return 0;
    }

    /**
     * @return total successful tests
     */
    public int getSuccessCount() {
        return this.getTestCount() - this.getFailCount();
    }

    public int getLastCompletedBuildNumber() {
        return job.getLastCompletedBuild().getNumber();
    }

    public String getLastCompletedBuildTimestampString() {
        return job.getLastCompletedBuild().getTimestampString();
    }

    /**
     * @return number of failed builds since the last successful build
     * @author Niko Mahle
     */
    public int getNumberOfFailedBuilds() {
        Run<?, ?> lastSuccess = this.job.getLastSuccessfulBuild();
        Run<?, ?> lastCompiled = this.job.getLastCompletedBuild();
        if (lastSuccess == null || lastCompiled == null) return 0;
        int numberOfFailedBuilds = lastCompiled.getNumber() - lastSuccess.getNumber();
        return numberOfFailedBuilds;
    }

    /**
     * @return difference between this job's last build successful tests and the previous'
     */
    public String getDiff() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            Run<?, ?> previous = this.getLastSuccessfulFrom(run);
            if (previous != null) {
                AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
                AbstractTestResultAction<?> prevTests = previous.getAction(AbstractTestResultAction.class);
                if (tests != null && prevTests != null) {
                    int currentSuccess = tests.getTotalCount() - tests.getFailCount();
                    int prevSuccess = prevTests.getTotalCount() - prevTests.getFailCount();
                    return Functions.getDiffString(currentSuccess-prevSuccess);
                }
            }
        }
        return "";
    }

    /**
     * @param run a run
     * @return the last successful run prior to the given run 
     */
    private Run<?, ?> getLastSuccessfulFrom(Run<?, ?> run) {
        Run<?, ?> r = run.getPreviousBuild();
        while (r != null
                && (r.isBuilding() || r.getResult() == null || r.getResult()
                        .isWorseThan(Result.UNSTABLE))) {
            r = r.getPreviousBuild();
        }
        return r;
    }

    /**
     * Elects culprit(s)/responsible(s) for a broken build by choosing the last commiter of a given build 
     * 
     * @return the culprit(s)/responsible(s)
     */
    public HashSet<User> getCulpritFromBuild( AbstractBuild<?, ?> build ){
        HashSet<User> r = new HashSet<User>();
        for (Entry e : build.getChangeSet()) {
            r.add(e.getAuthor());
        }
        return r;
    }

    public String convertCulpritsToString(HashSet<User> input) {
        String output = "";
        Iterator<User> it = input.iterator();

        int i=0;
        for (; it.hasNext(); i++) {
            if (i < getMaxAmmountOfResponsibles()) {
                output += it.next().getFullName() + ((it.hasNext()) ? ", " : "");
            } else {
                it.next();
            }
        }
        if (i > getMaxAmmountOfResponsibles()){
            output += "... <"+ (i-getMaxAmmountOfResponsibles()) + " more>";
        }
        if (!output.isEmpty())
            return output;
        return " - ";
    }

    public String getCulprits() {
        if (BlameState == Blame.ONLYFIRSTFAILEDBUILD) {
            Run<?, ?> run = this.job.getLastStableBuild(); //getLastSuccessfulBuild();
            if ( run == null ){ // if there aren't any successful builds
                run = this.job.getFirstBuild(); 
            } else {
                run = run.getNextBuild();
            }
            if (run instanceof AbstractBuild<?, ?>) {
                AbstractBuild<?, ?> firstFailedBuild = (AbstractBuild<?, ?>) run;
                return convertCulpritsToString( getCulpritFromBuild( firstFailedBuild ) );
            }
        } else if (BlameState == Blame.ONLYLASTFAILEDBUILD) {
            Run<?, ?> run = this.job.getLastFailedBuild();
            if (run instanceof AbstractBuild<?, ?>) {
                AbstractBuild<?, ?> lastFailedBuild = (AbstractBuild<?, ?>) run;
                return convertCulpritsToString( getCulpritFromBuild( lastFailedBuild ) );
            }
        } else if (BlameState == Blame.EVERYINVOLVED) {
            AbstractBuild<?, ?> build = this.getLastBuild();
            HashSet<User> BlameList = new HashSet<User>( build.getCulprits() );
            return convertCulpritsToString( BlameList );
        }
        return " -";
    }

    /**
     * If the claims plugin is installed, this will return ClaimBuildAction
     * 
     * @return claim on the build / null
     */
    private ClaimBuildAction getClaimAction() {
        ClaimBuildAction claimAction = null;

        if (Hudson.getInstance().getPlugin("claim") != null) {
            Run<?, ?> lastBuild = this.job.getLastBuild();
            if (lastBuild != null && lastBuild.isBuilding()) {
                // claims can only be made against builds once they've finished,
                // so check the previous build if currently building.
                lastBuild = lastBuild.getPreviousBuild();
            }
            if (lastBuild != null && lastBuild instanceof AbstractBuild<?, ?>) {
                List<ClaimBuildAction> claimActionList = lastBuild.getActions(ClaimBuildAction.class);
                if (claimActionList.size() == 1) {
                    claimAction = claimActionList.get(0);
                }
            }
        }
        return claimAction;
    }

    /**
     *
     * @return whether build is claimed or not
     */
    public boolean isClaimed() {
        boolean result = false;
        if (getIsClaimPluginInstalled()) {
            ClaimBuildAction cba = getClaimAction();
            if (cba != null) {
                result = cba.isClaimed();
            }
            if (result == false){
                final int claimedTests = getNumClaimedTests();
                final int totalTests = getFailCount();
                if (totalTests > 0 && claimedTests == totalTests) {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * If the claims plugin is installed, this will get details of the claimed
     * build failures.
     *
     *
     * @return details of any claims for the broken build, or null if nobody has
     *         claimed this build.
     */
    public String getClaimInfo() {
        ClaimBuildAction claimAction = getClaimAction();
        if (claimAction != null) {
            if (claimAction.isClaimed()) {
                String name = claimAction.getClaimedByName();
                // String reason = claimAction.getReason();
                if (name != null){
                    return name;
                }
            }
        }
        return "";
    }

    public hudson.tasks.junit.TestResult getClaimedTestCases(){
        if (Hudson.getInstance().getPlugin("claim") != null) {
            Run<?, ?> lastBuild = job.getLastBuild();
            if (lastBuild == null) {
                return null;
            }
            // if building check previous status
            if (lastBuild.isBuilding()){
                lastBuild = (AbstractBuild<?, ?>) lastBuild.getPreviousBuild();
                if (lastBuild == null){
                    return null;
                }
            }
            List<Action> claimTestActionList = lastBuild.getActions();
            List<TestResultAction> results = lastBuild.getActions(TestResultAction.class);
            if ( results == null || claimTestActionList == null || results.size() == 0) {
                return null;
            }
            return results.get(0).getResult();
        }
        return null;
    }

    public String getClaimInfoByTestCases(){
        hudson.tasks.junit.TestResult testResult = getClaimedTestCases();
        if (testResult == null) {
            return "";
        }

        String claimers = "";
        Set<String> claimerNames = new HashSet<String>();
        for (CaseResult result : testResult.getFailedTests()) {
            ClaimTestAction claimTestAction = result.getTestAction(ClaimTestAction.class);
            if (claimTestAction != null) {
                if (claimTestAction.isClaimed() == true) {
                    String claimer = claimTestAction.getClaimedBy();
                    if (claimer != null && claimer != "") {
                        if (!claimerNames.contains(claimer)) {
                            claimerNames.add(claimer);
                            if (claimers != "") {
                                claimers += ", ";
                            }
                            claimers += claimer;
                        }
                    }
                }
            }
        }
        if (claimers == null || claimers == "") {
            String buildClaimer = "";
            buildClaimer = getClaimInfo();
            if (buildClaimer == null || buildClaimer == "") {
                return "";
            }
            return "Build claimed by: " + buildClaimer;
        }
        return "Claimed by: " + claimers;
    }

    public int getNumClaimedTests() {
        hudson.tasks.junit.TestResult testResult = getClaimedTestCases();
        if (testResult != null) {
            int numClaimedTests = 0;
            for (CaseResult result : testResult.getFailedTests()) {
                ClaimTestAction claimTestAction = result.getTestAction(ClaimTestAction.class);
                if (claimTestAction != null) {
                    if (claimTestAction.isClaimed() == true) {
                        numClaimedTests++;
                    }
                }
            }
            return numClaimedTests;
        }
        return -1;
    }

    /**
     * Returns number of failed tests or number of unclaimed failed tests
     */
    public String getNumberOfTests(){
        final int failedTests = getFailCount();
        if (failedTests == 0 && getShowZeroTestCounts() == false) {
                return "";
        }
        if (getReplaceNumberOfTestCases()) {
            final int claimedTests = getNumClaimedTests();
            if (claimedTests >= 0) {
                return Integer.toString(failedTests - claimedTests);
            }
        }
        return Integer.toString(failedTests);
    }

    /**
     * @return color to be used to show the test diff
     */
    public String getDiffColor() {
        String diff = this.getDiff().trim();
        if (diff.length() > 0) {
            if (diff.startsWith("-")) {
                return "#FF0000";
            } else {
                return "#00FF00";
            }
        }
        return "#" + getBuildFontColor();
    }

    /**
     * @return the percentage of successful tests versus the total
     */
    public String getSuccessPercentage() {
        if (this.getTestCount() > 0) {
            Double perc = (this.getSuccessCount() / (this.getTestCount() * 1D));
            return NumberFormat.getPercentInstance().format(perc);
        }
        return "";
    }

    public String getBuildStatus(AbstractBuild<?, ?> build){
        if (build == null){
            return "UNBUILT";
        }
        if (build.isBuilding()) {
            build = (AbstractBuild<?, ?>) build.getPreviousBuild();
            return getBuildStatus(build);
        }
        Result result = build.getResult();
        if (result != null) {
            return result.toString();
        }
        return "UNKNOWN";
    }

    public boolean isBuildSuccessful() {
        AbstractBuild<?, ?> build = this.getLastBuild();
        if (build != null) {
            String buildStatus = getBuildStatus(build);
            return buildStatus.equals("SUCCESS");
        }
        return false;
    }

    public boolean isBuildUnstable(){
        AbstractBuild<?, ?> build = this.getLastBuild();
        if (build != null) {
            String buildStatus = getBuildStatus(build);
            return buildStatus.equals("UNSTABLE");
        }
        return false;
    }

    /**
     * Determines some information of the current job like which colors use, whether it's building or not or broken.
     */
    private void findStatus() {
        switch (this.job.getIconColor()) {
        case BLUE_ANIME:
            this.building = true;
        case BLUE:
            this.backgroundColor = getColors().getOkBG(); 
            this.color = colors.getOkFG();
            this.colorFade = "build-fade-ok.png";
            this.broken = false;
            break;
        case YELLOW_ANIME:
            this.building = true;
        case YELLOW:
            this.backgroundColor = getColors().getFailedBG(); 
            this.color = colors.getFailedFG();
            this.colorFade = "build-fade-fail.png";
            this.broken = false;
            break;
        case RED_ANIME:
            this.building = true;
        case RED:
            this.backgroundColor = getColors().getBrokenBG(); 
            this.color = colors.getBrokenFG();
            this.colorFade = "build-fade-broken.png";
            this.broken = true;
            break;
        case GREY_ANIME:
        case DISABLED_ANIME:
        case ABORTED_ANIME:
            this.building = true;
        default:
            this.backgroundColor = getColors().getOtherBG(); 
            this.color = colors.getOtherFG();
            this.colorFade = "build-fade-other.png";
            this.broken = true;
        }
    }
}

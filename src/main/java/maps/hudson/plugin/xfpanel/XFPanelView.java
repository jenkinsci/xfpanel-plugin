package maps.hudson.plugin.xfpanel;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.lang.Math;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Represents an eXtreme Feedback Panel View.
 * 
 * Thanks to Mark Howard and his work on the Radiator View Plugin from which this was based.
 *  
 * @author jrenaut
 */
public class XFPanelView extends ListView {

    private Integer numColumns = 2;
    private Integer refresh = 3;
    private Boolean fullHD = false;
    private Integer guiHeight = 205;
    private Integer guiJobFont = 80;
    private Integer guiFailFont = 150;
    private Integer guiInfoFont = 30;
    private Integer guiBuildFont = 30;
    private Integer guiClaimFont = 20;

    private Boolean showDescription = false;
    private Boolean showBrokenBuildCount = false;
    private Boolean showZeroTestCounts = true;
    private Boolean sortDescending = false;
    private Boolean showTimeStamp = true;
    private Boolean enableAutomaticSort = true;
    private Boolean manualSort = false;
    private Boolean showClaimInfo = true;
    private Boolean showWarningIcon = false;
    private Boolean replaceResponsibles = true;
    private Boolean autoResizeEntryHeight = true;
    private Boolean hideSuccessfulBuilds = false;
    private Boolean replaceNumberOfTestCases = true;
    private Boolean showClaimInfoInUnstable = true;
    private transient List<XFPanelEntry> entries;

    protected XFPanelColors colors;

    protected transient Map<hudson.model.Queue.Item, Integer> placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
    protected Map<String, Integer> priorityPerJob = new HashMap<String, Integer>();

    protected enum Blame { NOTATALL, ONLYLASTFAILEDBUILD, ONLYFIRSTFAILEDBUILD, EVERYINVOLVED }
    protected Blame BlameState = Blame.EVERYINVOLVED;

    private Integer maxAmmountOfResponsibles = 1;
    private String responsiblesTopic = "Responsible(s): ";
    private String lastBuildTimePreFix = "last successful: ";

    private String successfulBuildColor = "#7E7EFF";
    private String unstableBuildColor = "#FFC130";
    private String brokenBuildColor = "#FF0000";
    private String otherBuildColor = "#CCCCCC";
    private String buildFontColor = "#FFFFFF";
    
    private String jobNameReplaceRegExp;
    private String jobNameReplacement;
    private Pattern jobNameReplaceRegExpPattern;

    /**
     * C'tor<meta  />
     * @param name the name of the view
     * @param numColumns the number of columns to use on the layout (work in progress)
     */
    @DataBoundConstructor
    public XFPanelView(String name, Integer numColumns) {
        super(name);
        this.numColumns = numColumns != null ? numColumns : 2;
    }

    /**
     * @return the colors to use
     */
    public XFPanelColors getColors() {
        XFPanelColors obj = new XFPanelColors(successfulBuildColor, buildFontColor, unstableBuildColor, buildFontColor, brokenBuildColor, buildFontColor, otherBuildColor, buildFontColor);
        if (this.colors == null || this.colors.equals(obj) == false) {
            this.colors = obj;
        }
        return this.colors;
    }

    public Integer getGuiHeight() {
        if ( autoResizeEntryHeight    ){
            Integer entryHeight = guiJobFont + guiInfoFont;
            if ( showClaimInfo ){
                if ( BlameState != Blame.NOTATALL ){
                    if ( replaceResponsibles ){
                        entryHeight += guiClaimFont;
                    }
                    else{
                        entryHeight += guiClaimFont + guiInfoFont + guiInfoFont/2;
                    }
                }
                else {
                    entryHeight += guiClaimFont;
                }
                
                if ( showClaimInfoInUnstable ){
                    entryHeight += guiClaimFont;
                }
            }
            else if ( BlameState != Blame.NOTATALL ){
                entryHeight += guiInfoFont;
            }
            if ( showTimeStamp ){
                entryHeight += guiInfoFont + guiInfoFont/2;
            }

            entryHeight = Math.max( entryHeight, guiFailFont );
            entryHeight = Math.max( entryHeight, guiJobFont );

            if ( showZeroTestCounts && showTimeStamp ){
                entryHeight = Math.max( entryHeight, guiJobFont + guiInfoFont*3 );
            }
            Integer padding = 15; 
            return entryHeight + padding;
        }

        return guiHeight; 
    }

    public Integer getGuiJobFont() { return guiJobFont; }
    public Integer getGuiFailFont() { return guiFailFont; }
    public Integer getGuiInfoFont() { return guiInfoFont; }
    public Integer getGuiBuildFont() { return guiBuildFont; }
    public Integer getGuiClaimFont() { return guiClaimFont; }
    public Integer getMaxAmmountOfResponsibles(){ return maxAmmountOfResponsibles; }
    public Boolean getFullHD() { return this.fullHD; }

    public Boolean getShowDescription() {
        if (this.showDescription == null) {
            this.showDescription = false;
        }
        return this.showDescription;
    }
    
    public Boolean getShowBrokenBuildCount() {
        if (this.showBrokenBuildCount == null) {
            this.showBrokenBuildCount = Boolean.FALSE;;
        }
        return this.showBrokenBuildCount;
    }

    public Boolean getSortDescending() {
        if (this.sortDescending == null) {
            this.sortDescending = Boolean.FALSE;
        }
        return this.sortDescending;
    }

    public Boolean getShowZeroTestCounts() {
        if (this.showZeroTestCounts == null) {
            this.showZeroTestCounts = Boolean.TRUE;
        }
        return this.showZeroTestCounts;
    }

    public Boolean getShowTimeStamp() {
        return this.showTimeStamp;
    }

    public Boolean getShowClaimInfo() {
        return this.showClaimInfo;
    }
    public Boolean getShowClaimInfoInUnstable(){
        return this.showClaimInfoInUnstable;
    }
    public Boolean getShowWarningIcon(){
        return this.showWarningIcon;
    }
    public Boolean getReplaceResponsibles(){
        return this.replaceResponsibles;
    }
    public Integer getBlameState(){
        return BlameState.ordinal();
    }
    public Boolean getAutomaticSortState(){
        return enableAutomaticSort; // is automatic sort enabled or not
    }
    public Boolean getManualSortState(){
        return manualSort; // is manual sort enabled or not
    }
    public String getResponsiblesTopic(){
        if (this.responsiblesTopic == null){
            return "";
        }
        return this.responsiblesTopic;
    }
    public Boolean getHideSuccessfulBuilds(){
        return this.hideSuccessfulBuilds;
    }
    private String validateColor( String current, String defaultColor){
        if ( current != null && current.length() == 7 && current.startsWith("#")){
            return current.substring(1);
        }
        return defaultColor;
    }
    public String getSuccessfulBuildColor(){
        return validateColor( successfulBuildColor, "7E7EFF");
    }
    public String getUnstableBuildColor(){
        return validateColor( unstableBuildColor, "FFC130");
    }
    public String getBrokenBuildColor(){
        return validateColor( brokenBuildColor, "FF0000");
    }
    public String getOtherBuildColor(){
        return validateColor( otherBuildColor, "CCCCCC");
    }
    public String getBuildFontColor(){
        return validateColor( buildFontColor, "FFFFFF");
    }
    public Boolean getPriorityPerJob(){
        return priorityPerJob != null;
    }
    /**
     * Return true, if claim-plugin is installed
     */
    public Boolean getIsClaimPluginInstalled(){
        return (Hudson.getInstance().getPlugin("claim") != null);  
    }
    
    public Boolean getReplaceNumberOfTestCases(){
        if ( getIsClaimPluginInstalled() ){
            return this.replaceNumberOfTestCases;
        }
        return false;
    }
    public Boolean getAutoResizeEntryHeight(){
        return this.autoResizeEntryHeight;
    }

    static class selectComparator implements Comparator< XFPanelEntry > 
    {
        private int getPriority(AbstractBuild<?, ?> build) {
            // never built build
            if (build == null) {
                return 1;
            }

            if (build.isBuilding()) {
                build = build.getPreviousBuild();
                return getPriority(build);
            }

            Result result = build.getResult();
            if (result != null) {
                // priority order: the least important -> the most important 
                Result allResults[] = { Result.SUCCESS, Result.ABORTED, Result.NOT_BUILT, Result.UNSTABLE, Result.FAILURE };
                int resultValues[]  = {       0,              1,              1,                2,               3        };
                for (int i=0; i < allResults.length; i++) {
                    if (result == allResults[i] ){
                        return resultValues[i];
                    }
                }
            }
            return 1;
        }

        public int compare(XFPanelEntry a, XFPanelEntry b) {
            AbstractBuild<?, ?> buildA = a.getLastBuild();
            AbstractBuild<?, ?> buildB = b.getLastBuild();
            int result = getPriority(buildB) - getPriority(buildA);

            // if build results are same and builds exists-> sort by build timestamp    
            if (result == 0 ){

                // if build is null, show it on bottom of its class
                if ( buildA == null || buildB == null ){
                    return  ( buildA == null ) ? 1 : 0;
                }

                // if building atm -> show build on top of its class
                if ( buildA.isBuilding() || buildB.isBuilding() ){
                    return ( buildA.isBuilding() ) ? 0 : 1;
                }
                return b.getCompletionTimestamp().compareTo(a.getCompletionTimestamp());
            }
            return result;
        }
    }

    /**
     * @param jobs the selected jobs
     * @return the jobs list wrapped into {@link XFPanelEntry} instances
     */
    public Collection<XFPanelEntry> sort(Collection<Job<?, ?>> jobs) {
        placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();
        int j = 1;
        for(hudson.model.Queue.Item i : Hudson.getInstance().getQueue().getItems()) {
            placeInQueue.put(i, j++);
        }
        if (jobs != null) {
            List<XFPanelEntry> ents = new ArrayList<XFPanelEntry>();
            Collection<Job<?,?>> sortedJobs = getPrioritySortedJobs(jobs, false);
            for (Job<?, ?> job : sortedJobs) {
            	  XFPanelEntry xfPanelEntry = new XFPanelEntry(this, job);
            	  xfPanelEntry.init();
            	  ents.add(xfPanelEntry);
            }
            if ( enableAutomaticSort == true ){
                Collections.sort(ents, new selectComparator() );
            }

            if (this.getSortDescending()) {
                Collections.reverse(ents);
            }

            this.entries = ents;
            return this.entries;
        }
        return Collections.emptyList();
    }

    public Collection<Job<?, ?>> getPrioritySortedJobs() {
			List<Job<?, ?>> allItems = null;
			allItems = Jenkins.getInstance().getAllItems((Class<Job<?,?>>) (Class) Job.class);
    	return getPrioritySortedJobs(allItems);
    }
    
    public Collection<Job<?, ?>> getPrioritySortedJobs(Collection<Job<?, ?>> jobs) {
    	return getPrioritySortedJobs(jobs, true);
    }
    
    public Collection<Job<?, ?>> getPrioritySortedJobs(Collection<Job<?, ?>> jobs, final boolean isConfiguration) {
      if (jobs != null) {
          List<Job<?, ?>> sortedJobs = new ArrayList<Job<?, ?>>(jobs);
          final List<TopLevelItem> allItems = getItems(); //this is expensive function, and used in 'contains', so collect it once to speed up
          final Integer lastPriority = Integer.MAX_VALUE;
          if (manualSort || !enableAutomaticSort) {
	          Collections.sort(sortedJobs, new Comparator<Job<?, ?>>() {
	
							//@Override
							public int compare(Job<?, ?> o1, Job<?, ?> o2) {
								String n1 = o1.getName();
								String n2 = o2.getName();
								if (manualSort && priorityPerJob != null) {
									Integer p1 = (isConfiguration && !containsJob(o1)) ? lastPriority : priorityPerJob.get(n1);
									Integer p2 = (isConfiguration && !containsJob(o2)) ? lastPriority : priorityPerJob.get(n2);
									if (p1 == null) {
										p1 = lastPriority;
									}
									if (p2 == null) {
										p2 = lastPriority;
									}
									int c = p1.compareTo(p2);
									if (c == 0) {
										return compareNames(o1, o2);
									} else {
										return c;
									}
								} else {
									//alphabetical - selected on top
									return compareNames(o1, o2);
								}
							}

							private int compareNames(Job<?, ?> o1, Job<?, ?> o2) {
								if (isConfiguration) {
									Boolean cont1 = containsJob(o1);
									Boolean cont2 = containsJob(o2);
									int c = cont2.compareTo(cont1);
									if (c != 0) {
										return c;
									}
								}
								return o1.getName().compareToIgnoreCase(o2.getName());
							}

							private boolean containsJob(Job<?, ?> j) {
								if (j instanceof TopLevelItem) {
									return allItems.contains((TopLevelItem) j);
								}
								return false;
							}
						});
          }
          return sortedJobs;
      }
      return Collections.emptyList();
    }
    
    /**
     * @return the refresh time in seconds
     */
    public Integer getRefresh() {
        return this.refresh;
    }

    /**
     * @return the numColumns
     */
    public Integer getNumColumns() {
        return this.numColumns;
    }

    public String getLastBuildTimePreFix(){
        return (this.lastBuildTimePreFix != null) ? this.lastBuildTimePreFix : "";
    }

    /**
     * Gets from the request the configuration parameters
     * 
     * @param req {@link StaplerRequest}
     * 
     * @throws ServletException if any
     * @throws FormException if any
     */
    @Override
    protected void submit(StaplerRequest req) throws ServletException, FormException, IOException {
        super.submit(req);

        this.numColumns = asInteger(req, "numColumns");
        this.refresh = asInteger(req, "refresh");

        this.fullHD = Boolean.parseBoolean(req.getParameter("fullHD"));
        this.guiHeight = asInteger(req, "guiHeight");
        this.guiJobFont = asInteger(req, "guiJobFont");
        this.guiFailFont = asInteger(req, "guiFailFont");
        this.guiInfoFont = asInteger(req, "guiInfoFont");
        this.guiBuildFont = asInteger(req, "guiBuildFont");
        this.showDescription = Boolean.parseBoolean(req.getParameter("showDescription"));
        this.sortDescending = Boolean.parseBoolean(req.getParameter("sortDescending"));
        this.showTimeStamp = Boolean.parseBoolean(req.getParameter("showTimeStamp"));
        this.showZeroTestCounts = Boolean.parseBoolean(req.getParameter("showZeroTestCounts"));
        this.showWarningIcon = Boolean.parseBoolean(req.getParameter("showWarningIcon"));
        this.maxAmmountOfResponsibles = asInteger(req,"maxAmmountOfResponsibles");
        this.autoResizeEntryHeight = Boolean.parseBoolean(req.getParameter("autoResizeEntryHeight"));
        this.hideSuccessfulBuilds = Boolean.parseBoolean(req.getParameter("hideSuccessfulBuilds"));
        this.showBrokenBuildCount = Boolean.parseBoolean(req.getParameter("showBrokenBuildCount"));

        if ( getIsClaimPluginInstalled() ){
            this.guiClaimFont = asInteger(req, "guiClaimFont");
            this.showClaimInfo = Boolean.parseBoolean(req.getParameter("showClaimInfo"));
            this.showClaimInfoInUnstable = Boolean.parseBoolean(req.getParameter("showClaimInfoInUnstable"));
            this.replaceResponsibles = Boolean.parseBoolean(req.getParameter("replaceResponsibles"));
            this.replaceNumberOfTestCases = Boolean.parseBoolean(req.getParameter("replaceNumberOfTestCases"));
        }

        this.successfulBuildColor = "#" + req.getParameter("successfulBuildColor");
        this.unstableBuildColor = "#" + req.getParameter("unstableBuildColor");
        this.brokenBuildColor = "#" + req.getParameter("brokenBuildColor");
        this.otherBuildColor = "#" + req.getParameter("otherBuildColor");
        this.buildFontColor = "#" + req.getParameter("buildFontColor");

        if (this.priorityPerJob == null) {
            this.priorityPerJob = new HashMap<String, Integer>();
        }
        // Addition: Get priority of every job
        int priority = 0;
        this.priorityPerJob.clear();
        for (hudson.model.Item i : Hudson.getInstance().getItems()) {
            String itemName = i.getName();
            if (itemName != null) {
                String paramName = itemName + "_priority";
                try {
                    String priorityStr = req.getParameter(paramName);
                    if (priorityStr != null) {
                        priority = Integer.parseInt(priorityStr);
                    } else{
                        priority = 0;
                    }
                } catch (NumberFormatException e) {
                    priority++;
                } catch (Exception e) {
                    priority++;
                }
                this.priorityPerJob.put(itemName, priority);
            } else {
                throw new FormException("Couldn't read jobs from the config file. Generate a new config-file: jenkins_job_url/configure", "");
            }
        }

        String SortType = req.getParameter("sort");
        if (SortType != null) {
            this.enableAutomaticSort = false;
            this.manualSort = false;
            if (SortType.equals("sort.automatic")) {
                this.enableAutomaticSort = true;
            } else if (SortType.equals("sort.manual")) {
                this.manualSort = true;
            }
        }
        this.lastBuildTimePreFix = req.getParameter("lastBuildTimePreFix");
        this.responsiblesTopic = req.getParameter("responsiblesTopic");

        String blameType = req.getParameter("responsibles");
        if (blameType == null) {
            System.out.println("WARNING: Show responsibles == null --> Show responsibles disabled");
            this.BlameState = Blame.NOTATALL;
        }
        else if (blameType.equals("blame.notAtAll")) {
            this.BlameState = Blame.NOTATALL;
        }
        else if (blameType.equals("blame.onlyFirstFailedBuild")) {
            this.BlameState = Blame.ONLYFIRSTFAILEDBUILD;
        }
        else if (blameType.equals("blame.onlyLastFailedBuild")) {
            this.BlameState = Blame.ONLYLASTFAILEDBUILD;
        }
        else if (blameType.equals("blame.everyInvolved")) {
            this.BlameState = Blame.EVERYINVOLVED;
        }
        
        this.jobNameReplaceRegExp = req.getParameter("jobNameReplaceRegExp");
        this.jobNameReplacement = req.getParameter("jobNameReplacement");
        this.jobNameReplaceRegExpPattern = null;
    }

    private Integer asInteger(StaplerRequest request, String parameterName) throws FormException {
        try {
            return Integer.parseInt(request.getParameter(parameterName));
        } catch (NumberFormatException e) {
            throw new FormException(parameterName + " must be a positive integer", parameterName);
        }
    }
    
    public String getJobNameReplaceRegExp() {
	    return jobNameReplaceRegExp;
	}

	public String getJobNameReplacement() {
	    return jobNameReplacement;
	}


    public Pattern getJobNameReplaceRegExpPattern() {
        if (jobNameReplaceRegExpPattern == null && getJobNameReplaceRegExp() != null && getJobNameReplaceRegExp().length() > 0) {
            jobNameReplaceRegExpPattern = Pattern.compile(getJobNameReplaceRegExp());
        }
        return jobNameReplaceRegExpPattern;
	}

		/**
     * Notify Hudson we're implementing a new View
     * @author jrenaut
     */
    @Extension
    public static final class XFPanelViewDescriptor extends ViewDescriptor {
        public static final String REFRESH_MSG = "Refresh time must be a positive integer.";
        public static final String MSG = "Number of columns currently supported is 1 or 2.";

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return "eXtreme Feedback Panel";
        }

        /**
         * Performs validation on request parameters
         * @param value
         * @return a form validation
         */
        public FormValidation doCheckNumColumns(@QueryParameter String value) {
            try {
                int i = Integer.parseInt(value);
                if (i < 1 || i > 6) {
                    return FormValidation.error(MSG);
                }
            } catch (NumberFormatException e) {
                return FormValidation.error(MSG);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRefresh(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        public FormValidation doCheckGuiHeight(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        public FormValidation doCheckGuiJobFont(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        public FormValidation doCheckGuiFailFont(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        public FormValidation doCheckGuiInfoFont(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        public FormValidation doCheckGuiBuildFont(@QueryParameter String value) {
            return isPositiveInteger(value);
        }

        private FormValidation isPositiveInteger(String value) {
            return FormValidation.validatePositiveInteger(value);
        }
    }
    

/**
 * Represents colors to be used on the view  
 * @author jrenaut
 */
public static final class XFPanelColors {

    private String okBG;
    private String okFG;
    private String failedBG;
    private String failedFG;
    private String brokenBG;
    private String brokenFG;
    private String otherBG;
    private String otherFG;

    /**
     * C'tor
     * @param okBG ok builds background color
     * @param okFG ok builds foreground color
     * @param failedBG failed build background color 
     * @param failedFG failed build foreground color
     * @param brokenBG broken build background color
     * @param brokenFG broken build foreground color 
     * @param otherBG other build background color
     * @param otherFG other build foreground color
     */
    public XFPanelColors(String okBG, String okFG, String failedBG,
            String failedFG, String brokenBG, String brokenFG,
            String otherBG, String otherFG) {
        this.okBG = okBG;
        this.okFG = okFG;
        this.failedBG = failedBG;
        this.failedFG = failedFG;
        this.brokenBG = brokenBG;
        this.brokenFG = brokenFG;
        this.otherBG = otherBG;
        this.otherFG = otherFG;
    }
    /**
     * @return the okBG
     */
    public String getOkBG() {
        return okBG;
    }
    /**
     * @return the okFG
     */
    public String getOkFG() {
        return okFG;
    }
    /**
     * @return the failedBG
     */
    public String getFailedBG() {
        return failedBG;
    }
    /**
     * @return the failedFG
     */
    public String getFailedFG() {
        return failedFG;
    }
    /**
     * @return the brokenBG
     */
    public String getBrokenBG() {
        return brokenBG;
    }
    /**
     * @return the brokenFG
     */
    public String getBrokenFG() {
        return brokenFG;
    }
    /**
     * @return the otherBG
     */
    public String getOtherBG() {
        return otherBG;
    }
    /**
     * @return the otherFG
     */
    public String getOtherFG() {
        return otherFG;
    }

    public static final XFPanelColors DEFAULT = 
        new XFPanelColors("#7E7EFF", "#FFFFFF", "#FFC130", "#FFFFFF", "#FF0000", "#FFFFFF", "#CCCCCC", "#FFFFFF");
     /* okBG , okFG , failedBG , failedFG , brokenBG , brokenFG , otherBG ,
     * otherFG FFFFFF = white FF0000 = red 7E7EFF = blue FFC130 = yellow
     * 215E21 = huntergreen #267526 = another green
     */
}
}

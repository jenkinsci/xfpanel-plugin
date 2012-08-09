package maps.hudson.plugin.xfpanel;

import hudson.Extension;
import hudson.Functions;
import hudson.model.Result;
import hudson.model.ViewDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Run;
import hudson.model.User;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.FormValidation;
import hudson.scm.ChangeLogSet.Entry;
import hudson.plugins.claim.ClaimBuildAction;
import hudson.plugins.claim.ClaimTestAction;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestResultAction;
import hudson.model.Action;


import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Calendar;
import java.util.Comparator;
import java.lang.Math;

import javax.servlet.ServletException;

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

	private XFColors colors;
	
	private Integer numColumns = 2;
	private Integer refresh = 3;
	
	private Boolean fullHD = false;
	
	private Integer guiHeight = 205;
	private Integer guiImgHeight = 180;
	private Integer guiJobFont = 80;
	private Integer guiFailFont = 150;
	private Integer guiInfoFont = 30;
	private Integer guiBuildFont = 30;
	private Integer guiClaimFont = 30;
	
    private Boolean showDescription = false;

    private Boolean showZeroTestCounts = true;

    private Boolean sortDescending = false;
    
    private Boolean showTimeStamp = true;
    
    private Boolean enableAutomaticSort = true;
    
    private Boolean showClaimInfo = true;
    
    private Boolean showWarningIcon = false;
    
    private Boolean replaceResponsibles = true;
    
    private Boolean autoResizeEntryHeight = true;
    
    private Boolean hideSuccessfulBuilds = false;
    
	private transient List<XFPanelEntry> entries;

	private transient Map<hudson.model.Queue.Item, Integer> placeInQueue = new HashMap<hudson.model.Queue.Item, Integer>();

	protected enum Blame { NOTATALL, ONLYLASTFAILEDBUILD, ONLYFIRSTFAILEDBUILD, EVERYINVOLVED }
	protected Blame BlameState = Blame.EVERYINVOLVED;

	private Integer maxAmmountOfResponsibles = 1;
	
	private String responsiblesTopic = "Responsible(s): ";
	
	
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
	public XFColors getColors() {
		if (this.colors == null) {
			this.colors = XFColors.DEFAULT;
		}
		return this.colors;
	}
	
	public Integer getGuiHeight() { 
		if ( autoResizeEntryHeight	){
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
			}
			else if ( BlameState != Blame.NOTATALL ){
				entryHeight += guiInfoFont;
			}
			
			
			if ( showTimeStamp ){ 
				entryHeight += guiInfoFont + guiInfoFont/2;
			}
			
			entryHeight = Math.max( entryHeight, guiFailFont );
			entryHeight = Math.max( entryHeight, guiJobFont );
			
			if ( showWarningIcon || showClaimInfo ){
				entryHeight = Math.max( entryHeight, guiImgHeight );
			}
			
			if ( showZeroTestCounts && showTimeStamp ){
				entryHeight = Math.max( entryHeight, guiJobFont + guiInfoFont*3 );
			}
			
			Integer padding = 15; 
			return entryHeight + padding;
		}

		return guiHeight; 
	}

	public Integer getGuiImgHeight() { return guiImgHeight; }

	public Integer getGuiJobFont() { return guiJobFont; }

	public Integer getGuiFailFont() { return guiFailFont; }

	public Integer getGuiInfoFont() { return guiInfoFont; }

	public Integer getGuiBuildFont() { return guiBuildFont; }
	
	public Integer getGuiClaimFont() { return guiClaimFont; }
	
	public Integer getMaxAmmountOfResponsibles(){ return maxAmmountOfResponsibles; }
	public Boolean getFullHD() {
		return this.fullHD;
	}

    public Boolean getShowDescription() {
        if (this.showDescription == null) {
            this.showDescription = false;
        }
        return this.showDescription;
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
    public Boolean getShowWarningIcon(){
    	return this.showWarningIcon;
    }
    public Boolean getReplaceResponsibles(){
    	return this.replaceResponsibles;
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
    /**
     * Return true, if claim-plugin is installed
     */
    public Boolean getIsClaimPluginInstalled(){
    	return (Hudson.getInstance().getPlugin("claim") != null);  
    }
	
    static class selectComparator implements Comparator< XFPanelEntry >
    {
    	private int getPriority( AbstractBuild build )
    	{
    		// never built build
    		if (build == null ) {
    			return 1;
    		}
    		
    		if ( build.isBuilding() ){
				build = (AbstractBuild) build.getPreviousBuild();
				return getPriority( build );
    		}
    		
    		Result result = build.getResult();
    		if ( result != null ){
    			// priority order: the least important -> the most important 
	    		Result allResults[] = { Result.SUCCESS, Result.ABORTED, Result.NOT_BUILT, Result.UNSTABLE, Result.FAILURE };
	    		int resultValues[]  = {       0,              1,              1,                2,               3        };
				for (int i=0; i < allResults.length; i++ ){
					if (result == allResults[i] ){
						return resultValues[i];
					}
				}
    		}
			return 1;
    	}
    	
		public int compare( XFPanelEntry a, XFPanelEntry b) {
			AbstractBuild buildA = (AbstractBuild) a.job.getLastBuild();
			AbstractBuild buildB = (AbstractBuild) b.job.getLastBuild();
			int result = getPriority( buildB ) - getPriority( buildA );
				
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
				
				return b.completionTimestamp.compareTo( a.completionTimestamp );
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
			for (Job<?, ?> job : jobs) {
				ents.add(new XFPanelEntry(job));
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
		this.guiImgHeight = asInteger(req, "guiImgHeight");
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
        
        if ( getIsClaimPluginInstalled() ){
        	this.guiClaimFont = asInteger(req, "guiClaimFont");
        	this.showClaimInfo = Boolean.parseBoolean(req.getParameter("showClaimInfo"));
        	this.replaceResponsibles = Boolean.parseBoolean(req.getParameter("replaceResponsibles"));
        }
        
        String SortType = req.getParameter("sort");
        if ( SortType != null && SortType.equals("sort.automatic") ){
        	this.enableAutomaticSort = true;}
        else{
        	this.enableAutomaticSort = false;
        }
        
        this.responsiblesTopic = req.getParameter("responsiblesTopic");
        	
        String blameType = req.getParameter("responsibles");
        
        if ( blameType == null ){
        	System.out.println("WARNING: Show responsibles == null --> Show responsibles disabled" );
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

	}
	
	private Integer asInteger(StaplerRequest request, String parameterName) throws FormException {
		try {
			return Integer.parseInt(request.getParameter(parameterName));
		} catch (NumberFormatException e) {
			throw new FormException(parameterName + " must be a positive integer", parameterName);
		}		
	}
	
    /**
     * Represents a job to be shown on the panel
     * 
     * Intermediates access to data available for the given Job
     * 
     * @author jrenaut
     */
    public final class XFPanelEntry {

		private Job<?, ?> job;
    	
    	private String backgroundColor;
    	
    	private String color;
    	
    	private Boolean broken;
    	
    	private Boolean building = false;

        private Boolean queued = false;

        private Integer queueNumber;        
        
        private String completionTimestampString = "";
        
        private Calendar completionTimestamp;
        
    	/**
    	 * C'tor
    	 * @param job the job to be represented
    	 */
		public XFPanelEntry(Job<?, ?> job) {
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
			String label = job.getName().toUpperCase();
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
        
        private void setTimes() {
            AbstractBuild lastBuild = (AbstractBuild) this.job.getLastCompletedBuild();
            if (lastBuild != null) {
                this.completionTimestamp = lastBuild.getTimestamp();
                this.completionTimestampString = lastBuild.getTimestampString();
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
			for (Entry e : build.getChangeSet())
                r.add(e.getAuthor());
			return r;
		}
		
		public String convertCulpritsToString( HashSet<User> input ){
			String output = "";
			Iterator<User> it = input.iterator();
			
			int i=0;
	        for (; it.hasNext(); i++ ){
				if ( i < getMaxAmmountOfResponsibles()  ) 
					output += it.next().getFullName() + ((it.hasNext())?", ":"");
				else
					it.next();
	        }
			if ( i > getMaxAmmountOfResponsibles() ){
				output += "... <"+ (i-getMaxAmmountOfResponsibles()) +" more>";
			}
			if ( !output.isEmpty() )
				return output;
			return " - ";
		}
		
		public String getCulprits() {
			if ( BlameState == Blame.ONLYFIRSTFAILEDBUILD){
				Run<?, ?> run = this.job.getLastStableBuild(); //getLastSuccessfulBuild();
				if ( run == null ){ // if there aren't any successful builds
					run = this.job.getFirstBuild(); 
				}
				else {
					run = run.getNextBuild();
				}
				
				if (run instanceof AbstractBuild<?, ?>) {
					AbstractBuild<?, ?> firstFailedBuild = (AbstractBuild<?, ?>) run;
					return convertCulpritsToString( getCulpritFromBuild( firstFailedBuild ) );
				}
			}
			else if ( BlameState == Blame.ONLYLASTFAILEDBUILD){
				Run<?, ?> run = this.job.getLastFailedBuild(); //getLastBuild();
				
				if (run instanceof AbstractBuild<?, ?>) {
					AbstractBuild<?, ?> lastFailedBuild = (AbstractBuild<?, ?>) run;
					return convertCulpritsToString( getCulpritFromBuild( lastFailedBuild ) );
				}
			}
			else if ( BlameState == Blame.EVERYINVOLVED ){
				Run<?, ?> run = this.job.getLastBuild();
				
				
				if (run instanceof AbstractBuild<?, ?>) {
					AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
					HashSet<User> BlameList = new HashSet<User>( build.getCulprits() );
					return convertCulpritsToString( BlameList );
				}
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
	            Run lastBuild = job.getLastBuild();
	            if (lastBuild != null && lastBuild.isBuilding()) {
	                // claims can only be made against builds once they've finished,
	                // so check the previous build if currently building.
	                lastBuild = lastBuild.getPreviousBuild();
	            }
	            
	            if (lastBuild != null) {
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
	        ClaimBuildAction cba = getClaimAction();
	        if (cba != null) {
	            return cba.isClaimed();
	        }

	        return false;
	    }
	    
	    /**
	     * If the claims plugin is installed, this will get details of the claimed
	     * build failures.
	     *
	     * @return details of any claims for the broken build, or null if nobody has
	     *         claimed this build.
	     */
	    public String getClaimInfo() {
	        ClaimBuildAction claimAction = getClaimAction();
	        
	        if ( claimAction != null ){
	        	if (claimAction.isClaimed()) {
	        		String name = claimAction.getClaimedByName();    
	        		// String reason = claimAction.getReason();
	                return name;
	            }
	        }
	        
	        return "";
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
			return "#FFFFFF";
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
			
		/**
		 * Determines some information of the current job like which colors use, wether it's building or not or broken.
		 */
		private void findStatus() {
			switch (this.job.getIconColor()) {
			case BLUE_ANIME:
				this.building = true;
			case BLUE:
				this.backgroundColor = getColors().getOkBG(); 
				this.color = colors.getOkFG();
				this.broken = false;
				break;
			case YELLOW_ANIME:
				this.building = true;
			case YELLOW:
				this.backgroundColor = getColors().getFailedBG(); 
				this.color = colors.getFailedFG();
				this.broken = false;
				break;
			case RED_ANIME:
				this.building = true;
			case RED:
				this.backgroundColor = getColors().getBrokenBG(); 
				this.color = colors.getBrokenFG();
				this.broken = true;
				break;
			case GREY_ANIME:
			case DISABLED_ANIME:
				this.building = true;
			default:
				this.backgroundColor = getColors().getOtherBG(); 
				this.color = colors.getOtherFG();
				this.broken = true;
			}
		}
    	
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
		 * @param req request
		 * @param resp response
		 * @return a form validation
		 */
		public FormValidation doCheckNumColumns(@QueryParameter String value) {
      try {
        int i = Integer.parseInt(value);
        if (i < 1 || i > 2) {
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
		
		public FormValidation doCheckGuiImgHeight(@QueryParameter String value) {
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
    public static final class XFColors {
    	
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
		public XFColors(String okBG, String okFG, String failedBG,
				String failedFG, String brokenBG, String brokenFG,
				String otherBG, String otherFG) {
			super();
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
		
		public static final XFColors DEFAULT = 
			new XFColors("#7E7EFF", "#FFFFFF", "#FFC130", "#FFFFFF", "#FF0000", "#FFFFFF", "#CCCCCC", "#FFFFFF");
    }

}
 

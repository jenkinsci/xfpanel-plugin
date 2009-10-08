package maps.hudson.plugin.xfpanel;

import hudson.Extension;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.model.ViewDescriptor;
import hudson.tasks.test.AbstractTestResultAction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an eXtreme Feedback Panel View.
 * 
 * Thanks to Mark Howard and his work on the Radiator View Plugin from which this was based.
 *  
 * @author jrenaut
 */
public class XFPanelView extends ListView {

	private XFColors colors;
	
	private Integer numColumns;
	
	/**
	 * C'tor
	 * @param name the name of the view
	 * @param numColumns the number of columns to use on the layout (work in progress)
	 */
	@DataBoundConstructor
	public XFPanelView(String name, Integer numColumns) {
		super(name);
		this.numColumns = numColumns != null ? numColumns : 1;
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
	
	/**
	 * @param jobs the selected jobs
	 * @return the jobs list wrapped into {@link XFPanelEntry} instances
	 */
    public Collection<XFPanelEntry> sort(Collection<Job<?, ?>> jobs) {
    	if (jobs != null) {
    		List<XFPanelEntry> entries = new ArrayList<XFPanelEntry>();
    		for (Job<?, ?> job : jobs) {
				entries.add(new XFPanelEntry(job));
			}
    		return entries;
    	} 
        return Collections.emptyList();
    }
    
    /**
	 * @return the numColumns
	 */
	public Integer getNumColumns() {
		return numColumns;
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

    	/**
    	 * C'tor
    	 * @param job the job to be represented
    	 */
		public XFPanelEntry(Job<?, ?> job) {
			this.job = job;
			this.findStatus();
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
			return job.getName().toUpperCase();
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
		 * Elects a culprit/responsible for a broken build by choosing the last commiter of a given build 
		 * 
		 * @return the culprit/responsible
		 */
		public String getCulprit() {
			Run<?, ?> run = this.job.getLastBuild();
			String culprit = " - ";
			if (run instanceof AbstractBuild<?, ?>) {
				AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
				Iterator<User> it = build.getCulprits().iterator();
				while (it.hasNext()) {
					culprit = it.next().getFullName().toUpperCase();
				}
			}
			return culprit;
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

    	/**
    	 * {@inheritDoc}
    	 */
		@Override
		public String getDisplayName() {
			return "eXtreme Feedback Panel";
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
 
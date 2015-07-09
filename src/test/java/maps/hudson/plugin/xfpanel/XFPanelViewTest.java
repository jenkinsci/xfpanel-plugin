package maps.hudson.plugin.xfpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import hudson.DescriptorExtensionList;
import hudson.model.TopLevelItem;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Node;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.views.ListViewColumn;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;
import maps.hudson.plugin.xfpanel.XFPanelView.XFPanelColors;

import org.easymock.ConstructorArgs;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {TestResult.class, XFPanelEntry.class, XFPanelColors.class })
public class XFPanelViewTest {
	private static final int NR_OF_JOBS = 5;
	private static final int[] PRIORITIES = new int[] {1, 3, 2, 0, 4};

	@Test
	public void testGetPrioritySortedJobsAlphabetical() throws Exception {
  	PowerMock.resetAll();
		List<FreeStyleProject> fsps = prepareJobs();
		List<TopLevelItem> tli = Collections.<TopLevelItem>unmodifiableList(fsps);
		Collection<Job<?, ?>> jobs = Collections.<Job<?, ?>>unmodifiableList(fsps);;
		
		
		XFPanelView xfPanelView = prepareData(tli, false);
		Collection<Job<?, ?>> sortedJobs = xfPanelView.getPrioritySortedJobs(jobs, true);
		Job[] sortedJobsArray = sortedJobs.toArray(new Job[0]);
		assertNotNull(sortedJobs);
		assertEquals(5, sortedJobs.size());
		for (int i = 0; i < NR_OF_JOBS; i++) {
			assertEquals(((i%2 == 0)? "n": "N") + "ame"+ i, sortedJobsArray[i].getName());
		}
	}
	
	@Test
	public void testGetPrioritySortedJobsPriority() throws Exception {
  	PowerMock.resetAll();
		List<FreeStyleProject> fsps = prepareJobs();
		List<TopLevelItem> tli = Collections.<TopLevelItem>unmodifiableList(fsps);
		Collection<Job<?, ?>> jobs = Collections.<Job<?, ?>>unmodifiableList(fsps);;
		
		
		XFPanelView xfPanelView = prepareData(tli, true);
		Collection<Job<?, ?>> sortedJobs = xfPanelView.getPrioritySortedJobs(jobs, true);
		Job[] sortedJobsArray = sortedJobs.toArray(new Job[0]);
		assertNotNull(sortedJobs);
		assertEquals(5, sortedJobs.size());
		for (int i = 0; i < NR_OF_JOBS; i++) {
			int pr = PRIORITIES[i];
			int reversei = NR_OF_JOBS - i - 1; //because initial array was sorted descendant
			assertEquals("loop " + i, ((reversei%2 == 0)? "n": "N") + "ame"+ reversei, sortedJobsArray[pr].getName());
		}
	}

	private List<FreeStyleProject> prepareJobs() throws Exception {
/*	this works for new jenkins 1.598
 		Field field = Jenkins.class.getDeclaredField("theInstance");
    field.setAccessible(true);
    field.set(null, null);
*/
		Field field = Jenkins.class.getDeclaredField("theInstance");
    field.setAccessible(true);
    Jenkins jenkins = PowerMock.createNiceMock(Hudson.class);
    field.set(null, jenkins);
    EasyMock.expect(jenkins.getNodes()).andReturn(new ArrayList<Node>()).anyTimes(); 
		PowerMock.replayAll();
		
    List<FreeStyleProject> list = new ArrayList<FreeStyleProject>();
		//Jenkins jenkins = PowerMock.createNiceMock(Jenkins.class);
		Constructor<?> constructor = FreeStyleProject.class.getConstructor(Jenkins.class, String.class);
		for (int i = NR_OF_JOBS - 1; i >= 0; i--) { //make order different than alphabetical
			ConstructorArgs cargs = new ConstructorArgs(constructor, null, ((i%2 == 0)? "n": "N") + "ame"+ i);
			FreeStyleProject project = PowerMock.createNiceMock(FreeStyleProject.class, cargs);
			list.add(project);
		}
		return list;
	}

	private XFPanelView prepareData(List<TopLevelItem> jobs, Boolean manualSort) throws Exception {
		Field field = Jenkins.class.getDeclaredField("theInstance");
    field.setAccessible(true);
    Jenkins jenkins = PowerMock.createNiceMock(Hudson.class);
    field.set(null, jenkins);
    EasyMock.expect(jenkins.getNodes()).andReturn(new ArrayList<Node>()).anyTimes(); 

    DescriptorExtensionList del = PowerMock.createNiceMock(DescriptorExtensionList.class);
//    DescriptorExtensionList del = DescriptorExtensionList.createDescriptorList(jenkins, Publisher.class);
    List<String> al = new ArrayList<String>();
    EasyMock.expect(del.iterator()).andReturn(al.iterator()).anyTimes();
    EasyMock.expect(del.toArray()).andReturn(al.toArray()).anyTimes();
    EasyMock.expect(jenkins.getDescriptorList(ListViewColumn.class)).andReturn(del);
		PowerMock.replayAll();
		

    XFPanelView view = PowerMock.createPartialMock(XFPanelView.class, new String[]{"getItems"}, "testview", 2);
  	PowerMock.resetAll();
//    XFPanelView view = PowerMock.createPartialMockAndInvokeDefaultConstructor(XFPanelView.class, new String[]{"getItems"});
    //createDefaultInitialColumnList
    EasyMock.expect(view.getItems()).andReturn(jobs).anyTimes();
    
		Field fieldEnableAutomaticSort = XFPanelView.class.getDeclaredField("enableAutomaticSort");
		fieldEnableAutomaticSort.setAccessible(true);
		fieldEnableAutomaticSort.set(view, false);
		Field fieldManualSort = XFPanelView.class.getDeclaredField("manualSort");
		fieldManualSort.setAccessible(true);
		fieldManualSort.set(view, manualSort);
		
		if (manualSort) {
			Map<String, Integer> priorityPerJob = new HashMap<String, Integer>();
			int p = 0;
			for (TopLevelItem tli: jobs) {
				priorityPerJob.put(tli.getName(), PRIORITIES[p++]);
			}
			Field fieldPriorityPerJob = XFPanelView.class.getDeclaredField("priorityPerJob");
			fieldPriorityPerJob.setAccessible(true);
			fieldPriorityPerJob.set(view, priorityPerJob);
		}
		PowerMock.replayAll();
    return view;
	}
}

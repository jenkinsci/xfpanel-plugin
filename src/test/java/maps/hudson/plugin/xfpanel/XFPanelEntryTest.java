package maps.hudson.plugin.xfpanel;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import hudson.DescriptorExtensionList;
import hudson.Plugin;
import hudson.model.BallColor;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.claim.ClaimTestAction;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.views.ListViewColumn;

import maps.hudson.plugin.xfpanel.XFPanelView.XFPanelColors;

import org.easymock.ConstructorArgs;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMockBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {TestResult.class, XFPanelEntry.class, XFPanelColors.class, CaseResult.class })
public class XFPanelEntryTest {

	@Test
	public void testOneClaimed() throws Exception {
		XFPanelEntry xfPanelEntry = prepareData(3, true, true);
		assertEquals("2", xfPanelEntry.getNumberOfTests());
	}
	@Test
	public void testNoneClaimed() throws Exception {
		XFPanelEntry xfPanelEntry = prepareData(4, false, true);
		assertEquals("4", xfPanelEntry.getNumberOfTests());
		//verifyAll();
	}
	@Test
	public void testAllFailed() throws Exception {
		XFPanelEntry xfPanelEntry = prepareData(4, true, false);
		assertEquals("4", xfPanelEntry.getNumberOfTests());
		//verifyAll();
	}

	private XFPanelEntry prepareData(int allFailed, boolean oneClaimed, boolean showClaimed) throws NoSuchFieldException,
			IllegalAccessException {
		Field field = Jenkins.class.getDeclaredField("theInstance");
    field.setAccessible(true);
    Jenkins jenkins = PowerMock.createNiceMock(Jenkins.class);
    field.set(null, jenkins);
    //EasyMock.expect(jenkins.getInstance()).andReturn(jenkins);
    //<T extends Describable<T>, D extends Descriptor<T>> 
    DescriptorExtensionList del = PowerMock.createNiceMock(DescriptorExtensionList.class);
    List<String> al = new ArrayList<String>();
    EasyMock.expect(del.iterator()).andReturn(al.iterator());
    EasyMock.expect(jenkins.getDescriptorList(ListViewColumn.class)).andReturn(del);
		FreeStyleProject project = PowerMock.createNiceMock(FreeStyleProject.class);
		Job job = project;
		XFPanelView view = PowerMock.createNiceMock(XFPanelView.class);
		EasyMock.expect(view.getDisplayName()).andReturn("testview");
		EasyMock.expect(job.getIconColor()).andReturn(BallColor.BLUE);
		XFPanelColors xfPanelColors = PowerMock.createNiceMock(XFPanelColors.class);
		EasyMock.expect(view.getColors()).andReturn(xfPanelColors).anyTimes();
		PowerMock.replayAll();
		
//		XFPanelEntry xfPanelEntry = PowerMock.createMock(XFPanelEntry.class,
//				view, job);
		XFPanelEntry xfPanelEntry = new XFPanelEntry(view, job);
		PowerMock.resetAll();
		
		EasyMock.expect(view.getIsClaimPluginInstalled()).andReturn(true).anyTimes();
		EasyMock.expect(view.getReplaceNumberOfTestCases()).andReturn(showClaimed).anyTimes();

		AbstractBuild lastBuild = PowerMock.createNiceMock(AbstractBuild.class);
		AbstractTestResultAction atra = PowerMock.createNiceMock(AbstractTestResultAction.class);
		EasyMock.expect(atra.getFailCount()).andReturn(allFailed);
		EasyMock.expect(lastBuild.getAction(AbstractTestResultAction.class)).andReturn(atra);
		
		TestResult testResult = PowerMock.createNiceMock(TestResult.class);

    List<CaseResult> crl = new ArrayList<CaseResult>();
		CaseResult cr = PowerMock.createNiceMock(CaseResult.class);
		ClaimTestAction cta = PowerMock.createNiceMock(ClaimTestAction.class);
		EasyMock.expect(cta.isClaimed()).andReturn(oneClaimed);
		EasyMock.expect(cr.getTestAction(ClaimTestAction.class)).andReturn(cta);
		crl.add(cr);

		EasyMock.expect(testResult.getTotalCount()).andReturn(-1); //whatever
		EasyMock.expect(testResult.getFailedTests()).andReturn(crl);
		
		TestResultAction tra = PowerMock.createNiceMock(TestResultAction.class);
		EasyMock.expect(tra.getResult()).andReturn(testResult);
		List<TestResultAction> tral = new ArrayList<TestResultAction>();
		tral.add(tra);

		EasyMock.expect(lastBuild.getActions(TestResultAction.class)).andReturn(tral);
		EasyMock.expect(job.getLastSuccessfulBuild()).andReturn(lastBuild).anyTimes();
		EasyMock.expect(job.getLastBuild()).andReturn(lastBuild).anyTimes();

    EasyMock.expect(jenkins.getPlugin("claim")).andReturn(new Plugin.DummyImpl()).anyTimes();
		
		PowerMock.replayAll();
		assertNotNull(Jenkins.getInstance().getPlugin("claim"));
		return xfPanelEntry;
	}
}

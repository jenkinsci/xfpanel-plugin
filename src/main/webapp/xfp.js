/**
 * Applys best fit (fill box keeping aspect ratio) images
 *
 * Checks all image tags of imgBestFit class for height/width
 * and sets CSS for the smaller to 100% and clears the other
 * keeping aspect-ratio of image
 *
 * @return None
 */
function checkImgBestFit () {

	var imgList = $$('.imgBestFit');
	for (var x=0; x<imgList.length; x++)
	{
		if (imgList[x].parentElement.offsetWidth < imgList[x].parentElement.offsetHeight)
		{
			imgList[x].style.width  = "100%";
			imgList[x].style.height = "";
		} else {
			imgList[x].style.width  = "";
			imgList[x].style.height = "100%";	
		}
	}
}

/**
 * Applys fade to all fadeHiddenText classed tags
 *
 * checkTextFadeouts iterates through all tags of fadeHiddenText class.
 * Each is checked if scroll width > visible (overflowed) and if so any
 * fadeHiddenTextImg class <img> tags under it are made visible.  
 * Otherwise these img's are hidden (display:none).
 *
 * @return None
 */
function checkTextFadeouts () {

	var fadersList = $$('.fadeHiddenText');
	for (var x=0; x<fadersList.length; x++)
	{
		var applyFade = (fadersList[x].scrollWidth > fadersList[x].offsetWidth);
		
		var imgList = fadersList[x].select('img.fadeHiddenTextImg');
		for (var y=0; y<imgList.length; y++)
		{
			if (applyFade)
			{
				imgList[y].style.display = "";
			} else {
				imgList[y].style.display = "none";
			}
		}
	}
}

/**
 * Applys fade to all fadeHiddenText classed tags and sets default CSS
 *
 * Default CSS properties are applied to fadeHiddenText and fadeHiddenTextImg
 * class elements since the plugin doesn't have its own .css style to apply.
 *
 * @return None
 */
function checkTextFadeoutsInit () {

	var fadersList = $$('.fadeHiddenText');
	for (var x=0; x<fadersList.length; x++)
	{	
		fadersList[x].style.position = "relative";

		var imgList = fadersList[x].select('img.fadeHiddenTextImg');
		for (var y=0; y<imgList.length; y++)
		{
			imgList[y].style.position = "absolute"; 
			imgList[y].style.right    = "0px"; 
			imgList[y].style.top      = "0px"; 
			imgList[y].style.height   = "100%";
		}
	}
	
	checkTextFadeouts();
}


Behaviour.addLoadEvent(function(){
	//Remove side panel from display (since XFP displays as a "full" screen)
	document.getElementById("side-panel").style.display="none";
	
	//Initialize text faders
	checkTextFadeoutsInit();
	Event.observe(window, "resize", checkTextFadeouts);
		
	//Resize images to fit given blocks
	checkImgBestFit();
	Event.observe(window, "resize", checkImgBestFit);

    // automatically update page every 10 seconds via AJAX
    var refreshTime = $$(".conf-refresh-time")[0].innerHTML;
    var url = window.location.href;
    var refreshUrl = url + (url.charAt(url.length - 1) == "/" ? "" : "/") + "headlessdisplay";
    if (url.lastIndexOf("?") >= 0) {
        refreshUrl = url.substring(0, url.lastIndexOf("?"));
        refreshUrl = refreshUrl + (refreshUrl.charAt(refreshUrl.length - 1) == "/" ? "" : "/") + "headlessdisplay";
    }
    new Ajax.PeriodicalUpdater("xfdisplay-dashboard", refreshUrl, {
        method: 'get', frequency: refreshTime, decay: 2
    });
});

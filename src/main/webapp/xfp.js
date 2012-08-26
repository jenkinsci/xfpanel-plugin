

function checkTextFadeouts () {

	var fadersList = $$('.fadeHiddenText');
	for (var x=0; x<fadersList.length; x++)
	{
		var applyFade = (fadersList[x].scrollWidth > fadersList[x].offsetWidth);
		
		var imgList = fadersList[x].select('img.fadeHiddenTextImg');
		for (var y=0; y<imgList.length; y++)
		{
			//alert(imgList[y].src);
			if (applyFade)
			{
				imgList[y].style.display = "";
			} else {
				imgList[y].style.display = "none";
			}
		}
	}
}


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
	document.getElementById("side-panel").style.display="none";
	
	checkTextFadeoutsInit();
	Event.observe(window, "resize", checkTextFadeouts);
});



function swapNodes(item1,item2)
{
    var itemtmp = item1.cloneNode(1);
    var parent = item1.parentNode;
    item2 = parent.replaceChild(itemtmp,item2);
    parent.replaceChild(item2,item1);
    parent.replaceChild(item1,itemtmp);
    itemtmp = null;
}

function ChangePriority(direction) {
    if (selectedElement == null) {
        return;
    }
    
    var elem = document.getElementById(selectedElement);
	if (!elem) return;

	var next = (direction == 'up') ? elem.previousSibling : elem.nextSibling;
    if (next != null) {
        var elemPriority = document.getElementById(selectedElement + "_priority");
        var sibPriority = document.getElementById(next.id + "_priority");
        
        var tmp = elemPriority.value;
        elemPriority.value = sibPriority.value;
        sibPriority.value = tmp;
    
        swapNodes(elem, next);
    }

}

function UpdateJobOptions(currentElement) {
    
    selectedElement = currentElement;
    var elem = document.getElementById('allJobs');
    var x = elem.childNodes;

    var i=0;
    while(true) {
        if (x.length > i) {
            var optName = x[i].id + "_options";
            var optElem = document.getElementById(optName);
            optElem.style.visibility = 'hidden';
            
            i++;
        }
        else {
            break;
        }
    }          
    var optName = currentElement + "_options";
    
    var optElem = document.getElementById(optName);
    optElem.style.visibility = 'visible';
    
}




# RSA [Release Status Analyser]
==============

### Jenkins xfpanel plugin Reloaded
-----------


A one click solution for ALL [ QA engineer, DevOps, CTO ] based on [jenkins/xfpanel](https://github.com/jenkinsci/xfpanel-plugin)
A comprehensive dashboard for feedback of all tests triggered for a build with lot more benefits, 
originally built for serving test feedback needs @ [goibibo.com](http://www.goibibo.com/)


![Alt text](/docs/QuickView-dashBoard.png "QuickView")

It Includes :
* Nice layout of test jobs feedback [ Any one can understand and infer the release status in a glance ]
* Release info which broke the test [ DevOps Engineer love it]
* quick links to visit detail report of each build  [ Full fledged customised report which shows details about all tests, their data & reason for failure]
* quick link to visit screenshots of the failed tests  [ Dev Engineer love it ]
* quick link to visit console for debugging the cause  [ Test Engineer need it]

### Requirements
To add this plugin to your internal jenkins, you will have to add 2 keys in build parameters of your test jobs
- env
- tagtotest
![Alt text](/docs/jobdef.png “JobDefi”)

### Credits
- Thanks to [Vikalp](https://github.com/vikalp) for the idea
- Thanks to [Raj](github.com/rajdgreat007/) for all ui/js help & patience.
- Thanks to [Tilman and all contributors for starting it up :-)](https://github.com/jenkinsci/xfpanel-plugin)






Sakai Plus
==========

My project “Sakai Plus” is adding an LTI Advantage “tool provider” to Sakai 23.  Sakai
already has an LTI 1.1 provider (i.e. you can launch a Sakai tool from another LMS like Canvas, Moodle, Blackboard, etc.).

The Sakai Plus effort will build equivalent and expanded support for integrating
Sakai into other LMS systems using LTI 1.3 / LTI Advantage.

Unlike most of Sakai (and Tsugi) – the Sakai Plus LTI 1.3 provider will be done completely
separately from the LTI 1.1 support.  In lots of other places in Sakai/Tsugo, a lot of 
effort has been made to make code simultansously support LTI 1.1 and LTI 1.3 in the same
code base with careful use of if-then-else.  But because the scope of LTI Advantage tool
provider is quite a bit broader that the LTI 1.1 provider, it will be kept separate so
it can grow without maintaining legacy capabilities.


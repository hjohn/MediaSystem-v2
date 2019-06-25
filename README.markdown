MediaSystem
===========

Front-end application for video libraries.  See screenshots!

Features
--------
* Plays any video with VLC or MPV back-end
* Scans your collection and displays covers, plots, ratings and actors using data from TMDB (www.themoviedb.org)
* Can show trailers, recommendations, cast & crew, participations and movie collections
* Supports Series and Movies, as well as local video collections
* Extracts meta information from your collection (exact duration, tracks, subtitles, snapshots)
* Keeps track of watched videos
* Resumes playback from last position
* Supports plugins for different styled views, video playback and video collections
* Fully keyboard controlled (or remote controlled using EventGhost mappings), intended for use on a Projector

Technical Features
------------------
* Supports PostgreSQL as a database back-end, falling back to Apache Derby when left unconfigured

Requirements
------------
* Java Runtime Environment 11+ installed (64-bit)

Optional
--------
### VLC

* Install VLCPlayer (64-bit)

### MPV

MPV is included, however it uses "youtube-dl" when playing YouTube videos (trailers).

* Install "youtube-dl.exe" in your search path

Getting started
---------------
Coming soon...


Third Party Dependencies
========================

VLC for Java (vlcj)
------------------
by Mark Lee of Caprica Software  
License: GNU GPL, version 3 or later  
https://github.com/caprica/vlcj

Apache Derby
------------
License: Apache License, version 2.0  
http://db.apache.org/derby/

OpenCV
------
License: Apache License, version 2.0  
https://github.com/bytedeco/javacv

MPV
---
License: GNU GPL, version 2  
http://mpv.io

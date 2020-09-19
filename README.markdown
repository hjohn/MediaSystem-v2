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

MediaSystem should run on most operating systems.  It comes with its own embedded database 
(Apache Derby) but can also utilize a Postgres database.  This is configured in the 
`mediasystem.yaml` file.  If no database is configured, a database will be created automatically 
in the current directory in a folder named `db`.  Depending on how many media you have, 
the database can become a couple of gigabytes in size.  This size mainly consists of 
extracted snapshots and cached photos and media to improve performance.

Initial scanning of your media may take a while.  MediaSystem has several background 
processes that will:

* Scan imported folders for changes and create a fingerprint for each file found (to detect renames or moves)
* Identify files and download meta-data from TMDB
* Analyze each file to extract its audio, video and subtitle tracks and a few snapshots

These processes all take a while, and will automatically continue where they left off when 
interrupted.  For about a 1000 video files, the scanning and fingerprinting takes upto 
30 minutes, identification can take an hour (limited the allowed requests per minute 
on TMDB) and the analysis can take a few hours as each media is decoded partially to 
extract snapshots.  Only the scanning phase needs to be completed to already start using
the system, but it becomes much more useful once identification has also completed.

### Linux

* Install Java 11+, for example `apt install default-jre`
* Install the Noto fonts (https://https://www.google.com/get/noto)
* Install libmpv (`apt install libmpv1`) and libavfilter7
* (Optional) Install `youtube-dl` for playing trailers from YouTube with MPV
* LC_NUMERIC="C" ????
* Unzip the archive where you would like the software to run
* Change directory to the `MediaSystem` directory
* Use the provided `run.sh` script or `java -jar mediasystem.jar` to run the program

Note: the program expects its configuration files to be in the current directory.  This 
is also where it will create the database folder `db`.  Future runs of the program should
be from the same folder so cached data can be utilized from the database.

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

#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Badit (BuildAndDebugIT) is a program to to simplify the process of deployment
and updating an android project
 copyright:  2012, (c) tuenti.com
 author:   Nick Flink <nickflink@github.com>
"""
import sys
import getopt
import os
import subprocess
import getpass
import logging

#non standard includes

##
##  The following options are provided.
##  --help [-h]. What you are reading now
##  --log-level [-l]. setting the log level dynamically
##  --profile [-p]. Setting build profile

logger = logging.getLogger(__name__)
def usage():
	fh = open(__file__,"r")
	me = fh.readlines()
	sys.stderr.write("usage:\n")
	for line in me:
		if line.find("##") == 0:
			sys.stderr.write(line)

def mavenBuild(profile):
	subprocess.call(["mvn", "install", "--activate-profiles", profile])

def installApk():
	homePath = os.environ['HOME']
	snapshotVersion = "1.0-SNAPSHOT"
	apkPath = os.path.join(homePath, ".m2", "repository", "com", "tuenti", "voice", "voice-example", snapshotVersion, "voice-example-"+snapshotVersion+".apk")
	installCmdList = ["adb", "install", "-r", apkPath]
	logger.info("installCmd = "+" ".join(installCmdList))
	subprocess.call(installCmdList)

def startApk():
	subprocess.call(["adb", "shell", "am", "start", "-a", "android.intent.action.VIEW", "-n", "com.tuenti.voice/.ui.LoginView"])

def main(argv = None):
	profiles = ["default_debug", "default_release", "default_final", "tuenti_debug", "tuenti_release", "tuenti_final"]
	logLevel = "INFO"
	profile="default_debug"
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hd:l:p:", ["help", "log-level=", "profile="])
	except getopt.GetoptError, err:
		# print help information and exit:
		logger.error(err) # will print something like "option -a not recognized"
		usage()
		return 1
	for o, a in opts:
		if o in ("-h", "--help"):
			usage()
			sys.exit(1)
		if o in ("-l", "--log-level"):
			logLevel = a
		if o in ("-p", "--profile"):
			profile = a
		else:
			usage()
	if logLevel == "DEBUG":
		logging.basicConfig(level=logging.DEBUG)
	else:
		logging.basicConfig(level=logging.INFO)
	if profile not in profiles:
		logger.error("bad profile "+profile+"\noptions are: "+", ".join(profiles)+"\nexiting.")
		sys.exit(1)
	logger.info("Running with:\n\tprofile: "+profile)
	savedPath = os.getcwd()
	os.chdir(os.path.join(os.path.dirname(os.path.dirname(__file__)), "android"))
	mavenBuild(profile)
	installApk()
	startApk()
	os.chdir(savedPath)
	logger.info("Done!")
	return 0

if __name__ == "__main__":
  sys.exit(main())

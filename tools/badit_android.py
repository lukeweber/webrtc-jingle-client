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

build_bit = 0x1
install_bit = 0x2
start_bit = 0x4
debug_bit = 0x8
uninstall_bit = 0x16
taskMask = build_bit | install_bit | start_bit | debug_bit

#non standard includes

##
##  The following options are provided.
##  --help [-h]. What you are reading now
##  --log-level [-l]. setting the log level dynamically
##  --profile [-p]. Setting build profile
##  --task-mask [-m]. skip over some tasks

logger = logging.getLogger(__name__)
def usage():
	fh = open(__file__,"r")
	me = fh.readlines()
	sys.stderr.write("usage:\n")
	for line in me:
		if line.find("##") == 0:
			sys.stderr.write(line)

def runCmd(name, cmdList):
	logger.info("=> "+name+" = "+" ".join(cmdList))
	if subprocess.call(cmdList) == 0:
		logger.info("[OK] "+name+" = "+" ".join(cmdList))
	else:
		logger.info("[KO] "+name+" = "+" ".join(cmdList))


def mavenBuild(profile):
	if taskMask & build_bit:
		profiles = ["default_debug", "default_release", "default_final", "tuenti_debug", "tuenti_release", "tuenti_final"]
		if profile not in profiles:
			logger.error("[KO] Build bad profile "+profile+"\noptions are: "+", ".join(profiles)+"\nexiting.")
			sys.exit(1)
		runCmd("Build", ["mvn", "install", "--activate-profiles", profile])
	else:
		logger.info("skipping build")

def installApk():
	if taskMask & install_bit:
		homePath = os.environ['HOME']
		snapshotVersion = "1.0-SNAPSHOT"
		#apkPath = os.path.join(homePath, ".m2", "repository", "com", "tuenti", "voice", "voice-example", snapshotVersion, "voice-example-"+snapshotVersion+".apk")
		# ('-r' means reinstall the app, keeping its data)
		#runCmd("Install", ["adb", "install", "-r", apkPath])
		runCmd("Install", ["mvn", "-pl", "voice-client-example", "android:redeploy"])
	else:
		logger.info("skipping install");
		

def uninstallApk():
	if taskMask & uninstall_bit:
		runCmd("Uninstall", ["adb", "uninstall", "com.tuenti.voice.example"])
	else:
		logger.info("skipping uninstall");

def startApk():
	if taskMask & start_bit:
		#runCmd("Start", ["adb", "shell", "am", "start", "-a", "android.intent.action.VIEW", "-n", "com.tuenti.voice/.ui.LoginView"])
		runCmd("Start", ["mvn", "-pl", "voice-client-example", "android:run"])
	else:
		logger.info("skipping start");

def debugApk():
  #gdbApk = os.path.join("..","build","android","gdb_apk")
#  DBG_CMD="$TRUNKDIR/build/android/gdb_apk -p com.tuenti.voice.example -s VoiceClientService -l android/voice-client-core/obj/$BUILD_PROFILE/local/$FORCE_CPU_ABI"
	runCmd("Debug", ["echo", "implement", "debugging"])

def checkSDK():
		try:
			sdkRoot = os.environ['ANDROID_SDK_ROOT']
			platformsDir = os.path.join(sdkRoot, "platforms", "android-14")
		except KeyError:
			logger.error("Please set ANDROID_SDK_ROOT")
			sys.exit(1)
		try:
			androidCmd = os.path.join(str(sdkRoot), "tools", "android")
			f = open(androidCmd, 'r')
			f.close()
		except IOError:
			logger.error("ANDROID_SDK_ROOT is corrupt cannot find:\n\t"+androidCmd)
			sys.exit(1)
		if not os.path.isdir(platformsDir):
			logger.error("You're missing android-14, which is required")
			logger.error("You can install it by copy pasting the below command")
			sys.stderr.write(androidCmd + " update sdk -u --filter android-14\n")
			sys.exit(1)

def checkNDK():
		try:
			ndkRoot = os.environ['ANDROID_NDK_ROOT']
			releaseTxt = os.path.join(ndkRoot, "RELEASE.TXT")
			releaseHandle = open(releaseTxt, 'r')
			releaseContents = releaseHandle.read().strip()
			releaseHandle.close()
		except (KeyError,IOError) as e:
			logger.error("Please set ANDROID_NDK_ROOT");
			sys.exit(1)
		if not (releaseContents == "r8" or releaseContents == "r8c"):
			logger.error("Please install NDK version r8 or r8c.")
			sys.exit(1)

def main(argv = None):
	global taskMask
	taskBits = ["build", "install", "start"]
	logLevel = "INFO"
	profile="default_debug"
	logging.basicConfig(level=logging.INFO)
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hd:l:p:m:", ["help", "log-level=", "profile=", "task-mask="])
	except getopt.GetoptError, err:
		# print help information and exit:
		logger.error(err) # will print something like "option -a not recognized"
		print "ERROR 2"
		usage()
		return 1
	for o, a in opts:
		if o in ("-h", "--help"):
			usage()
			sys.exit(1)
		elif o in ("-l", "--log-level"):
			logLevel = a
		elif o in ("-p", "--profile"):
			profile = a
		elif o in ("-m", "--task-mask"):
			taskMask = int(a)
		else:
			usage()
	if logLevel == "DEBUG":
		logging.basicConfig(level=logging.DEBUG)
	logger.info("Running with:\n\tprofile: "+profile+"\n\tlog-level: "+logLevel+"\n\ttask-mask: "+str(taskMask))
	checkSDK()
	checkNDK()
	savedPath = os.getcwd()
	os.chdir(os.path.join(os.path.dirname(os.path.dirname(__file__)), "android"))
	mavenBuild(profile)
	uninstallApk()
	installApk()
	startApk()
	debugApk()
	os.chdir(savedPath)
	logger.info("Done!")
	return 0

if __name__ == "__main__":
  sys.exit(main())

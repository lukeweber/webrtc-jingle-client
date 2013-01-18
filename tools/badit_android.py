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
import pprint

build_bit =     (1<<0)
install_bit =   (1<<1)
start_bit =     (1<<2)
debug_bit =     (1<<3)
uninstall_bit = (1<<4)

#global vars
taskMask = build_bit | install_bit | start_bit | debug_bit
profile="default_debug"
supportedProfiles = ["default_debug", "default_release", "default_final", "tuenti_debug", "tuenti_release", "tuenti_final"]
supportedNDKs = ["r8", "r8d"]

##
##  The following options are provided.
##  --help [-h]. What you are reading now
##  --log-level [-l]. setting the log level dynamically
##  --profile [-p]. Setting build profile
##  --task-mask [-m]. skip over some tasks
##  --build [-b] build. Build the apk
##  --uninstall [-u] uninstall. Uninstall the apk
##  --install [-i] install. Install the apk
##  --start [-s] start. Start the apk
##  --debug [-d] debug. Debug the apk

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
		logger.error("[KO] "+name+" = "+" ".join(cmdList))
		return 1
	return 0

def mavenBuild():
	if taskMask & build_bit:
		if profile not in supportedProfiles:
			logger.error("[KO] Build bad profile "+profile+"\noptions are: "+", ".join(supportedProfiles)+"\nexiting.")
			sys.exit(1)
		if runCmd("Build", ["mvn", "install", "--activate-profiles", profile]) != 0:
			sys.exit(1)
	else:
		logger.info("skipping build")

def uninstallApk():
	if taskMask & uninstall_bit:
		# we don't want to fail during the uninstall
		runCmd("Uninstall", ["adb", "uninstall", "com.tuenti.voice.example"])
	else:
		logger.info("skipping uninstall");

def installApk():
	if taskMask & install_bit:
		homePath = os.environ['HOME']
		snapshotVersion = "1.0-SNAPSHOT"
		if runCmd("Install", ["mvn", "-pl", "voice-client-example", "android:redeploy"]) != 0:
			newTaskMask = (taskMask - (taskMask & build_bit)) | uninstall_bit
			logger.error("New version is not compatible with the old version")
			logger.error("This can be fixed with the below command:")
			logger.error("\tNOTE: it will uninstall your previous installation")
			sys.stderr.write(__file__+" --task-mask="+str(newTaskMask))
			sys.exit(1)
	else:
		logger.info("skipping install");

def startApk():
	if taskMask & start_bit:
		if runCmd("Start", ["mvn", "-pl", "voice-client-example", "android:run"]) != 0:
			sys.exit(1)
	else:
		logger.info("skipping start");

def debugApk():
	cpuInfoCmd = ["adb", "shell", "cat", "/system/build.prop"]
	p = subprocess.Popen(cpuInfoCmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	out, err = p.communicate()
	if len(err) != 0:
		logger.error(" ".join(cpuInfoCmd)+" exited with errors:\n"+err)
		sys.exit(1)
	for line in out.splitlines():
		if line.startswith("ro.product.cpu.abi="):
			cpuAbi = line.split("=")[1]
	androidBuildDir = os.path.join("build","android")
	gdbApk = os.path.join(androidBuildDir, "gdb_apk")
	envSetup = os.path.join(androidBuildDir, "envsetup.sh")
	abiSymbolDir = os.path.join("android", "voice-client-core", "obj", profile, "local", cpuAbi)
	debugCmd = ["bash", "-c", "'source "+envSetup+" && "+gdbApk+" -p com.tuenti.voice.example -s VoiceClientService -l "+abiSymbolDir+"'"]
	logger.info("Debugging requires a shell.  Copy paste the below to begin debugging:\n"+" ".join(debugCmd))
	#runCmd("Debug", debugCmd)

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
		if releaseContents not in supportedNDKs:
			logger.error("Please install a supported NDK version:\n\t"+" ".join(supportedNDKs))
			sys.exit(1)

def main(argv = None):
	global taskMask
	global profile
	logLevel = "INFO"
	logging.basicConfig(level=logging.INFO)
	newTaskMask = 0
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hbuisdl:p:m:", ["help", "build", "uninstall", "install", "start", "debug", "log-level=", "profile=", "task-mask="])
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
		elif o in ("-b", "--build"):
			newTaskMask |= build_bit
		elif o in ("-u", "--uninstall"):
			newTaskMask |= uninstall_bit
		elif o in ("-i", "--install"):
			newTaskMask |= install_bit
		elif o in ("-s", "--start"):
			newTaskMask |= start_bit
		elif o in ("-d", "--debug"):
			newTaskMask |= debug_bit
		elif o in ("-l", "--log-level"):
			logLevel = a
		elif o in ("-p", "--profile"):
			profile = a
		elif o in ("-m", "--task-mask"):
			newTaskMask |= int(a)
		else:
			usage()
	if newTaskMask > 0:
		taskMask = newTaskMask
	if logLevel == "DEBUG":
		logging.basicConfig(level=logging.DEBUG)
	logger.info("Running with:\n\tprofile: "+profile+"\n\tlog-level: "+logLevel+"\n\ttask-mask: "+str(taskMask))
	checkSDK()
	checkNDK()
	savedPath = os.getcwd()
	os.chdir(os.path.join(os.path.dirname(os.path.dirname(__file__)), "android"))
	mavenBuild()
	uninstallApk()
	installApk()
	startApk()
	os.chdir(os.path.dirname(os.path.dirname(__file__)))
	debugApk()
	os.chdir(savedPath)
	logger.info("Done!")
	return 0

if __name__ == "__main__":
	newTaskMask = 1
	sys.exit(main())

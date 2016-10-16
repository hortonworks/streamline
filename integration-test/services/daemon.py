import sys, os, time, subprocess
from signal import SIGTERM
from utils import process_alive
import logging

try:
    from subprocess import DEVNULL  # Python
except ImportError:
    DEVNULL = open(os.devnull, 'wb')

class Daemon:
    """
    A generic daemon class.
    Usage: subclass the Daemon class and override the run() method
    """

    def __init__(self, cmd, pidfile, stdin='/dev/null', stdout='/dev/null', stderr='/dev/null'):
        self.cmd = cmd
	self.pidfile = pidfile
        self.stdin = stdin
        self.stdout = stdout
        self.stderr = stderr
        self.logger = logging.getLogger("streamline.Daemon")

    def daemonize(self):
        # and finally let's execute the executable for the daemon!
        try:
            process = subprocess.Popen(self.cmd, stdout=DEVNULL, stderr=DEVNULL, shell=True)
            # write pidfile
            self.logger.info("process started with pid " + str(process.pid))
            file(self.pidfile,'w+').write("%s\n" % process.pid)
        except Exception, e:
            print("failed to start process " + self.cmd + " due to %s", e)
            os._exit(255)

    def delpid(self):
	os.remove(self.pidfile)

    def start(self):
        """
	Start the daemon
	"""
        # Check for a pidfile to see if the daemon already runs
        try:
	    pf = file(self.pidfile,'r')
	    pid = int(pf.read().strip())
	    pf.close()
	except IOError:
	    pid = None
	    if pid:
		message = "pidfile %s already exist. Daemon already running?\n"
		sys.stderr.write(message % self.pidfile)
		sys.exit(1)
	# Start the daemon
	self.daemonize()

    def status(self):
        """
        returns true if the process is running with pid
        """
        pid = self.pid()
        return process_alive(pid)

    def pid(self):
        """
        returns PID from file
        """
        # Get the pid from the pidfile
	try:
	    pf = file(self.pidfile,'r')
	    pid = int(pf.read().strip())
	    pf.close()
	except IOError:
	    pid = None
        return pid

    def stop(self):
	"""
	Stop the daemon
	"""
        pid = self.pid()
	if not pid:
	    message = "pidfile %s does not exist. Daemon not running?\n"
	    self.logger.error(message % self.pidfile)
	    return # not an error in a restart
        self.logger.info("killing process %s", pid)
	# Try killing the daemon process
        try:
	    while self.status():
		pkill = os.kill(pid, SIGTERM)
		time.sleep(0.1)
	except OSError, err:
	    self.logger.info("failed to kill the process %s due to %s", pid, str(err))
        if os.path.exists(self.pidfile):
	    os.remove(self.pidfile)

    def restart(self):
	"""
	Restart the daemon
	"""
	self.stop()
	self.start()

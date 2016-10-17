import urllib, os, shutil, logging, tarfile, zipfile, subprocess, errno
from urlparse import urlparse
from os.path import basename
from shutil import copyfile

module_logger = logging.getLogger("streamline.service.utils")
logger = logging.getLogger("streamline.service.utils")

def download_and_unzip(url, dir):
    logger.info("downloading " + url + " to " + dir)
    disassembled = urlparse(url)
    filename, file_ext = splitext(basename(disassembled.path))
    logger.info("filename " + filename + " ext " + file_ext)
    full_filename = os.path.join(dir, filename+file_ext)
    mkdir(dir)
    downloadFile = urllib.urlretrieve(url, full_filename)
    extract(full_filename, dir)
    return os.path.join(dir, filename)

def download_and_build(url, dir, service_name, service_dist, service_version, service_dist_file):
    logger.info("downloading " + url + " to " + dir)
    # create the main directory for storing the git code
    mkdir(dir)
    os.chdir(dir)
    #clone git repo
    repo_dir = os.path.join(dir, service_name)
    logger.info("cloning the %s", service_name)
    subprocess.call("git clone "+ url, shell=True)
    os.chdir(repo_dir)

    #run the main project build
    logger.info("building the %s", service_name)
    subprocess.call("mvn -DskipTests clean install", shell=True)

    #build the binaries
    logger.info("building the dist %s", service_dist)
    os.chdir(os.path.join(repo_dir, service_dist))
    subprocess.call("mvn -DskipTests clean install", shell=True)

    #copy the binary file into main directory
    srcfile = os.path.join(repo_dir, service_dist, "target", service_dist_file)
    logger.info("built file %s", srcfile)
    extract(srcfile, dir)
    service_ext_dir, ext = splitext(service_dist_file)

    return os.path.join(dir, service_ext_dir)

def run_cmd(cmd):
    #build the binaries
    logger.info("Running the command %s", cmd)
    subprocess.call(cmd, shell=True)


def delete_dir(dir):
    logger.info("deleting dir " + dir)
    #shutil.rmtree(dir)

def mkdir(dir):
    if not os.path.exists(dir):
        os.makedirs(dir)

def process_alive(pid):
    if pid < 0:
        return False
    if pid == 0:
        raise ValueError('invalid PID 0')
    try:
        return os.waitpid(pid, os.WNOHANG) == (0, 0)
    except OSError as err:
        logger.error("process alive error %s", err)
        if err.errno != errno.ECHILD:
            raise
    return False


def splitext(path):
    for ext in ['.tar.gz', '.tar.bz2']:
        if path.endswith(ext):
            return path[:-len(ext)], path[-len(ext):]
    return os.path.splitext(path)

def extract(filename, dir):
    logger.info("extracting " + filename)
    os.chdir(dir)
    if filename.endswith("zip"):
        zip = zipfile.ZipFile(filename)
        zip.extractAll(dir)
    else:
        tar = tarfile.open(filename, "r:gz")
        tar.extractall()
        tar.close()

